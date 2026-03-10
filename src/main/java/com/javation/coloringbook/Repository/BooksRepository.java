package com.javation.coloringbook.Repository;

import com.javation.coloringbook.Entity.Books;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BooksRepository extends JpaRepository<Books, Long> {
    List<Books> findBookByUserId(Long userId);

    @Query("SELECT b FROM Books b LEFT JOIN FETCH b.images WHERE b.id = :id")
    Optional<Books> findByIdWithImages(@Param("id") Long id);

    @Query("SELECT b FROM Books b LEFT JOIN FETCH b.user WHERE b.id = :id")
    Optional<Books> findByIdWithUser(@Param("id") Long id);

    @Query("SELECT b FROM Books b LEFT JOIN FETCH b.images LEFT JOIN FETCH b.user WHERE b.id = :id")
    Optional<Books> findByIdWithImagesAndUser(@Param("id") Long id);
}
