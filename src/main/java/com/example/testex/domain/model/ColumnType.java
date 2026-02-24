package com.example.testex.domain.model;

public enum ColumnType {
    TEXT("TEXT"),
    BIGINT("BIGINT"),
    NUMERIC("NUMERIC(19,4)"),
    BOOLEAN("BOOLEAN");

    private final String ddlType;

    ColumnType(String ddlType) {
        this.ddlType = ddlType;
    }

    public String ddlType() {
        return ddlType;
    }
}
