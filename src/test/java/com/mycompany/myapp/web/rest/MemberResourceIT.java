package com.mycompany.myapp.web.rest;

import static com.mycompany.myapp.domain.MemberAsserts.*;
import static com.mycompany.myapp.web.rest.TestUtil.createUpdateProxyForBean;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.myapp.IntegrationTest;
import com.mycompany.myapp.domain.Member;
import com.mycompany.myapp.repository.EntityManager;
import com.mycompany.myapp.repository.MemberRepository;
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
 * Integration tests for the {@link MemberResource} REST controller.
 */
@IntegrationTest
@AutoConfigureWebTestClient(timeout = IntegrationTest.DEFAULT_ENTITY_TIMEOUT)
@WithMockUser
class MemberResourceIT {

    private static final String DEFAULT_NAME = "AAAAAAAAAA";
    private static final String UPDATED_NAME = "BBBBBBBBBB";

    private static final String DEFAULT_EMAIL = "AAAAAAAAAA";
    private static final String UPDATED_EMAIL = "BBBBBBBBBB";

    private static final Instant DEFAULT_JOIN_DATE = Instant.ofEpochMilli(0L);
    private static final Instant UPDATED_JOIN_DATE = Instant.now().truncatedTo(ChronoUnit.MILLIS);

    private static final String ENTITY_API_URL = "/api/members";
    private static final String ENTITY_API_URL_ID = ENTITY_API_URL + "/{id}";

    private static Random random = new Random();
    private static AtomicLong longCount = new AtomicLong(random.nextInt() + (2 * Integer.MAX_VALUE));

    @Autowired
    private ObjectMapper om;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private EntityManager em;

    @Autowired
    private WebTestClient webTestClient;

    private Member member;

    private Member insertedMember;

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Member createEntity() {
        return new Member().name(DEFAULT_NAME).email(DEFAULT_EMAIL).joinDate(DEFAULT_JOIN_DATE);
    }

