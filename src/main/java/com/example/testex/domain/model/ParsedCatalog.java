package com.example.testex.domain.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ParsedCatalog {

    private final LinkedHashMap<String, TableData> tables;
    private final List<OfferVendor> offerVendors;
    private final List<OfferMeta> offerMetas;
    private final List<OfferParam> offerParams;

    public ParsedCatalog(
            LinkedHashMap<String, TableData> tables,
            List<OfferVendor> offerVendors,
            List<OfferMeta> offerMetas,
            List<OfferParam> offerParams
    ) {
        this.tables = new LinkedHashMap<>(tables);
        this.offerVendors = List.copyOf(offerVendors);
        this.offerMetas = List.copyOf(offerMetas);
        this.offerParams = List.copyOf(offerParams);
    }

    public Set<String> tableNames() {
        return Collections.unmodifiableSet(tables.keySet());
    }

    public TableData table(String tableName) {
        return tables.get(tableName);
    }

    public Map<String, TableData> tables() {
        return Collections.unmodifiableMap(tables);
    }

    public List<OfferVendor> offerVendors() {
        return offerVendors;
    }

    public List<OfferMeta> offerMetas() {
        return offerMetas;
    }

    public List<OfferParam> offerParams() {
        return offerParams;
    }
}
