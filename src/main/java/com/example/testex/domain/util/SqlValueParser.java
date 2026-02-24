package com.example.testex.domain.util;

import com.example.testex.domain.model.ColumnType;
import java.math.BigDecimal;

public class SqlValueParser {

    public Object parse(String value, ColumnType columnType) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        return switch (columnType) {
            case BOOLEAN -> parseBoolean(trimmed);
            case BIGINT -> parseLong(trimmed);
            case NUMERIC -> parseDecimal(trimmed);
            case TEXT -> trimmed;
        };
    }

    private Object parseBoolean(String value) {
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return Boolean.parseBoolean(value);
        }
        return null;
    }

    private Object parseLong(String value) {
        String normalized = value.replace(" ", "").replace(",", "");
        if (!normalized.matches("-?\\d+")) {
            return null;
        }
        try {
            return Long.parseLong(normalized);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private Object parseDecimal(String value) {
        String normalized = value.replace(" ", "").replace(",", ".");
        if (!normalized.matches("-?\\d+(\\.\\d+)?")) {
            return null;
        }
        try {
            return new BigDecimal(normalized);
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
