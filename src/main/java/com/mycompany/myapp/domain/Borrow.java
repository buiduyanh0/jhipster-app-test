package com.mycompany.myapp.domain;

import jakarta.validation.constraints.*;
import java.io.Serializable;
import java.time.Instant;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

/**
 * A Borrow.
 */
@Table("borrow")
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Borrow implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @Column("id")
    private Long id;

    @NotNull(message = "must not be null")
    @Column("borrow_date")
    private Instant borrowDate;

    @Column("return_date")
    private Instant returnDate;

    @org.springframework.data.annotation.Transient
    private Member member;

    @org.springframework.data.annotation.Transient
    private Book book;

    @Column("member_id")
    private Long memberId;

    @Column("book_id")
    private Long bookId;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Borrow id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Instant getBorrowDate() {
        return this.borrowDate;
    }

    public Borrow borrowDate(Instant borrowDate) {
        this.setBorrowDate(borrowDate);
        return this;
    }

    public void setBorrowDate(Instant borrowDate) {
        this.borrowDate = borrowDate;
    }

    public Instant getReturnDate() {
        return this.returnDate;
    }

    public Borrow returnDate(Instant returnDate) {
        this.setReturnDate(returnDate);
        return this;
    }

    public void setReturnDate(Instant returnDate) {
        this.returnDate = returnDate;
    }

    public Member getMember() {
        return this.member;
    }

    public void setMember(Member member) {
        this.member = member;
        this.memberId = member != null ? member.getId() : null;
    }

    public Borrow member(Member member) {
        this.setMember(member);
        return this;
    }

    public Book getBook() {
        return this.book;
    }

    public void setBook(Book book) {
        this.book = book;
        this.bookId = book != null ? book.getId() : null;
    }

    public Borrow book(Book book) {
        this.setBook(book);
        return this;
    }

    public Long getMemberId() {
        return this.memberId;
    }

    public void setMemberId(Long member) {
        this.memberId = member;
    }

    public Long getBookId() {
        return this.bookId;
    }

    public void setBookId(Long book) {
        this.bookId = book;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Borrow)) {
            return false;
        }
        return getId() != null && getId().equals(((Borrow) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Borrow{" +
            "id=" + getId() +
            ", borrowDate='" + getBorrowDate() + "'" +
            ", returnDate='" + getReturnDate() + "'" +
            "}";
    }
}
