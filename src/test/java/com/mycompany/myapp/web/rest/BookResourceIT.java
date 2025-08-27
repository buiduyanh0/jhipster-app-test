package com.mycompany.myapp.web.rest;

import static com.mycompany.myapp.domain.BookAsserts.*;
import static com.mycompany.myapp.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.domain.Book;
import com.mycompany.myapp.repository.BookRepository;
import com.mycompany.myapp.repository.EntityManager;
import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.reactive.server.WebTestClient;

/**
 * Integration tests for the {@link BookResource} REST controller.
 */
@IntegrationTest
@AutoConfigureWebTestClient(timeout = IntegrationTest.DEFAULT_ENTITY_TIMEOUT)
@WithMockUser
class BookResourceIT {

    private static final String DEFAULT_TITLE = "AAAAAAAAAA";
    private static final String UPDATED_TITLE = "BBBBBBBBBB";

    private static final String DEFAULT_AUTHOR = "AAAAAAAAAA";
    private static final String UPDATED_AUTHOR = "BBBBBBBBBB";

    private static final Integer DEFAULT_PUBLISHED_YEAR = 1;
    private static final Integer UPDATED_PUBLISHED_YEAR = 2;

    private static final Double DEFAULT_PRICE = 1D;
    private static final Double UPDATED_PRICE = 2D;

    private static final Boolean DEFAULT_AVAILABLE = false;
    private static final Boolean UPDATED_AVAILABLE = true;

    private static final String ENTITY_API_URL = "/api/books";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private WebTestClient webTestClient;

    private Book book;

    private Book insertedBook;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Book createEntity() {
        return new Book()
            .title(DEFAULT_TITLE)
            .author(DEFAULT_AUTHOR)
            .publishedYear(DEFAULT_PUBLISHED_YEAR)
            .price(DEFAULT_PRICE)
            .available(DEFAULT_AVAILABLE);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Book createUpdatedEntity() {
        return new Book()
            .title(UPDATED_TITLE)
            .author(UPDATED_AUTHOR)
            .publishedYear(UPDATED_PUBLISHED_YEAR)
            .price(UPDATED_PRICE)
            .available(UPDATED_AVAILABLE);
    }

    public static void deleteEntities(EntityManager em) {
        try {
            em.deleteAll(Book.class).block();
        } catch (Exception e) {
            // It can fail, if other entities are still referring this - it will be removed later.
        }
    }

    @BeforeEach
    void initTest() {
        book = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedBook != null) {
            bookRepository.delete(insertedBook).block();
            insertedBook = null;
        }
        deleteEntities(em);
    }

