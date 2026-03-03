package com.javation.coloringbook.Service;

import com.javation.coloringbook.Exception.BookNotFoundException;
import com.javation.coloringbook.Exception.UserNotFoundException;
import com.javation.coloringbook.Entity.BookPaymentStatus;
import com.javation.coloringbook.Entity.Books;
import com.javation.coloringbook.Entity.ImageBooks;
import com.javation.coloringbook.Entity.Users;
import com.javation.coloringbook.Repository.BooksRepository;
import com.javation.coloringbook.Repository.ImageBooksRepository;
import com.javation.coloringbook.Repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookService {

    private final BooksRepository booksRepository;
    private final UsersRepository usersRepository;
    private final ImageBooksService imageBooksService;
    private final PDFGeneratorService pdfGeneratorService;
    private final ImageBooksRepository imageBooksRepository;
    private final CloudinaryService cloudinaryService;
    private final FileValidator fileValidator;

    @Transactional(readOnly = true)
    public List<Books> findBooksByUserId(Long userId) {
        log.debug("Finding books for user ID: {}", userId);
        return booksRepository.findBookByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<Books> findBookByUserId(Long userId) {
        return findBooksByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Books findBookById(Long bookId) {
        log.debug("Finding book by ID: {}", bookId);
        return booksRepository.findByIdWithImagesAndUser(bookId)
                .orElseThrow(() -> new BookNotFoundException(bookId));
    }

    public Books findBookByIdForUser(Long bookId, Long userId) {
        log.debug("Finding book {} for user {}", bookId, userId);
        Books book = booksRepository.findByIdWithImagesAndUser(bookId)
                .orElseThrow(() -> new BookNotFoundException(bookId));

        if (!book.getUser().getId().equals(userId)) {
            log.warn("User {} attempted to access book {} belonging to another user", userId, bookId);
            throw new BookNotFoundException(bookId);
        }
        
        return book;
    }

    @Transactional
    public Books createBookWithImages(Long userId, String title, List<MultipartFile> files) throws IOException {
        log.info("Creating book for user ID: {} with {} files", userId, files.size());
        
        fileValidator.validateFiles(files);

        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        Books book = Books.builder()
                .user(user)
                .title(title != null && !title.isBlank() ? title : "Livro de Colorir")
                .totalPages(files.size())
                .createAt(LocalDateTime.now())
                .statusPay(BookPaymentStatus.PENDING)
                .build();

        book = booksRepository.save(book);
        log.debug("Book created with ID: {}", book.getId());

        List<ImageBooks> imageBooksList = imageBooksService.processAndSaveImages(book, files);
        book.setImages(imageBooksList);
        
        log.info("Book {} created successfully with {} images", book.getId(), imageBooksList.size());
        return book;
    }

    @Transactional
    public byte[] generateBookDownloadUrl(Long bookId, Long userId) throws Exception {
        log.info("Generating download URL for book ID: {} by user: {}", bookId, userId);
        
        Books book = findBookByIdForUser(bookId, userId);

        List<ImageBooks> images = imageBooksRepository.findByBookIdIdOrderByOrderIndexAsc(bookId);

        if (images.isEmpty()) {
            log.warn("Book {} has no images", bookId);
            throw new IllegalStateException("Este livro não possui imagens.");
        }

        String bookTitle = "Livro de Colorir - " + book.getUser().getEmail();
        log.debug("Generating PDF for book: {}", bookTitle);
        
        byte[] pdfBytes = pdfGeneratorService.generatePdfFromImageUrls(images, bookTitle);

        String downloadUrl = cloudinaryService.uploadPdf(pdfBytes, "livro_" + bookId);
        log.debug("PDF uploaded to Cloudinary: {}", downloadUrl);

        book.setDownloadUrl(pdfBytes);
        book.setStatusPay(BookPaymentStatus.PAID);
        booksRepository.save(book);

        log.info("Book {} marked as PAID and ready for download", bookId);
        return pdfBytes;
    }

    @Transactional
    public Books updateBookStatus(Long bookId, BookPaymentStatus status) {
        log.info("Updating book {} status to {}", bookId, status);
        Books book = booksRepository.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException(bookId));
        
        book.setStatusPay(status);
        return booksRepository.save(book);
    }
}
