package com.javation.coloringbook.Controller;

import com.javation.coloringbook.DTO.BookResponseDTO;
import com.javation.coloringbook.Entity.Books;
import com.javation.coloringbook.Entity.ImageBooks;
import com.javation.coloringbook.Service.BookService;
import com.javation.coloringbook.Service.ImageBooksService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/books")
public class BookController {

    private final BookService bookService;
    private final ImageBooksService imageBooksService;

    @PostMapping
    public ResponseEntity<?> postBook (@RequestParam Long userId, @RequestParam List<MultipartFile> files) throws IOException {
        Books books = bookService.createBookWithImages(userId, files);
        return ResponseEntity.ok(new BookResponseDTO(books));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<Books>> getBook (@PathVariable Long useId){
        List<Books> books = bookService.findBookByUserId(useId);
        return ResponseEntity.ok(books);
    }

    @GetMapping("/{bookId}/images")
    public ResponseEntity<List<ImageBooks>> getBookImage (@PathVariable Long bookId){
        List<ImageBooks> images = imageBooksService.findByBookId(bookId);
        return ResponseEntity.ok(images);
    }

    @GetMapping("/{bookId}/download-url")
    public ResponseEntity<byte[]> getDownloadUrl(@PathVariable Long bookId){
        Books books = bookService.findBookById(bookId);
        byte[] pdfBytes = books.getDownloadUrl();

        return ResponseEntity.ok()
                .header("Content-Type", "application/pdf")
                .header("Content-Disposition", "inline; filename=\"livro_" + books.getId() + ".pdf\"")
                .body(pdfBytes);
    }
}
