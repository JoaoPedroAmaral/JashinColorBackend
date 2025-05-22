package com.javation.coloringbook.Repository;

import com.javation.coloringbook.Entity.Books;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BooksRepository extends JpaRepository<Books, Long> {
    List<Books> findBookByUserId(Long userId);
}
