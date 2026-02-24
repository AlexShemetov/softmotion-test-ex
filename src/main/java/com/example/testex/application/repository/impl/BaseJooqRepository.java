package com.example.testex.application.repository.impl;

import com.example.testex.domain.model.ColumnType;
import com.example.testex.domain.util.SqlValueParser;
import java.util.List;
import org.jooq.DSLContext;
import org.jooq.Query;

public abstract class BaseJooqRepository {

    private final SqlValueParser sqlValueParser;

    protected final DSLContext dslContext;

    protected BaseJooqRepository(DSLContext dslContext) {
        this.dslContext = dslContext;
        this.sqlValueParser = new SqlValueParser();
    }

    protected Long parseLongSafe(String value) {
        Object parsed = sqlValueParser.parse(value, ColumnType.BIGINT);
        if (parsed instanceof Long longValue) {
            return longValue;
        }
        return null;
    }

    protected Boolean parseBooleanSafe(String value) {
        Object parsed = sqlValueParser.parse(value, ColumnType.BOOLEAN);
        if (parsed instanceof Boolean boolValue) {
            return boolValue;
        }
        return null;
    }

    protected String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed;
    }

    protected void executeBatch(List<Query> queries) {
        if (queries.isEmpty()) {
            return;
        }
        dslContext.batch(queries).execute();
    }
}
