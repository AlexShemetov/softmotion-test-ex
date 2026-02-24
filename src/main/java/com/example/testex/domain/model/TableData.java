package com.example.testex.domain.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TableData {

    private final String tableName;
    private final String idColumn;
    private final LinkedHashMap<String, ColumnSpec> columns;
    private final List<Map<String, String>> rows;

    public TableData(
            String tableName,
            String idColumn,
            LinkedHashMap<String, ColumnSpec> columns,
            List<Map<String, String>> rows
    ) {
        this.tableName = tableName;
        this.idColumn = idColumn;
        this.columns = new LinkedHashMap<>(columns);
        this.rows = new ArrayList<>(rows);
    }

    public String tableName() {
        return tableName;
    }

    public String idColumn() {
        return idColumn;
    }

    public Map<String, ColumnSpec> columns() {
        return Collections.unmodifiableMap(columns);
    }

    public List<String> columnNames() {
        return new ArrayList<>(columns.keySet());
    }

    public List<Map<String, String>> rows() {
        return Collections.unmodifiableList(rows);
    }
}
