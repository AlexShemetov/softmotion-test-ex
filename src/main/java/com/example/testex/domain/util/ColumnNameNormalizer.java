package com.example.testex.domain.util;

import java.util.Locale;
import java.util.Map;

public class ColumnNameNormalizer {

    private static final int POSTGRES_MAX_IDENTIFIER = 63;

    private static final Map<String, String> KNOWN_COLUMNS = Map.of(
            "vendorCode", "vendor_code",
            "currencyId", "currency_id",
            "categoryId", "category_id",
            "parentId", "parent_id"
    );

    public String normalize(String sourceKey) {
        boolean isParam = sourceKey.startsWith("param:");
        String sourceHash = Integer.toUnsignedString(sourceKey.hashCode(), 16);
        String predefined = KNOWN_COLUMNS.get(sourceKey);
        if (predefined != null) {
            return predefined;
        }

        String raw = sourceKey;
        if (isParam) {
            raw = "param_" + sourceKey.substring("param:".length());
        }

        String camelConverted = raw.replaceAll("([a-z0-9])([A-Z])", "$1_$2");
        String lower = camelConverted.toLowerCase(Locale.ROOT);
        String cleaned = lower.replaceAll("[^a-z0-9]+", "_").replaceAll("^_+", "").replaceAll("_+$", "");
        if (cleaned.isEmpty()) {
            cleaned = "col_" + Integer.toHexString(sourceKey.hashCode());
        }
        if (Character.isDigit(cleaned.charAt(0))) {
            cleaned = "c_" + cleaned;
        }
        if (isParam) {
            cleaned = cleaned + "_" + sourceHash;
        }
        cleaned = fitIdentifier(cleaned, sourceHash);
        return cleaned;
    }

    private String fitIdentifier(String candidate, String sourceHash) {
        if (candidate.length() <= POSTGRES_MAX_IDENTIFIER) {
            return candidate;
        }

        int suffixLength = sourceHash.length() + 1;
        int prefixLength = Math.max(1, POSTGRES_MAX_IDENTIFIER - suffixLength);
        return candidate.substring(0, prefixLength) + "_" + sourceHash;
    }
}
