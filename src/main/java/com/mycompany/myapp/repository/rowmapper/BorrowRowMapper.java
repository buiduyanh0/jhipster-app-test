package com.mycompany.myapp.repository.rowmapper;

import com.mycompany.myapp.domain.Borrow;
import io.r2dbc.spi.Row;
import java.time.Instant;
import java.util.function.BiFunction;
import org.springframework.stereotype.Service;

/**
 * Converter between {@link Row} to {@link Borrow}, with proper type conversions.
 */
@Service
public class BorrowRowMapper implements BiFunction<Row, String, Borrow> {

    private final ColumnConverter converter;

    public BorrowRowMapper(ColumnConverter converter) {
        this.converter = converter;
    }

    /**
     * Take a {@link Row} and a column prefix, and extract all the fields.
     * @return the {@link Borrow} stored in the database.
     */
    @Override
    public Borrow apply(Row row, String prefix) {
        Borrow entity = new Borrow();
        entity.setId(converter.fromRow(row, prefix + "_id", Long.class));
        entity.setBorrowDate(converter.fromRow(row, prefix + "_borrow_date", Instant.class));
        entity.setReturnDate(converter.fromRow(row, prefix + "_return_date", Instant.class));
        entity.setMemberId(converter.fromRow(row, prefix + "_member_id", Long.class));
        entity.setBookId(converter.fromRow(row, prefix + "_book_id", Long.class));
        return entity;
    }
}
