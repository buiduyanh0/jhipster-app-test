package com.mycompany.myapp.service;

import com.mycompany.myapp.domain.Book;
import com.mycompany.myapp.repository.BookRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class BookService {

    private final BookRepository bookRepository;

    public BookService(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    public Flux<Book> getTop3Books() {
        return bookRepository.findTop3Books();
    }
}
