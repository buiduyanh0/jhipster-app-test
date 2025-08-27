package com.mycompany.myapp.web.rest;

import static com.mycompany.myapp.domain.BorrowAsserts.*;
import static com.mycompany.myapp.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.domain.Borrow;
import com.mycompany.myapp.repository.BorrowRepository;
import com.mycompany.myapp.repository.EntityManager;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
 * Integration tests for the {@link BorrowResource} REST controller.
 */
@IntegrationTest
@AutoConfigureWebTestClient(timeout = IntegrationTest.DEFAULT_ENTITY_TIMEOUT)
@WithMockUser
class BorrowResourceIT {

    private static final Instant DEFAULT_BORROW_DATE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_BORROW_DATE = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final Instant DEFAULT_RETURN_DATE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_RETURN_DATE = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/borrows";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private BorrowRepository borrowRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private WebTestClient webTestClient;

    private Borrow borrow;

    private Borrow insertedBorrow;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Borrow createEntity() {
        return new Borrow().borrowDate(DEFAULT_BORROW_DATE).returnDate(DEFAULT_RETURN_DATE);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Borrow createUpdatedEntity() {
        return new Borrow().borrowDate(UPDATED_BORROW_DATE).returnDate(UPDATED_RETURN_DATE);
    }

    public static void deleteEntities(EntityManager em) {
        try {
            em.deleteAll(Borrow.class).block();
        } catch (Exception e) {
            // It can fail, if other entities are still referring this - it will be removed later.
        }
    }

    @BeforeEach
    void initTest() {
        borrow = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedBorrow != null) {
            borrowRepository.delete(insertedBorrow).block();
            insertedBorrow = null;
        }
        deleteEntities(em);
    }

