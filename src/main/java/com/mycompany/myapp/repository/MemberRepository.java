package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.Member;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Spring Data R2DBC repository for the Member entity.
 */
@SuppressWarnings("unused")
@Repository
public interface MemberRepository extends ReactiveCrudRepository<Member, Long>, MemberRepositoryInternal {
    @Override
    <S extends Member> Mono<S> save(S entity);

    @Override
    Flux<Member> findAll();

    @Override
    Mono<Member> findById(Long id);

    @Override
    Mono<Void> deleteById(Long id);

    @Query(
        "SELECT m.name, COUNT(b.id) AS total_borrowed FROM member m JOIN borrow b ON m.id = b.member_id GROUP BY m.name HAVING COUNT(b.id) > 3;}"
    )
    Flux<Member> FindMemberWithMoreThan3Books();

    @Query(
        "SELECT m.id AS MemberId, m.name AS MemberName, m.email, bk.title AS BookTitle, b.borrow_date FROM borrow b JOIN member m ON b.member_id = m.id JOIN Book bk ON b.book_id = bk.id WHERE b.return_date IS NULL;"
    )
    Flux<Member> FindMemberNotReturnBook();
}

interface MemberRepositoryInternal {
    <S extends Member> Mono<S> save(S entity);

    Flux<Member> findAllBy(Pageable pageable);

    Flux<Member> findAll();

    Mono<Member> findById(Long id);
    // this is not supported at the moment because of https://github.com/jhipster/generator-jhipster/issues/18269
    // Flux<Member> findAllBy(Pageable pageable, Criteria criteria);
}
