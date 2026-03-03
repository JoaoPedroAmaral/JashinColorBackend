package com.javation.coloringbook.Controller;

import com.javation.coloringbook.DTO.PixPaymentResponse;
import com.javation.coloringbook.Entity.Books;
import com.javation.coloringbook.Exception.BookNotFoundException;
import com.javation.coloringbook.Repository.BooksRepository;
import com.javation.coloringbook.Service.PixPaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/pix")
public class PixController {

    private final PixPaymentService pixPaymentService;
    private final BooksRepository booksRepository;

    @PostMapping("/generate")
    public ResponseEntity<?> generatePixQrCode(@RequestBody Map<String, Object> request) {
        try {
            Long bookId      = Long.valueOf(request.get("bookId").toString());
            Double amount    = Double.valueOf(request.get("amount").toString());

            Books book = booksRepository.findById(bookId)
                    .orElseThrow(() -> new BookNotFoundException(bookId));

            PixPaymentResponse response = pixPaymentService.generatePixQrCode(amount, book.getTitle());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Erro ao gerar QR Code PIX: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}