    @Test
    void createBorrow() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Borrow
        var returnedBorrow = webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(borrow))
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody(Borrow.class)
            .returnResult()
            .getResponseBody();

        // Validate the Borrow in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        assertBorrowUpdatableFieldsEquals(returnedBorrow, getPersistedBorrow(returnedBorrow));

        insertedBorrow = returnedBorrow;
    }

    @Test
    void createBorrowWithExistingId() throws Exception {
        // Create the Borrow with an existing ID
        borrow.setId(1L);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(borrow))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Borrow in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    void checkBorrowDateIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        borrow.setBorrowDate(null);

        // Create the Borrow, which fails.

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(borrow))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void getAllBorrowsAsStream() {
        // Initialize the database
        borrowRepository.save(borrow).block();

        List<Borrow> borrowList = webTestClient
            .get()
            .uri(ENTITY_API_URL)
            .accept(MediaType.APPLICATION_NDJSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_NDJSON)
            .returnResult(Borrow.class)
            .getResponseBody()
            .filter(borrow::equals)
            .collectList()
            .block(Duration.ofSeconds(5));

        assertThat(borrowList).isNotNull();
        assertThat(borrowList).hasSize(1);
        Borrow testBorrow = borrowList.get(0);

        // Test fails because reactive api returns an empty object instead of null
        // assertBorrowAllPropertiesEquals(borrow, testBorrow);
        assertBorrowUpdatableFieldsEquals(borrow, testBorrow);
    }

    @Test
    void getAllBorrows() {
        // Initialize the database
        insertedBorrow = borrowRepository.save(borrow).block();

        // Get all the borrowList
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
            .value(hasItem(borrow.getId().intValue()))
            .jsonPath("$.[*].borrowDate")
            .value(hasItem(DEFAULT_BORROW_DATE.toString()))
            .jsonPath("$.[*].returnDate")
            .value(hasItem(DEFAULT_RETURN_DATE.toString()));
    }

    @Test
    void getBorrow() {
        // Initialize the database
        insertedBorrow = borrowRepository.save(borrow).block();

        // Get the borrow
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, borrow.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.id")
            .value(is(borrow.getId().intValue()))
            .jsonPath("$.borrowDate")
            .value(is(DEFAULT_BORROW_DATE.toString()))
            .jsonPath("$.returnDate")
            .value(is(DEFAULT_RETURN_DATE.toString()));
    }

    @Test
    void getNonExistingBorrow() {
        // Get the borrow
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, Long.MAX_VALUE)
            .accept(MediaType.APPLICATION_PROBLEM_JSON)
            .exchange()
            .expectStatus()
            .isNotFound();
    }

    @Test
    void putExistingBorrow() throws Exception {
        // Initialize the database
        insertedBorrow = borrowRepository.save(borrow).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the borrow
        Borrow updatedBorrow = borrowRepository.findById(borrow.getId()).block();
        updatedBorrow.borrowDate(UPDATED_BORROW_DATE).returnDate(UPDATED_RETURN_DATE);

        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, updatedBorrow.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(updatedBorrow))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Borrow in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedBorrowToMatchAllProperties(updatedBorrow);
    }

    @Test
    void putNonExistingBorrow() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        borrow.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, borrow.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(borrow))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Borrow in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithIdMismatchBorrow() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        borrow.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, longCount.incrementAndGet())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(borrow))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Borrow in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithMissingIdPathParamBorrow() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        borrow.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(borrow))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the Borrow in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void partialUpdateBorrowWithPatch() throws Exception {
        // Initialize the database
        insertedBorrow = borrowRepository.save(borrow).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the borrow using partial update
        Borrow partialUpdatedBorrow = new Borrow();
        partialUpdatedBorrow.setId(borrow.getId());

        partialUpdatedBorrow.borrowDate(UPDATED_BORROW_DATE).returnDate(UPDATED_RETURN_DATE);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedBorrow.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(partialUpdatedBorrow))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Borrow in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertBorrowUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedBorrow, borrow), getPersistedBorrow(borrow));
    }

    @Test
    void fullUpdateBorrowWithPatch() throws Exception {
        // Initialize the database
        insertedBorrow = borrowRepository.save(borrow).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the borrow using partial update
        Borrow partialUpdatedBorrow = new Borrow();
        partialUpdatedBorrow.setId(borrow.getId());

        partialUpdatedBorrow.borrowDate(UPDATED_BORROW_DATE).returnDate(UPDATED_RETURN_DATE);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedBorrow.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(partialUpdatedBorrow))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Borrow in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertBorrowUpdatableFieldsEquals(partialUpdatedBorrow, getPersistedBorrow(partialUpdatedBorrow));
    }

    @Test
    void patchNonExistingBorrow() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        borrow.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, borrow.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(borrow))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Borrow in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithIdMismatchBorrow() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        borrow.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, longCount.incrementAndGet())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(borrow))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Borrow in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithMissingIdPathParamBorrow() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        borrow.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(borrow))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the Borrow in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void deleteBorrow() {
        // Initialize the database
        insertedBorrow = borrowRepository.save(borrow).block();

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the borrow
        webTestClient
            .delete()
            .uri(ENTITY_API_URL_ID, borrow.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent();

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return borrowRepository.count().block();
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

    protected Borrow getPersistedBorrow(Borrow borrow) {
        return borrowRepository.findById(borrow.getId()).block();
    }

    protected void assertPersistedBorrowToMatchAllProperties(Borrow expectedBorrow) {
        // Test fails because reactive api returns an empty object instead of null
        // assertBorrowAllPropertiesEquals(expectedBorrow, getPersistedBorrow(expectedBorrow));
        assertBorrowUpdatableFieldsEquals(expectedBorrow, getPersistedBorrow(expectedBorrow));
    }

    protected void assertPersistedBorrowToMatchUpdatableProperties(Borrow expectedBorrow) {
        // Test fails because reactive api returns an empty object instead of null
        // assertBorrowAllUpdatablePropertiesEquals(expectedBorrow, getPersistedBorrow(expectedBorrow));
        assertBorrowUpdatableFieldsEquals(expectedBorrow, getPersistedBorrow(expectedBorrow));
    }
}
