package com.javation.coloringbook.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "books")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Books {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private Users user;

    @Column(name = "created_datetime")
    private LocalDateTime createAt = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "status_pay")
    private BookPaymentStatus statusPay = BookPaymentStatus.PENDING;

    @Column(name = "total_pages")
    private Integer totalPages;

    @Column(name = "download_url")
    private String downloadUrl;

    @OneToMany(mappedBy = "bookId", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ImageBooks> images;

    @OneToOne(mappedBy = "bookPay", cascade = CascadeType.ALL, orphanRemoval = true)
    private Payment payment;
}
