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

    @Column(name = "title")
    private String title;

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

    @Column(name = "download_url", columnDefinition = "LONGBLOB")
    private byte[] downloadUrl;

    @OneToMany(mappedBy = "bookId", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ImageBooks> images;

    @OneToOne(mappedBy = "bookPay", cascade = CascadeType.ALL, orphanRemoval = true)
    private Payment payment;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Users getUser() {
        return user;
    }

    public void setUser(Users user) {
        this.user = user;
    }

    public LocalDateTime getCreateAt() {
        return createAt;
    }

    public void setCreateAt(LocalDateTime createAt) {
        this.createAt = createAt;
    }

    public BookPaymentStatus getStatusPay() {
        return statusPay;
    }

    public void setStatusPay(BookPaymentStatus statusPay) {
        this.statusPay = statusPay;
    }

    public Integer getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(Integer totalPages) {
        this.totalPages = totalPages;
    }

    public byte[] getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(byte[] downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public List<ImageBooks> getImages() {
        return images;
    }

    public void setImages(List<ImageBooks> images) {
        this.images = images;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }
}
