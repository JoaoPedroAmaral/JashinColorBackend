package com.javation.coloringbook.Service;

import com.javation.coloringbook.Entity.BookPaymentStatus;
import com.javation.coloringbook.Entity.Books;
import com.javation.coloringbook.Entity.ImageBooks;
import com.javation.coloringbook.Entity.Users;
import com.javation.coloringbook.Repository.BooksRepository;
import com.javation.coloringbook.Repository.ImageBooksRepository;
import com.javation.coloringbook.Repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class BookService {
    private final BooksRepository booksRepository;
    private final UsersRepository usersRepository;
    private final ImageBooksService imageBooksService;
    private final PDFGeneratorService pdfGeneratorService;
    private final ImageBooksRepository imageBooksRepository;
    private final CloudinaryService cloudinaryService;

    public List<Books> findBookByUserId(Long userId){
        return booksRepository.findBookByUserId(userId);
    }

    public Books findBookById(Long bookId){
        return booksRepository.findById(bookId).orElseThrow(() -> new IllegalArgumentException("Livro não encontrado"));
    }

    public Books createBookWithImages(Long userId, List<MultipartFile> files) throws IOException {
        Optional<Users> userOptional = usersRepository.findById(userId);
        if (userOptional.isEmpty()) {
            throw new IllegalArgumentException("Usuário não encontrado.");
        }

        Users user = userOptional.get();

        Books book = Books.builder()
                .user(user)
                .totalPages(files.size())
                .createAt(LocalDateTime.now())
                .statusPay(BookPaymentStatus.PENDING)
                .build();

        book = booksRepository.save(book);

        List<ImageBooks> imageBooksList = imageBooksService.processAndSaveImages(book,files);
        book.setImages(imageBooksList);
        return book;
    }

    public String generateBookDownloadUrl(Books book) throws Exception {
        List<ImageBooks> images = imageBooksRepository.findByBookIdIdOrderByOrderIndexAsc(book.getId());

        if (images.isEmpty()) {
            throw new IllegalStateException("Este livro não possui imagens.");
        }

        byte[] pdfBytes = pdfGeneratorService.generatePdfFromImageUrls(images);

        String downloadUrl = cloudinaryService.uploadPdf(pdfBytes, "livro_" + book.getId());

        book.setDownloadUrl(downloadUrl);
        book.setStatusPay(BookPaymentStatus.PAID);
        booksRepository.save(book);

        return downloadUrl;
    }


}
