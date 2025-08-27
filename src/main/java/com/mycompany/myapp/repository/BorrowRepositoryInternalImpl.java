package com.mycompany.myapp.repository;

import com.mycompany.myapp.domain.Borrow;
import com.mycompany.myapp.repository.rowmapper.BookRowMapper;
import com.mycompany.myapp.repository.rowmapper.BorrowRowMapper;
import com.mycompany.myapp.repository.rowmapper.MemberRowMapper;
import io.r2dbc.spi.Row;
import io.r2dbc.spi.RowMetadata;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.convert.R2dbcConverter;
import org.springframework.data.r2dbc.core.R2dbcEntityOperations;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.repository.support.SimpleR2dbcRepository;
import org.springframework.data.relational.core.sql.Column;
import org.springframework.data.relational.core.sql.Comparison;
import org.springframework.data.relational.core.sql.Condition;
import org.springframework.data.relational.core.sql.Conditions;
import org.springframework.data.relational.core.sql.Expression;
import org.springframework.data.relational.core.sql.Select;
import org.springframework.data.relational.core.sql.SelectBuilder.SelectFromAndJoinCondition;
import org.springframework.data.relational.core.sql.Table;
import org.springframework.data.relational.repository.support.MappingRelationalEntityInformation;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.r2dbc.core.RowsFetchSpec;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Spring Data R2DBC custom repository implementation for the Borrow entity.
 */
@SuppressWarnings("unused")
class BorrowRepositoryInternalImpl extends SimpleR2dbcRepository<Borrow, Long> implements BorrowRepositoryInternal {

    private final DatabaseClient db;
    private final R2dbcEntityTemplate r2dbcEntityTemplate;
    private final EntityManager entityManager;

    private final MemberRowMapper memberMapper;
    private final BookRowMapper bookMapper;
    private final BorrowRowMapper borrowMapper;

    private static final Table entityTable = Table.aliased("borrow", EntityManager.ENTITY_ALIAS);
    private static final Table memberTable = Table.aliased("member", "e_member");
    private static final Table bookTable = Table.aliased("book", "book");

    public BorrowRepositoryInternalImpl(
        R2dbcEntityTemplate template,
        EntityManager entityManager,
        MemberRowMapper memberMapper,
        BookRowMapper bookMapper,
        BorrowRowMapper borrowMapper,
        R2dbcEntityOperations entityOperations,
        R2dbcConverter converter
    ) {
        super(
            new MappingRelationalEntityInformation(converter.getMappingContext().getRequiredPersistentEntity(Borrow.class)),
            entityOperations,
            converter
        );
        this.db = template.getDatabaseClient();
        this.r2dbcEntityTemplate = template;
        this.entityManager = entityManager;
        this.memberMapper = memberMapper;
        this.bookMapper = bookMapper;
        this.borrowMapper = borrowMapper;
    }

    @Override
    public Flux<Borrow> findAllBy(Pageable pageable) {
        return createQuery(pageable, null).all();
    }

    RowsFetchSpec<Borrow> createQuery(Pageable pageable, Condition whereClause) {
        List<Expression> columns = BorrowSqlHelper.getColumns(entityTable, EntityManager.ENTITY_ALIAS);
        columns.addAll(MemberSqlHelper.getColumns(memberTable, "member"));
        columns.addAll(BookSqlHelper.getColumns(bookTable, "book"));
        SelectFromAndJoinCondition selectFrom = Select.builder()
            .select(columns)
            .from(entityTable)
            .leftOuterJoin(memberTable)
            .on(Column.create("member_id", entityTable))
            .equals(Column.create("id", memberTable))
            .leftOuterJoin(bookTable)
            .on(Column.create("book_id", entityTable))
            .equals(Column.create("id", bookTable));
        // we do not support Criteria here for now as of https://github.com/jhipster/generator-jhipster/issues/18269
        String select = entityManager.createSelect(selectFrom, Borrow.class, pageable, whereClause);
        return db.sql(select).map(this::process);
    }

    @Override
    public Flux<Borrow> findAll() {
        return findAllBy(null);
    }

    @Override
    public Mono<Borrow> findById(Long id) {
        Comparison whereClause = Conditions.isEqual(entityTable.column("id"), Conditions.just(id.toString()));
        return createQuery(null, whereClause).one();
    }

    private Borrow process(Row row, RowMetadata metadata) {
        Borrow entity = borrowMapper.apply(row, "e");
        entity.setMember(memberMapper.apply(row, "member"));
        entity.setBook(bookMapper.apply(row, "book"));
        return entity;
    }

    @Override
    public <S extends Borrow> Mono<S> save(S entity) {
        return super.save(entity);
    }
}
