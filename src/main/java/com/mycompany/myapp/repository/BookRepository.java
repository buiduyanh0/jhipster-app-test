package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.Book;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Spring Data R2DBC repository for the Book entity.
 */
@SuppressWarnings("unused")
@Repository
public interface BookRepository extends ReactiveCrudRepository<Book, Long>, BookRepositoryInternal {
    @Override
    <S extends Book> Mono<S> save(S entity);

    @Override
    Flux<Book> findAll();

    @Override
    Mono<Book> findById(Long id);

    @Override
    Mono<Void> deleteById(Long id);

    @Query("SELECT * FROM book WHERE Price > 100000;")
    Mono<Void> FindBooks();

    // @Query("SELECT YEAR(borrow_date) AS BorrowYear, COUNT(*) AS TotalBorrowed FROM borrow GROUP BY YEAR(borrow_date) ORDER BY BorrowYear;")
    // Mono<Void> FindTotalBorrowedBookWithYears();

    @Query(
        "SELECT TOP 3 bk.id, bk.title, bk.author, COUNT(b.id) AS TotalBorrowed FROM borrow b JOIN book bk ON b.book_id = bk.id GROUP BY bk.id, bk.title, bk.author ORDER BY COUNT(b.id) DESC;"
    )
    Flux<Book> findTop3Books();
}

interface BookRepositoryInternal {
    <S extends Book> Mono<S> save(S entity);

    Flux<Book> findAllBy(Pageable pageable);

    Flux<Book> findAll();

    Mono<Book> findById(Long id);
    // this is not supported at the moment because of https://github.com/jhipster/generator-jhipster/issues/18269
    // Flux<Book> findAllBy(Pageable pageable, Criteria criteria);
}
