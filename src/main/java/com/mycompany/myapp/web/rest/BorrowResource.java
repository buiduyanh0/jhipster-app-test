package com.mycompany.myapp.web.rest;

import com.mycompany.myapp.domain.Borrow;
import com.mycompany.myapp.repository.BookRepository;
import com.mycompany.myapp.repository.BorrowPerYearProjection;
import com.mycompany.myapp.repository.BorrowRepository;
import com.mycompany.myapp.service.BorrowService;
import com.mycompany.myapp.service.dto.BorrowRequestDTO;
import com.mycompany.myapp.web.rest.errors.BadRequestAlertException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tech.jhipster.web.util.HeaderUtil;
import tech.jhipster.web.util.reactive.ResponseUtil;

/**
 * REST controller for managing {@link com.mycompany.myapp.domain.Borrow}.
 */
@RestController
@RequestMapping("/api/borrows")
@Transactional
public class BorrowResource {

    private static final Logger LOG = LoggerFactory.getLogger(BorrowResource.class);

    private static final String ENTITY_NAME = "borrow";

    @Value("${jhipster.clientApp.name}")
    private String applicationName;

    private final BorrowRepository borrowRepository;
    private final BorrowService borrowService;
    private final BookRepository bookRepository;
    private final TransactionalOperator txOperator;

    public BorrowResource(
        BorrowRepository borrowRepository,
        BorrowService borrowService,
        BookRepository bookRepository,
        R2dbcTransactionManager txManager
    ) {
        this.borrowRepository = borrowRepository;
        this.borrowService = borrowService;
        this.bookRepository = bookRepository;
        this.txOperator = TransactionalOperator.create(txManager);
    }

    // public BorrowService(BorrowService borrowService) {
    //     this.borrowService = borrowService;
    // }

