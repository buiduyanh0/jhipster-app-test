package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.Book;
import com.mycompany.myapp.domain.Borrow;
import com.mycompany.myapp.domain.Member;
import com.mycompany.myapp.repository.BookRepository;
import com.mycompany.myapp.repository.BorrowRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
public class BorrowService {

    private final BorrowRepository borrowRepository;
    private final BookRepository bookRepository;

    public BorrowService(BorrowRepository borrowRepository, BookRepository bookRepository) {
        this.borrowRepository = borrowRepository;
        this.bookRepository = bookRepository;
    }

    public Mono<Borrow> borrowBook(Long memberId, Long bookId) {
        Borrow borrow = new Borrow();
        borrow.setBorrowDate(Instant.now());
        borrow.setMember(new Member().id(memberId));
        borrow.setBook(new Book().id(bookId));

        // Lưu Borrow và cập nhật trạng thái Book
        return borrowRepository
            .save(borrow)
            .flatMap(savedBorrow ->
                bookRepository
                    .findById(bookId)
                    .flatMap(book -> {
                        book.setAvailable(false);
                        return bookRepository.save(book);
                    })
                    .thenReturn(savedBorrow)
            );
    }
    // public Mono<Map<Integer, Long>> getBooksPerYear() {
    // return borrowRepository.countBooksPerYear()
    //         .collectMap(
    //             tuple -> tuple.getT1(), // year
    //             tuple -> tuple.getT2()  // count
    //         );
    // }
}
