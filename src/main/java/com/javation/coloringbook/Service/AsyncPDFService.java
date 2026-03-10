package com.javation.coloringbook.Service;

import com.javation.coloringbook.Entity.BookPaymentStatus;
import com.javation.coloringbook.Entity.Books;
import com.javation.coloringbook.Entity.ImageBooks;
import com.javation.coloringbook.Repository.BooksRepository;
import com.javation.coloringbook.Repository.ImageBooksRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncPDFService {

    private final PDFGeneratorService pdfGeneratorService;
    private final CloudinaryService cloudinaryService;
    private final ImageBooksRepository imageBooksRepository;
    private final BooksRepository booksRepository;

    @Async
    public void generatePdfAsync(Long bookId) {
        log.info("Starting async PDF generation for book ID: {}", bookId);
        
        try {
            // Pequeno delay para garantir que a transação do banco que marcou como PAID terminou
            Thread.sleep(1000);

            Books book = booksRepository.findByIdWithImagesAndUser(bookId)
                    .orElseThrow(() -> new RuntimeException("Book not found: " + bookId));

            List<ImageBooks> images = imageBooksRepository.findByBookIdIdOrderByOrderIndexAsc(bookId);

            if (images.isEmpty()) {
                log.warn("Book {} has no images, skipping PDF generation", bookId);
                return;
            }

            log.info("Generating PDF with {} images for book {}", images.size(), bookId);
            String bookTitle = "Livro de Colorir - " + book.getUser().getEmail();
            byte[] pdfBytes = pdfGeneratorService.generatePdfFromImageUrls(images, bookTitle);

            if (pdfBytes == null || pdfBytes.length == 0) {
                throw new RuntimeException("Generated PDF is empty for book " + bookId);
            }

            log.info("PDF generated successfully ({} bytes). Uploading to Cloudinary...", pdfBytes.length);
            // Opcional: Upload para o Cloudinary se você quiser um link externo
            try {
                String cloudinaryUrl = cloudinaryService.uploadPdf(pdfBytes, "livro_" + bookId);
                log.info("PDF uploaded to Cloudinary: {}", cloudinaryUrl);
            } catch (Exception e) {
                log.warn("Cloudinary upload failed (non-critical): {}", e.getMessage());
            }

            // Salva o PDF no banco de dados
            book.setDownloadUrl(pdfBytes);
            book.setStatusPay(BookPaymentStatus.PAID);
            booksRepository.saveAndFlush(book);

            log.info("Async PDF generation and save completed for book: {}", bookId);
            
        } catch (Exception e) {
            log.error("CRITICAL error during async PDF generation for book {}: {}", bookId, e.getMessage(), e);
            
            try {
                Books book = booksRepository.findById(bookId).orElse(null);
                if (book != null) {
                    book.setStatusPay(BookPaymentStatus.FAILED);
                    booksRepository.saveAndFlush(book);
                }
            } catch (Exception ex) {
                log.error("Failed to mark book as FAILED: {}", ex.getMessage());
            }
        }
    }
}
