package com.javation.coloringbook.DTO;

import com.javation.coloringbook.Entity.Books;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class BookResponseDTO {
    private Long id;
    private Long userId;
    private String title;
    private LocalDateTime createAt;
    private String statusPay;
    private Integer totalPages;
    private Double price;

    public BookResponseDTO(Books book){
        this.id = book.getId();
        this.userId = book.getUser().getId();
        this.title = book.getTitle();
        this.createAt = book.getCreateAt();
        this.statusPay = book.getStatusPay().name();
        this.totalPages = book.getTotalPages();
        this.price = book.getPrice();
    }
}
