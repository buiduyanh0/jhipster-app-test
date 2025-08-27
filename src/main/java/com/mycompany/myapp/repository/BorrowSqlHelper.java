package com.mycompany.myapp.repository;

import java.util.ArrayList;
import java.util.List;
import org.springframework.data.relational.core.sql.Column;
import org.springframework.data.relational.core.sql.Expression;
import org.springframework.data.relational.core.sql.Table;

public class BorrowSqlHelper {

    public static List<Expression> getColumns(Table table, String columnPrefix) {
        List<Expression> columns = new ArrayList<>();
        columns.add(Column.aliased("id", table, columnPrefix + "_id"));
        columns.add(Column.aliased("borrow_date", table, columnPrefix + "_borrow_date"));
        columns.add(Column.aliased("return_date", table, columnPrefix + "_return_date"));

        columns.add(Column.aliased("member_id", table, columnPrefix + "_member_id"));
        columns.add(Column.aliased("book_id", table, columnPrefix + "_book_id"));
        return columns;
    }
}
