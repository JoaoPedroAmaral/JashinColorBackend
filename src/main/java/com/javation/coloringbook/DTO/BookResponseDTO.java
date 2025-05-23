package com.javation.coloringbook.DTO;

import com.javation.coloringbook.Entity.Books;
import com.javation.coloringbook.Entity.ImageBooks;
import com.javation.coloringbook.Entity.Users;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
public class BookResponseDTO {
    private Long id;
    private Users user;
    private LocalDateTime createAt;
    private String statusPay;
    private Integer totalPages;
    private String downloadURL;

    public BookResponseDTO(Books book){
        this.id = book.getId();
        this.user = book.getUser();
        this.createAt = book.getCreateAt();
        this.statusPay = book.getStatusPay().name();
        this.totalPages = book.getTotalPages();
    }

}
