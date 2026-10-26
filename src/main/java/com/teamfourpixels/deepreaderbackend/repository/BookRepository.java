package com.teamfourpixels.deepreaderbackend.repository;

import com.teamfourpixels.deepreaderbackend.dto.BookDto;
import com.teamfourpixels.deepreaderbackend.model.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {

    @Query("""
        SELECT new com.teamfourpixels.deepreaderbackend.dto.BookDto(
            b.id, b.title, b.author, b.genre, b.language, b.description, b.coverUrl,
            b.createdAt, b.updatedAt,
            (SELECT COUNT(c) FROM Chapter c WHERE c.book = b),
            (SELECT COUNT(a) FROM Annotation a WHERE a.book = b)
        )
        FROM Book b
        ORDER BY b.createdAt DESC
    """)
    List<BookDto> findAllAsDto();

    @Query("""
        SELECT new com.teamfourpixels.deepreaderbackend.dto.BookDto(
            b.id, b.title, b.author, b.genre, b.language, b.description, b.coverUrl,
            b.createdAt, b.updatedAt,
            (SELECT COUNT(c) FROM Chapter c WHERE c.book = b),
            (SELECT COUNT(a) FROM Annotation a WHERE a.book = b)
        )
        FROM Book b
        WHERE LOWER(b.title) LIKE LOWER(CONCAT('%', :query, '%'))
           OR LOWER(b.author) LIKE LOWER(CONCAT('%', :query, '%'))
        ORDER BY b.createdAt DESC
    """)
    List<BookDto> searchAsDto(@Param("query") String query);

    @Query("""
        SELECT new com.teamfourpixels.deepreaderbackend.dto.BookDto(
            b.id, b.title, b.author, b.genre, b.language, b.description, b.coverUrl,
            b.createdAt, b.updatedAt,
            (SELECT COUNT(c) FROM Chapter c WHERE c.book = b),
            (SELECT COUNT(a) FROM Annotation a WHERE a.book = b)
        )
        FROM Book b
        WHERE b.id = :id
    """)
    Optional<BookDto> findDtoById(@Param("id") Long id);
}
