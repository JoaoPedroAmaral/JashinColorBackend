package com.javation.coloringbook.Repository;

import com.javation.coloringbook.Entity.ImageBooks;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImageBooksRepository extends JpaRepository<ImageBooks, Long> {
    List<ImageBooks> findByBookIdIdOrderByOrderIndexAsc(Long bookId);
    List<ImageBooks> findByBookIdId(Long bookId);
}