    /**
     * {@code POST  /borrows} : Create a new borrow.
     *
     * @param borrow the borrow to create.
     * @return the {@link ResponseEntity} with status {@code 201 (Created)} and with body the new borrow, or with status {@code 400 (Bad Request)} if the borrow has already an ID.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PostMapping("")
    public Mono<ResponseEntity<Borrow>> createBorrow(@Valid @RequestBody Borrow borrow) throws URISyntaxException {
        LOG.debug("REST request to save Borrow : {}", borrow);
        if (borrow.getId() != null) {
            throw new BadRequestAlertException("A new borrow cannot already have an ID", ENTITY_NAME, "idexists");
        }
        return borrowRepository
            .save(borrow)
            .flatMap(savedBorrow -> {
                return bookRepository
                    .findById(savedBorrow.getBookId())
                    .flatMap(book -> {
                        book.setAvailable(false);
                        return bookRepository.save(book); // Mono<Book>
                    })
                    .thenReturn(savedBorrow); // Mono<Borrow>
            })
            .map(savedBorrow -> {
                try {
                    return ResponseEntity.created(new URI("/api/borrows/" + savedBorrow.getId()))
                        .headers(HeaderUtil.createEntityCreationAlert(applicationName, false, ENTITY_NAME, savedBorrow.getId().toString()))
                        .body(savedBorrow);
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
            })
            .as(txOperator::transactional); // ✅ Wrap trong transaction reactive
    }

    /**
     * {@code PUT  /borrows/:id} : Updates an existing borrow.
     *
     * @param id the id of the borrow to save.
     * @param borrow the borrow to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated borrow,
     * or with status {@code 400 (Bad Request)} if the borrow is not valid,
     * or with status {@code 500 (Internal Server Error)} if the borrow couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PutMapping("/{id}")
    public Mono<ResponseEntity<Borrow>> updateBorrow(
        @PathVariable(value = "id", required = false) final Long id,
        @Valid @RequestBody Borrow borrow
    ) throws URISyntaxException {
        LOG.debug("REST request to update Borrow : {}, {}", id, borrow);
        if (borrow.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, borrow.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        return borrowRepository
            .existsById(id)
            .flatMap(exists -> {
                if (!exists) {
                    return Mono.error(new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound"));
                }

                return borrowRepository
                    .save(borrow)
                    .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)))
                    .map(result ->
                        ResponseEntity.ok()
                            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, result.getId().toString()))
                            .body(result)
                    );
            });
    }

    /**
     * {@code PATCH  /borrows/:id} : Partial updates given fields of an existing borrow, field will ignore if it is null
     *
     * @param id the id of the borrow to save.
     * @param borrow the borrow to update.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the updated borrow,
     * or with status {@code 400 (Bad Request)} if the borrow is not valid,
     * or with status {@code 404 (Not Found)} if the borrow is not found,
     * or with status {@code 500 (Internal Server Error)} if the borrow couldn't be updated.
     * @throws URISyntaxException if the Location URI syntax is incorrect.
     */
    @PatchMapping(value = "/{id}", consumes = { "application/json", "application/merge-patch+json" })
    public Mono<ResponseEntity<Borrow>> partialUpdateBorrow(
        @PathVariable(value = "id", required = false) final Long id,
        @NotNull @RequestBody Borrow borrow
    ) throws URISyntaxException {
        LOG.debug("REST request to partial update Borrow partially : {}, {}", id, borrow);
        if (borrow.getId() == null) {
            throw new BadRequestAlertException("Invalid id", ENTITY_NAME, "idnull");
        }
        if (!Objects.equals(id, borrow.getId())) {
            throw new BadRequestAlertException("Invalid ID", ENTITY_NAME, "idinvalid");
        }

        return borrowRepository
            .existsById(id)
            .flatMap(exists -> {
                if (!exists) {
                    return Mono.error(new BadRequestAlertException("Entity not found", ENTITY_NAME, "idnotfound"));
                }

                Mono<Borrow> result = borrowRepository
                    .findById(borrow.getId())
                    .map(existingBorrow -> {
                        if (borrow.getBorrowDate() != null) {
                            existingBorrow.setBorrowDate(borrow.getBorrowDate());
                        }
                        if (borrow.getReturnDate() != null) {
                            existingBorrow.setReturnDate(borrow.getReturnDate());
                        }

                        return existingBorrow;
                    })
                    .flatMap(borrowRepository::save);

                return result
                    .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND)))
                    .map(res ->
                        ResponseEntity.ok()
                            .headers(HeaderUtil.createEntityUpdateAlert(applicationName, false, ENTITY_NAME, res.getId().toString()))
                            .body(res)
                    );
            });
    }

    /**
     * {@code GET  /borrows} : get all the borrows.
     *
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and the list of borrows in body.
     */
    @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<List<Borrow>> getAllBorrows() {
        LOG.debug("REST request to get all Borrows");
        return borrowRepository.findAll().collectList();
    }

    @GetMapping("/statistics/books-per-year")
    public Flux<BorrowPerYearProjection> getBooksPerYear() {
        return borrowRepository.countBooksPerYear();
    }

    /**
     * {@code GET  /borrows} : get all the borrows as a stream.
     * @return the {@link Flux} of borrows.
     */
    @GetMapping(value = "", produces = MediaType.APPLICATION_NDJSON_VALUE)
    public Flux<Borrow> getAllBorrowsAsStream() {
        LOG.debug("REST request to get all Borrows as a stream");
        return borrowRepository.findAll();
    }

    /**
     * {@code GET  /borrows/:id} : get the "id" borrow.
     *
     * @param id the id of the borrow to retrieve.
     * @return the {@link ResponseEntity} with status {@code 200 (OK)} and with body the borrow, or with status {@code 404 (Not Found)}.
     */
    @PostMapping("/getBorrowById")
    public Mono<ResponseEntity<Borrow>> getBorrow(@RequestBody BorrowRequestDTO request) {
        Long id = request.getId();
        LOG.debug("REST request to get Borrow : {}", id);
        Mono<Borrow> borrow = borrowRepository.findById(id);
        return ResponseUtil.wrapOrNotFound(borrow);
    }

    /**
     * {@code DELETE  /borrows/:id} : delete the "id" borrow.
     *
     * @param id the id of the borrow to delete.
     * @return the {@link ResponseEntity} with status {@code 204 (NO_CONTENT)}.
     */
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Void>> deleteBorrow(@PathVariable("id") Long id) {
        LOG.debug("REST request to delete Borrow : {}", id);
        return borrowRepository
            .deleteById(id)
            .then(
                Mono.just(
                    ResponseEntity.noContent()
                        .headers(HeaderUtil.createEntityDeletionAlert(applicationName, false, ENTITY_NAME, id.toString()))
                        .build()
                )
            );
    }
}