    @Test
    void createBook() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Book
        var returnedBook = webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(book))
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody(Book.class)
            .returnResult()
            .getResponseBody();

        // Validate the Book in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        assertBookUpdatableFieldsEquals(returnedBook, getPersistedBook(returnedBook));

        insertedBook = returnedBook;
    }

    @Test
    void createBookWithExistingId() throws Exception {
        // Create the Book with an existing ID
        book.setId(1L);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(book))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Book in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    void checkTitleIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        book.setTitle(null);

        // Create the Book, which fails.

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(book))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void checkAuthorIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        book.setAuthor(null);

        // Create the Book, which fails.

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(book))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void getAllBooksAsStream() {
        // Initialize the database
        bookRepository.save(book).block();

        List<Book> bookList = webTestClient
            .get()
            .uri(ENTITY_API_URL)
            .accept(MediaType.APPLICATION_NDJSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_NDJSON)
            .returnResult(Book.class)
            .getResponseBody()
            .filter(book::equals)
            .collectList()
            .block(Duration.ofSeconds(5));

        assertThat(bookList).isNotNull();
        assertThat(bookList).hasSize(1);
        Book testBook = bookList.get(0);

        // Test fails because reactive api returns an empty object instead of null
        // assertBookAllPropertiesEquals(book, testBook);
        assertBookUpdatableFieldsEquals(book, testBook);
    }

    @Test
    void getAllBooks() {
        // Initialize the database
        insertedBook = bookRepository.save(book).block();

        // Get all the bookList
        webTestClient
            .get()
            .uri(ENTITY_API_URL + "?sort=id,desc")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.[*].id")
            .value(hasItem(book.getId().intValue()))
            .jsonPath("$.[*].title")
            .value(hasItem(DEFAULT_TITLE))
            .jsonPath("$.[*].author")
            .value(hasItem(DEFAULT_AUTHOR))
            .jsonPath("$.[*].publishedYear")
            .value(hasItem(DEFAULT_PUBLISHED_YEAR))
            .jsonPath("$.[*].price")
            .value(hasItem(DEFAULT_PRICE))
            .jsonPath("$.[*].available")
            .value(hasItem(DEFAULT_AVAILABLE));
    }

    @Test
    void getBook() {
        // Initialize the database
        insertedBook = bookRepository.save(book).block();

        // Get the book
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, book.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.id")
            .value(is(book.getId().intValue()))
            .jsonPath("$.title")
            .value(is(DEFAULT_TITLE))
            .jsonPath("$.author")
            .value(is(DEFAULT_AUTHOR))
            .jsonPath("$.publishedYear")
            .value(is(DEFAULT_PUBLISHED_YEAR))
            .jsonPath("$.price")
            .value(is(DEFAULT_PRICE))
            .jsonPath("$.available")
            .value(is(DEFAULT_AVAILABLE));
    }

    @Test
    void getNonExistingBook() {
        // Get the book
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, Long.MAX_VALUE)
            .accept(MediaType.APPLICATION_PROBLEM_JSON)
            .exchange()
            .expectStatus()
            .isNotFound();
    }

    @Test
    void putExistingBook() throws Exception {
        // Initialize the database
        insertedBook = bookRepository.save(book).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the book
        Book updatedBook = bookRepository.findById(book.getId()).block();
        updatedBook
            .title(UPDATED_TITLE)
            .author(UPDATED_AUTHOR)
            .publishedYear(UPDATED_PUBLISHED_YEAR)
            .price(UPDATED_PRICE)
            .available(UPDATED_AVAILABLE);

        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, updatedBook.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(updatedBook))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Book in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedBookToMatchAllProperties(updatedBook);
    }

    @Test
    void putNonExistingBook() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        book.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, book.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(book))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Book in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithIdMismatchBook() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        book.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, longCount.incrementAndGet())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(book))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Book in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithMissingIdPathParamBook() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        book.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(book))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the Book in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void partialUpdateBookWithPatch() throws Exception {
        // Initialize the database
        insertedBook = bookRepository.save(book).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the book using partial update
        Book partialUpdatedBook = new Book();
        partialUpdatedBook.setId(book.getId());

        partialUpdatedBook.price(UPDATED_PRICE).available(UPDATED_AVAILABLE);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedBook.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(partialUpdatedBook))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Book in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertBookUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedBook, book), getPersistedBook(book));
    }

    @Test
    void fullUpdateBookWithPatch() throws Exception {
        // Initialize the database
        insertedBook = bookRepository.save(book).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the book using partial update
        Book partialUpdatedBook = new Book();
        partialUpdatedBook.setId(book.getId());

        partialUpdatedBook
            .title(UPDATED_TITLE)
            .author(UPDATED_AUTHOR)
            .publishedYear(UPDATED_PUBLISHED_YEAR)
            .price(UPDATED_PRICE)
            .available(UPDATED_AVAILABLE);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedBook.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(partialUpdatedBook))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Book in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertBookUpdatableFieldsEquals(partialUpdatedBook, getPersistedBook(partialUpdatedBook));
    }

    @Test
    void patchNonExistingBook() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        book.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, book.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(book))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Book in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithIdMismatchBook() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        book.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, longCount.incrementAndGet())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(book))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Book in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithMissingIdPathParamBook() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        book.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(book))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the Book in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void deleteBook() {
        // Initialize the database
        insertedBook = bookRepository.save(book).block();

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the book
        webTestClient
            .delete()
            .uri(ENTITY_API_URL_ID, book.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent();

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return bookRepository.count().block();
    }

    protected void assertIncrementedRepositoryCount(long countBefore) {
        assertThat(countBefore + 1).isEqualTo(getRepositoryCount());
    }

    protected void assertDecrementedRepositoryCount(long countBefore) {
        assertThat(countBefore - 1).isEqualTo(getRepositoryCount());
    }

    protected void assertSameRepositoryCount(long countBefore) {
        assertThat(countBefore).isEqualTo(getRepositoryCount());
    }

    protected Book getPersistedBook(Book book) {
        return bookRepository.findById(book.getId()).block();
    }

    protected void assertPersistedBookToMatchAllProperties(Book expectedBook) {
        // Test fails because reactive api returns an empty object instead of null
        // assertBookAllPropertiesEquals(expectedBook, getPersistedBook(expectedBook));
        assertBookUpdatableFieldsEquals(expectedBook, getPersistedBook(expectedBook));
    }

    protected void assertPersistedBookToMatchUpdatableProperties(Book expectedBook) {
        // Test fails because reactive api returns an empty object instead of null
        // assertBookAllUpdatablePropertiesEquals(expectedBook, getPersistedBook(expectedBook));
        assertBookUpdatableFieldsEquals(expectedBook, getPersistedBook(expectedBook));
    }
}
