package com.javation.coloringbook.Entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "book_images")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImageBooks {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "book_id", nullable = false)
    private Books bookId;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;
}
