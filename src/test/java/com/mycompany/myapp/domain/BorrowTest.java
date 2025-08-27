package com.mycompany.myapp.domain;

import static com.mycompany.myapp.domain.BookTestSamples.*;
import static com.mycompany.myapp.domain.BorrowTestSamples.*;
import static com.mycompany.myapp.domain.MemberTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.mycompany.myapp.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class BorrowTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Borrow.class);
        Borrow borrow1 = getBorrowSample1();
        Borrow borrow2 = new Borrow();
        assertThat(borrow1).isNotEqualTo(borrow2);

        borrow2.setId(borrow1.getId());
        assertThat(borrow1).isEqualTo(borrow2);

        borrow2 = getBorrowSample2();
        assertThat(borrow1).isNotEqualTo(borrow2);
    }

    @Test
    void memberTest() {
        Borrow borrow = getBorrowRandomSampleGenerator();
        Member memberBack = getMemberRandomSampleGenerator();

        borrow.setMember(memberBack);
        assertThat(borrow.getMember()).isEqualTo(memberBack);

        borrow.member(null);
        assertThat(borrow.getMember()).isNull();
    }

    @Test
    void bookTest() {
        Borrow borrow = getBorrowRandomSampleGenerator();
        Book bookBack = getBookRandomSampleGenerator();

        borrow.setBook(bookBack);
        assertThat(borrow.getBook()).isEqualTo(bookBack);

        borrow.book(null);
        assertThat(borrow.getBook()).isNull();
    }
}
