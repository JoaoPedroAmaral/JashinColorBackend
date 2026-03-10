package com.javation.coloringbook.Service;

import com.javation.coloringbook.Entity.BookPaymentStatus;
import com.javation.coloringbook.Entity.Books;
import com.javation.coloringbook.Entity.Payment;
import com.javation.coloringbook.Entity.TransactionStatus;
import com.javation.coloringbook.Repository.BooksRepository;
import com.javation.coloringbook.Repository.PaymentRepository;
import com.mercadopago.MercadoPagoConfig;
import com.mercadopago.client.payment.PaymentClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentWebhookService {

    @Value("${mercadopago.access-token}")
    private String accessToken;

    private final PaymentRepository paymentRepository;
    private final BooksRepository booksRepository;
    private final BookService bookService;

    @Transactional
    public void processNotification(String id, String topic, Map<String, Object> payload) {
        try {
            MercadoPagoConfig.setAccessToken(accessToken);
            String resourceId = id;
            String resourceTopic = topic;

            if (payload != null) {
                if (payload.containsKey("type")) {
                    resourceTopic = payload.get("type").toString();
                }
                if (payload.containsKey("data") && payload.get("data") instanceof Map) {
                    Map<String, Object> data = (Map<String, Object>) payload.get("data");
                    if (data.containsKey("id")) {
                        resourceId = data.get("id").toString();
                    }
                }
            }

            if ("payment".equals(resourceTopic)) {
                handlePaymentUpdate(resourceId);
            }
        } catch (Exception e) {
            log.error("Webhook processing failed: {}", e.getMessage());
            // Se falhou por deadlock ou outra exceção de banco, o Spring vai dar rollback
            // mas aqui estamos capturando para logar.
            throw e; 
        }
    }

    @Transactional
    protected void handlePaymentUpdate(String paymentId) throws Exception {
        PaymentClient client = new PaymentClient();
        com.mercadopago.resources.payment.Payment mpPayment = client.get(Long.valueOf(paymentId));

        if ("approved".equals(mpPayment.getStatus())) {
            String externalRef = mpPayment.getExternalReference();

            if (externalRef != null && !externalRef.isBlank()) {
                Long bookId = Long.valueOf(externalRef);
                confirmBookPayment(bookId, paymentId);
            }
        }
    }

    @Transactional
    protected void confirmBookPayment(Long bookId, String transactionId) throws Exception {
        // Busca o livro com lock para evitar que dois processos tentem criar o pagamento ao mesmo tempo
        Books book = booksRepository.findById(bookId)
                .orElseThrow(() -> new RuntimeException("Book not found: " + bookId));

        if (book.getStatusPay() == BookPaymentStatus.PAID) {
            log.info("Book {} already marked as PAID. Skipping.", bookId);
            return;
        }

        Payment payment = paymentRepository.findByBookPayId(bookId);
        if (payment == null) {
            payment = Payment.builder()
                    .bookPay(book)
                    .transactionId(transactionId)
                    .statusPay(TransactionStatus.SUCCESS)
                    .paidAt(LocalDateTime.now())
                    .build();
            log.info("Creating new payment record for book {}", bookId);
        } else {
            payment.setTransactionId(transactionId);
            payment.setStatusPay(TransactionStatus.SUCCESS);
            payment.setPaidAt(LocalDateTime.now());
            log.info("Updating existing payment record for book {}", bookId);
        }
        
        paymentRepository.saveAndFlush(payment);

        book.setStatusPay(BookPaymentStatus.PAID);
        booksRepository.saveAndFlush(book);
        
        log.info("Book {} status updated to PAID", bookId);

        try {
            bookService.generateBookDownloadUrl(bookId, book.getUser().getId());
        } catch (Exception e) {
            log.error("Error generating download URL, but payment was successful: {}", e.getMessage());
        }
    }
}
