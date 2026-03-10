package com.javation.coloringbook.Service;

import com.javation.coloringbook.Exception.BookNotFoundException;
import com.javation.coloringbook.Exception.PaymentException;
import com.javation.coloringbook.Entity.BookPaymentStatus;
import com.javation.coloringbook.Entity.Books;
import com.javation.coloringbook.Entity.Payment;
import com.javation.coloringbook.Entity.TransactionStatus;
import com.javation.coloringbook.Repository.BooksRepository;
import com.javation.coloringbook.Repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final BooksRepository booksRepository;
    private final BookService bookService;

    @Transactional
    public Payment registerPayment(Long bookPayId, String transactionId) {
        log.info("Registering payment for book ID: {} with transaction: {}", bookPayId, transactionId);

        if (transactionId == null || transactionId.isBlank()) {
            throw new PaymentException("Transaction ID não pode ser vazio");
        }

        Books book = booksRepository.findById(bookPayId)
                .orElseThrow(() -> new BookNotFoundException(bookPayId));

        if (book.getStatusPay() == BookPaymentStatus.PAID) {
            log.warn("Attempt to pay for already paid book: {}", bookPayId);
            throw new PaymentException("Este livro já foi pago");
        }

        Payment payment = Payment.builder()
                .bookPay(book)
                .transactionId(transactionId)
                .statusPay(TransactionStatus.PENDING)
                .build();

        Payment savedPayment = paymentRepository.save(payment);
        log.info("Payment {} registered successfully for book {}", savedPayment.getId(), bookPayId);
        return savedPayment;
    }

    @Transactional
    public void confirmPayment(Long paymentId) {
        log.info("Confirming payment ID: {}", paymentId);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new PaymentException("Pagamento não encontrado com ID: " + paymentId));

        if (payment.getStatusPay() == TransactionStatus.SUCCESS) {
            log.warn("Payment {} already confirmed", paymentId);
            throw new PaymentException("Este pagamento já foi confirmado");
        }

        try {
            payment.setStatusPay(TransactionStatus.SUCCESS);
            payment.setPaidAt(LocalDateTime.now());
            paymentRepository.save(payment);
            log.debug("Payment {} status updated to SUCCESS", paymentId);

            Books book = payment.getBookPay();
            book.setStatusPay(BookPaymentStatus.PAID);
            booksRepository.save(book);
            log.debug("Book {} status updated to PAID", book.getId());

            bookService.updateBookStatus(book.getId(), BookPaymentStatus.PAID);
            
            bookService.generateBookDownloadUrl(book.getId(), book.getUser().getId());
            
            log.info("Payment {} confirmed successfully for book {}", paymentId, book.getId());
            
        } catch (Exception e) {
            log.error("Error during payment confirmation for payment {}: {}", paymentId, e.getMessage(), e);
            payment.setStatusPay(TransactionStatus.FAILED);
            paymentRepository.save(payment);
            throw new PaymentException("Erro ao processar o pagamento: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public Payment getPaymentsByBookId(Long bookPayId) {
        log.debug("Finding payment for book ID: {}", bookPayId);
        Payment payment = paymentRepository.findByBookPayId(bookPayId);
        
        if (payment == null) {
            log.warn("No payment found for book ID: {}", bookPayId);
            throw new PaymentException("Nenhum pagamento encontrado para este livro");
        }
        
        return payment;
    }
}
