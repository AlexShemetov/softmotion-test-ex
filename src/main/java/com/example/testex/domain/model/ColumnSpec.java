package com.example.testex.domain.model;

public record ColumnSpec(
        String sourceKey,
        String columnName,
        ColumnType columnType
) {
}
