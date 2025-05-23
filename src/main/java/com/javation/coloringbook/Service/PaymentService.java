package com.javation.coloringbook.Service;

import com.javation.coloringbook.Entity.Books;
import com.javation.coloringbook.Entity.Payment;
import com.javation.coloringbook.Entity.TransactionStatus;
import com.javation.coloringbook.Repository.BooksRepository;
import com.javation.coloringbook.Repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final BooksRepository booksRepository;
    private final BookService bookService;

    public Payment registerPayment(Long bookPayId, String transactionId) {
        Books book = booksRepository.findById(bookPayId)
                .orElseThrow(() -> new IllegalArgumentException("Livro não encontrado"));

        Payment payment = Payment.builder()
                .bookPay(book)
                .transactionId(transactionId)
                .statusPay(TransactionStatus.PENDING)
                .build();

        return paymentRepository.save(payment);
    }

    public void confirmPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Pagamento não encontrado"));
        try {

            payment.setStatusPay(TransactionStatus.SUCCESS);
            payment.setPaidAt(LocalDateTime.now());
            paymentRepository.save(payment);

            Books book = payment.getBookPay();
            book.setStatusPay(com.javation.coloringbook.Entity.BookPaymentStatus.PAID);


            bookService.generateBookDownloadUrl(book);
        } catch (Exception e) {
            payment.setStatusPay(TransactionStatus.FAILED);
            paymentRepository.save(payment);
            throw new RuntimeException("Erro ao pagar o livro", e);
        }

    }

    public Payment getPaymentsByBookId(Long bookPayId) {
        return paymentRepository.findByBookPayId(bookPayId);
    }
}
