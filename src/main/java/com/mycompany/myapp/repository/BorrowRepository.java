package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.Borrow;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;

/**
 * Spring Data R2DBC repository for the Borrow entity.
 */
@SuppressWarnings("unused")
@Repository
public interface BorrowRepository extends ReactiveCrudRepository<Borrow, Long>, BorrowRepositoryInternal {
    @Query("SELECT * FROM borrow entity WHERE entity.member_id = :id")
    Flux<Borrow> findByMember(Long id);

    @Query("SELECT * FROM borrow entity WHERE entity.member_id IS NULL")
    Flux<Borrow> findAllWhereMemberIsNull();

    @Query("SELECT * FROM borrow entity WHERE entity.book_id = :id")
    Flux<Borrow> findByBook(Long id);

    @Query("SELECT * FROM borrow entity WHERE entity.book_id IS NULL")
    Flux<Borrow> findAllWhereBookIsNull();

    @Override
    <S extends Borrow> Mono<S> save(S entity);

    @Override
    Flux<Borrow> findAll();

    @Override
    Mono<Borrow> findById(Long id);

    @Override
    Mono<Void> deleteById(Long id);

    @Query("DELETE FROM borrow WHERE return_date < '2020-01-01';")
    Flux<Borrow> DeleteBorrow();

    @Query("SELECT YEAR(borrow_date) AS year, COUNT(*) AS total FROM borrow GROUP BY YEAR(borrow_date) ORDER BY YEAR(borrow_date) ASC")
    Flux<BorrowPerYearProjection> countBooksPerYear();
}

interface BorrowRepositoryInternal {
    <S extends Borrow> Mono<S> save(S entity);

    Flux<Borrow> findAllBy(Pageable pageable);

    Flux<Borrow> findAll();

    Mono<Borrow> findById(Long id);
    // this is not supported at the moment because of https://github.com/jhipster/generator-jhipster/issues/18269
    // Flux<Borrow> findAllBy(Pageable pageable, Criteria criteria);
}
