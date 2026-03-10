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
        log.info("Starting async PDF generation for book: {}", bookId);
        
        try {
            Books book = booksRepository.findByIdWithImagesAndUser(bookId)
                    .orElseThrow(() -> new RuntimeException("Book not found: " + bookId));

            List<ImageBooks> images = imageBooksRepository.findByBookIdIdOrderByOrderIndexAsc(bookId);

            if (images.isEmpty()) {
                log.warn("Book {} has no images, skipping PDF generation", bookId);
                return;
            }

            String bookTitle = "Livro de Colorir - " + book.getUser().getEmail();
            byte[] pdfBytes = pdfGeneratorService.generatePdfFromImageUrls(images, bookTitle);

            String downloadUrl = cloudinaryService.uploadPdf(pdfBytes, "livro_" + bookId);
            log.debug("PDF uploaded to Cloudinary: {}", downloadUrl);

            book.setDownloadUrl(pdfBytes);
            book.setStatusPay(BookPaymentStatus.PAID);
            booksRepository.save(book);

            log.info("Async PDF generation completed for book: {}", bookId);
            
        } catch (Exception e) {
            log.error("Error during async PDF generation for book {}: {}", bookId, e.getMessage(), e);
            
            Books book = booksRepository.findById(bookId).orElse(null);
            if (book != null) {
                book.setStatusPay(BookPaymentStatus.FAILED);
                booksRepository.save(book);
            }
        }
    }
}
