# Code Improvement Prompt - Ready to Copy/Paste

---

## Project Overview

- **Project Name**: Coloring Book Backend
- **Tech Stack**: Java 17, Spring Boot, Maven
- **Database**: [MySQL/PostgreSQL]
- **Key Dependencies**: Spring Security, JWT, Cloudinary, iTextPDF

---

## Current Code

### BookService.java
```java
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

    public byte[] generateBookDownloadUrl(Books book) throws Exception {
        List<ImageBooks> images = imageBooksRepository.findByBookIdIdOrderByOrderIndexAsc(book.getId());

        if (images.isEmpty()) {
            throw new IllegalStateException("Este livro não possui imagens.");
        }

        String bookTitle = "Livro de Colorir - " + book.getUser().getEmail();
        byte[] pdfBytes = pdfGeneratorService.generatePdfFromImageUrls(images, bookTitle);

        String downloadUrl = cloudinaryService.uploadPdf(pdfBytes, "livro_" + book.getId());

        book.setDownloadUrl(pdfBytes);
        book.setStatusPay(BookPaymentStatus.PAID);
        booksRepository.save(book);

        return pdfBytes;
    }
}
```

### UserService.java
```java
package com.javation.coloringbook.Service;

import com.javation.coloringbook.Entity.Users;
import com.javation.coloringbook.Repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UsersRepository usersRepository;

    public Users findUserById(Long id){
        return usersRepository.findById(id).orElseThrow(()-> new IllegalArgumentException("Usuario não encontrado"));
    }

    public Users findUserByEmail(String email){
        return usersRepository.findByEmail(email).orElseThrow(()-> new IllegalArgumentException("Email não encontrado!"));
    }

    public Users createUser(Users user){
        return usersRepository.save(user);
    }
}
```

### PaymentService.java
```java
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
```

---

## What You Want to Improve

- [x] Code readability
- [x] Performance optimization
- [x] Error handling
- [x] Security best practices
- [x] Clean Code / SOLID principles
- [x] Exception handling
- [x] Code duplication
- [x] Missing validation

---

## Specific Concerns

1. **Error Handling**: Currently using `IllegalArgumentException` and `RuntimeException` inconsistently. Need custom exceptions and proper error responses.

2. **Performance**: Potential N+1 query in `generateBookDownloadUrl()` - loading images separately. Also, PDF is generated synchronously which could timeout for large books.

3. **Code Duplication**: Validation logic for finding users/books is repeated across services.

4. **Missing Validation**: No input validation on `files` in `createBookWithImages()`, no null checks, no file size/type validation.

5. **Transaction Management**: `confirmPayment()` updates payment and book status but if PDF generation fails after payment is marked SUCCESS, there's inconsistent state.

6. **Logging**: No logging throughout the services.

7. ** SOLID Principles**:
   - SRP: Services do too much (business logic + data access)
   - BookService has too many dependencies

8. **Security**: No authorization checks - any user could access any book.

---

## Example Prompt (Copy This!)

```
I have a Spring Boot coloring book application and I need help improving my service layer code.

## Current Issues:
1. Inconsistent exception handling (mixing IllegalArgumentException, RuntimeException)
2. No custom exceptions or global error handling
3. Potential N+1 query in generateBookDownloadUrl()
4. No input validation (file size, type, null checks)
5. Transaction issues - payment status can become inconsistent if PDF generation fails
6. No logging anywhere in the code
7. No authorization checks - users can access any book
8. Code duplication across services
9. BookService has too many dependencies (violates SRP)

## Services to improve:
- BookService.java (findBookById, createBookWithImages, generateBookDownloadUrl)
- UserService.java (findUserById, findUserByEmail)
- PaymentService.java (registerPayment, confirmPayment)

## Requirements:
1. Create custom exceptions (UserNotFoundException, BookNotFoundException, PaymentException)
2. Add @Transactional annotations with proper rollback
3. Add input validation using @Valid and custom validators
4. Implement proper logging with SLF4J
5. Add authorization checks to ensure users can only access their own books
6. Fix N+1 query using JOIN FETCH
7. Use DTOs for API responses
8. Add proper error responses with @ControllerAdvice
9. Split BookService if it has too many responsibilities
10. Add async processing for PDF generation

Please provide the improved code with explanations for each change.
```