    /**
     * Create an updated entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Member createUpdatedEntity() {
        return new Member().name(UPDATED_NAME).email(UPDATED_EMAIL).joinDate(UPDATED_JOIN_DATE);
    }

    public static void deleteEntities(EntityManager em) {
        try {
            em.deleteAll(Member.class).block();
        } catch (Exception e) {
            // It can fail, if other entities are still referring this - it will be removed later.
        }
    }

    @BeforeEach
    void initTest() {
        member = createEntity();
    }

    @AfterEach
    void cleanup() {
        if (insertedMember != null) {
            memberRepository.delete(insertedMember).block();
            insertedMember = null;
        }
        deleteEntities(em);
    }

    @Test
    void createMember() throws Exception {
        long databaseSizeBeforeCreate = getRepositoryCount();
        // Create the Member
        var returnedMember = webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(member))
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody(Member.class)
            .returnResult()
            .getResponseBody();

        // Validate the Member in the database
        assertIncrementedRepositoryCount(databaseSizeBeforeCreate);
        assertMemberUpdatableFieldsEquals(returnedMember, getPersistedMember(returnedMember));

        insertedMember = returnedMember;
    }

    @Test
    void createMemberWithExistingId() throws Exception {
        // Create the Member with an existing ID
        member.setId(1L);

        long databaseSizeBeforeCreate = getRepositoryCount();

        // An entity with an existing ID cannot be created, so this API call must fail
        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(member))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Member in the database
        assertSameRepositoryCount(databaseSizeBeforeCreate);
    }

    @Test
    void checkNameIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        member.setName(null);

        // Create the Member, which fails.

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(member))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void checkEmailIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        member.setEmail(null);

        // Create the Member, which fails.

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(member))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void checkJoinDateIsRequired() throws Exception {
        long databaseSizeBeforeTest = getRepositoryCount();
        // set the field null
        member.setJoinDate(null);

        // Create the Member, which fails.

        webTestClient
            .post()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(member))
            .exchange()
            .expectStatus()
            .isBadRequest();

        assertSameRepositoryCount(databaseSizeBeforeTest);
    }

    @Test
    void getAllMembersAsStream() {
        // Initialize the database
        memberRepository.save(member).block();

        List<Member> memberList = webTestClient
            .get()
            .uri(ENTITY_API_URL)
            .accept(MediaType.APPLICATION_NDJSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentTypeCompatibleWith(MediaType.APPLICATION_NDJSON)
            .returnResult(Member.class)
            .getResponseBody()
            .filter(member::equals)
            .collectList()
            .block(Duration.ofSeconds(5));

        assertThat(memberList).isNotNull();
        assertThat(memberList).hasSize(1);
        Member testMember = memberList.get(0);

        // Test fails because reactive api returns an empty object instead of null
        // assertMemberAllPropertiesEquals(member, testMember);
        assertMemberUpdatableFieldsEquals(member, testMember);
    }

    @Test
    void getAllMembers() {
        // Initialize the database
        insertedMember = memberRepository.save(member).block();

        // Get all the memberList
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
            .value(hasItem(member.getId().intValue()))
            .jsonPath("$.[*].name")
            .value(hasItem(DEFAULT_NAME))
            .jsonPath("$.[*].email")
            .value(hasItem(DEFAULT_EMAIL))
            .jsonPath("$.[*].joinDate")
            .value(hasItem(DEFAULT_JOIN_DATE.toString()));
    }

    @Test
    void getMember() {
        // Initialize the database
        insertedMember = memberRepository.save(member).block();

        // Get the member
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, member.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk()
            .expectHeader()
            .contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.id")
            .value(is(member.getId().intValue()))
            .jsonPath("$.name")
            .value(is(DEFAULT_NAME))
            .jsonPath("$.email")
            .value(is(DEFAULT_EMAIL))
            .jsonPath("$.joinDate")
            .value(is(DEFAULT_JOIN_DATE.toString()));
    }

    @Test
    void getNonExistingMember() {
        // Get the member
        webTestClient
            .get()
            .uri(ENTITY_API_URL_ID, Long.MAX_VALUE)
            .accept(MediaType.APPLICATION_PROBLEM_JSON)
            .exchange()
            .expectStatus()
            .isNotFound();
    }

    @Test
    void putExistingMember() throws Exception {
        // Initialize the database
        insertedMember = memberRepository.save(member).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the member
        Member updatedMember = memberRepository.findById(member.getId()).block();
        updatedMember.name(UPDATED_NAME).email(UPDATED_EMAIL).joinDate(UPDATED_JOIN_DATE);

        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, updatedMember.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(updatedMember))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Member in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertPersistedMemberToMatchAllProperties(updatedMember);
    }

    @Test
    void putNonExistingMember() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        member.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, member.getId())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(member))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Member in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithIdMismatchMember() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        member.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL_ID, longCount.incrementAndGet())
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(member))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Member in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void putWithMissingIdPathParamMember() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        member.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .put()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(om.writeValueAsBytes(member))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the Member in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void partialUpdateMemberWithPatch() throws Exception {
        // Initialize the database
        insertedMember = memberRepository.save(member).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the member using partial update
        Member partialUpdatedMember = new Member();
        partialUpdatedMember.setId(member.getId());

        partialUpdatedMember.email(UPDATED_EMAIL);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedMember.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(partialUpdatedMember))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Member in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertMemberUpdatableFieldsEquals(createUpdateProxyForBean(partialUpdatedMember, member), getPersistedMember(member));
    }

    @Test
    void fullUpdateMemberWithPatch() throws Exception {
        // Initialize the database
        insertedMember = memberRepository.save(member).block();

        long databaseSizeBeforeUpdate = getRepositoryCount();

        // Update the member using partial update
        Member partialUpdatedMember = new Member();
        partialUpdatedMember.setId(member.getId());

        partialUpdatedMember.name(UPDATED_NAME).email(UPDATED_EMAIL).joinDate(UPDATED_JOIN_DATE);

        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, partialUpdatedMember.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(partialUpdatedMember))
            .exchange()
            .expectStatus()
            .isOk();

        // Validate the Member in the database

        assertSameRepositoryCount(databaseSizeBeforeUpdate);
        assertMemberUpdatableFieldsEquals(partialUpdatedMember, getPersistedMember(partialUpdatedMember));
    }

    @Test
    void patchNonExistingMember() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        member.setId(longCount.incrementAndGet());

        // If the entity doesn't have an ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, member.getId())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(member))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Member in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithIdMismatchMember() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        member.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL_ID, longCount.incrementAndGet())
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(member))
            .exchange()
            .expectStatus()
            .isBadRequest();

        // Validate the Member in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void patchWithMissingIdPathParamMember() throws Exception {
        long databaseSizeBeforeUpdate = getRepositoryCount();
        member.setId(longCount.incrementAndGet());

        // If url ID doesn't match entity ID, it will throw BadRequestAlertException
        webTestClient
            .patch()
            .uri(ENTITY_API_URL)
            .contentType(MediaType.valueOf("application/merge-patch+json"))
            .bodyValue(om.writeValueAsBytes(member))
            .exchange()
            .expectStatus()
            .isEqualTo(405);

        // Validate the Member in the database
        assertSameRepositoryCount(databaseSizeBeforeUpdate);
    }

    @Test
    void deleteMember() {
        // Initialize the database
        insertedMember = memberRepository.save(member).block();

        long databaseSizeBeforeDelete = getRepositoryCount();

        // Delete the member
        webTestClient
            .delete()
            .uri(ENTITY_API_URL_ID, member.getId())
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isNoContent();

        // Validate the database contains one less item
        assertDecrementedRepositoryCount(databaseSizeBeforeDelete);
    }

    protected long getRepositoryCount() {
        return memberRepository.count().block();
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

    protected Member getPersistedMember(Member member) {
        return memberRepository.findById(member.getId()).block();
    }

    protected void assertPersistedMemberToMatchAllProperties(Member expectedMember) {
        // Test fails because reactive api returns an empty object instead of null
        // assertMemberAllPropertiesEquals(expectedMember, getPersistedMember(expectedMember));
        assertMemberUpdatableFieldsEquals(expectedMember, getPersistedMember(expectedMember));
    }

    protected void assertPersistedMemberToMatchUpdatableProperties(Member expectedMember) {
        // Test fails because reactive api returns an empty object instead of null
        // assertMemberAllUpdatablePropertiesEquals(expectedMember, getPersistedMember(expectedMember));
        assertMemberUpdatableFieldsEquals(expectedMember, getPersistedMember(expectedMember));
    }
}
