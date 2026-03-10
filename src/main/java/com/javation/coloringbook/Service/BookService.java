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
import org.springframework.scheduling.annotation.Async;
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
        return booksRepository.findBookByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Books findBookById(Long bookId) {
        return booksRepository.findByIdWithImagesAndUser(bookId)
                .orElseThrow(() -> new BookNotFoundException(bookId));
    }

    public Books findBookByIdForUser(Long bookId, Long userId) {
        Books book = booksRepository.findByIdWithImagesAndUser(bookId)
                .orElseThrow(() -> new BookNotFoundException(bookId));

        if (!book.getUser().getId().equals(userId)) {
            throw new BookNotFoundException(bookId);
        }
        
        return book;
    }

    @Transactional
    public Books createBookWithImages(Long userId, String title, List<MultipartFile> files, Double price) throws IOException {
        if (price == null || price < 0) {
            throw new IllegalArgumentException("Invalid price");
        }

        fileValidator.validateFiles(files);

        Users user = usersRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        Books book = Books.builder()
                .user(user)
                .title(title != null && !title.isBlank() ? title : "Livro de Colorir")
                .totalPages(files.size())
                .price(price)
                .createAt(LocalDateTime.now())
                .statusPay(BookPaymentStatus.PENDING)
                .build();

        book = booksRepository.save(book);

        List<ImageBooks> imageBooksList = imageBooksService.processAndSaveImages(book, files);
        book.setImages(imageBooksList);
        
        return book;
    }

    @Async
    @Transactional
    public void generateBookDownloadUrl(Long bookId, Long userId) throws Exception {
        Books book = findBookByIdForUser(bookId, userId);
        List<ImageBooks> images = imageBooksRepository.findByBookIdIdOrderByOrderIndexAsc(bookId);

        if (images.isEmpty()) {
            return;
        }

        String bookTitle = "Livro de Colorir - " + book.getUser().getEmail();
        byte[] pdfBytes = pdfGeneratorService.generatePdfFromImageUrls(images, bookTitle);

        cloudinaryService.uploadPdf(pdfBytes, "livro_" + bookId);

        book.setDownloadUrl(pdfBytes);
        book.setStatusPay(BookPaymentStatus.PAID);
        booksRepository.save(book);
    }

    @Transactional
    public Books updateBookStatus(Long bookId, BookPaymentStatus status) {
        Books book = booksRepository.findById(bookId)
                .orElseThrow(() -> new BookNotFoundException(bookId));
        
        book.setStatusPay(status);
        return booksRepository.save(book);
    }
}
