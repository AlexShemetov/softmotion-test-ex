package com.example.testex.application.service;

import com.example.testex.application.parser.CatalogXmlParser;
import com.example.testex.application.repository.CategoriesRepository;
import com.example.testex.application.repository.CurrencyRepository;
import com.example.testex.application.repository.OfferMetaRepository;
import com.example.testex.application.repository.OfferParamsRepository;
import com.example.testex.application.repository.OffersRepository;
import com.example.testex.application.repository.VendorRepository;
import com.example.testex.domain.exception.SchemaChangeNotAllowedException;
import com.example.testex.domain.model.ColumnSpec;
import com.example.testex.domain.model.ColumnType;
import com.example.testex.domain.model.ParsedCatalog;
import com.example.testex.domain.model.TableData;
import com.example.testex.domain.service.XmlCatalogService;
import com.example.testex.domain.util.ColumnNameNormalizer;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.Record2;
import org.jooq.Result;
import org.jooq.impl.DSL;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class XmlCatalogServiceImpl implements XmlCatalogService {

    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    private final CatalogXmlParser catalogXmlParser;
    private final DSLContext dslContext;
    private final String schemaName;
    private final CurrencyRepository currencyRepository;
    private final CategoriesRepository categoriesRepository;
    private final VendorRepository vendorRepository;
    private final OffersRepository offersRepository;
    private final OfferMetaRepository offerMetaRepository;
    private final OfferParamsRepository offerParamsRepository;
    private final ColumnNameNormalizer columnNameNormalizer;

    private volatile ParsedCatalog cachedCatalog;
    private volatile Instant loadedAt = Instant.EPOCH;

    public XmlCatalogServiceImpl(
            CatalogXmlParser catalogXmlParser,
            DSLContext dslContext,
            @Value("${app.schema}") String schemaName,
            CurrencyRepository currencyRepository,
            CategoriesRepository categoriesRepository,
            VendorRepository vendorRepository,
            OffersRepository offersRepository,
            OfferMetaRepository offerMetaRepository,
            OfferParamsRepository offerParamsRepository
    ) {
        this.catalogXmlParser = catalogXmlParser;
        this.dslContext = dslContext;
        this.schemaName = schemaName;
        this.currencyRepository = currencyRepository;
        this.categoriesRepository = categoriesRepository;
        this.vendorRepository = vendorRepository;
        this.offersRepository = offersRepository;
        this.offerMetaRepository = offerMetaRepository;
        this.offerParamsRepository = offerParamsRepository;
        this.columnNameNormalizer = new ColumnNameNormalizer();
    }

    @Override
    public ArrayList<String> getTableNames() {
        return new ArrayList<>(loadCatalog().tableNames());
    }

    @Override
    public String getTableDDL(String tableName) {
        TableData tableData = getTableData(tableName);

        List<String> elements = new ArrayList<>();
        for (String columnName : tableData.columnNames()) {
            ColumnSpec columnSpec = tableData.columns().get(columnName);
            elements.add(quote(columnName) + " " + columnSpec.columnType().ddlType());
        }

        if (tableData.idColumn() != null && tableData.columns().containsKey(tableData.idColumn())) {
            elements.add("CONSTRAINT " + quote(tableData.tableName() + "_pkey")
                    + " PRIMARY KEY (" + quote(tableData.idColumn()) + ")");
        }

        String joined = String.join(",\n    ", elements);
        return "CREATE TABLE IF NOT EXISTS " + qualifiedTable(tableData.tableName()) + " (\n    "
                + joined + "\n);";
    }

    @Override
    @Transactional
    public void update() {
        ParsedCatalog parsedCatalog = loadCatalog();
        for (String tableName : parsedCatalog.tableNames()) {
            updateSingleTable(parsedCatalog, tableName);
        }
    }

    @Override
    @Transactional
    public void update(String tableName) {
        ParsedCatalog parsedCatalog = loadCatalog();
        updateSingleTable(parsedCatalog, tableName);
    }

    @Override
    public ArrayList<String> getColumnNames(String tableName) {
        return new ArrayList<>(getTableData(tableName).columnNames());
    }

    @Override
    public boolean isColumnId(String tableName, String columnName) {
        TableData tableData = getTableData(tableName);
        String resolvedColumn = resolveColumnName(tableData, columnName);
        if (resolvedColumn == null) {
            return false;
        }

        Set<String> seen = new HashSet<>();
        for (Map<String, String> row : tableData.rows()) {
            String value = row.get(resolvedColumn);
            if (value == null || value.isBlank()) {
                return false;
            }
            if (!seen.add(value)) {
                return false;
            }
        }
        return !tableData.rows().isEmpty();
    }

    @Override
    public String getDDLChange(String tableName) {
        TableData tableData = getTableData(tableName);
        if (tableExists(tableData.tableName())) {
            return getTableDDL(tableData.tableName());
        }

        Map<String, String> dbColumns = loadDbColumns(tableData.tableName());
        for (String dbColumn : dbColumns.keySet()) {
            if (!tableData.columns().containsKey(dbColumn)) {
                throw new SchemaChangeNotAllowedException(
                        "Table structure changed: column removed in XML: " + dbColumn
                );
            }
        }

        List<String> alters = new ArrayList<>();
        for (String expectedColumnName : tableData.columnNames()) {
            ColumnType expectedType = tableData.columns().get(expectedColumnName).columnType();
            String dbType = dbColumns.get(expectedColumnName);
            if (dbType == null) {
                alters.add("ALTER TABLE " + qualifiedTable(tableData.tableName())
                        + " ADD COLUMN " + quote(expectedColumnName) + " " + expectedType.ddlType() + ";");
                continue;
            }
            if (!isCompatible(expectedType, dbType)) {
                throw new SchemaChangeNotAllowedException(
                        "Column type changed for " + expectedColumnName + ": db=" + dbType
                                + ", xml=" + expectedType.ddlType()
                );
            }
        }
        return String.join("\n", alters);
    }

    private void updateSingleTable(ParsedCatalog parsedCatalog, String tableName) {
        TableData tableData = getTableData(parsedCatalog, tableName);

        if (tableExists(tableData.tableName())) {
            throw new IllegalStateException("Table " + tableData.tableName()
                    + " is missing. Run Flyway migrations first.");
        }

        String ddlChange = getDDLChange(tableData.tableName());
        if (ddlChange != null && !ddlChange.isBlank()) {
            throw new SchemaChangeNotAllowedException(
                    "Schema differs from XML. Create Flyway migration first:\n" + ddlChange
            );
        }

        switch (tableData.tableName()) {
            case "offers" -> updateOffersWithRelatedTables(parsedCatalog, tableData);
            case "currency" -> currencyRepository.upsert(tableData.rows());
            case "categories" -> categoriesRepository.upsert(tableData.rows());
            default -> throw new IllegalArgumentException("Unsupported table update: " + tableData.tableName());
        }
    }

    private void updateOffersWithRelatedTables(ParsedCatalog parsedCatalog, TableData offersTable) {
        TableData currencyTable = getTableData(parsedCatalog, "currency");
        TableData categoriesTable = getTableData(parsedCatalog, "categories");

        currencyRepository.upsert(currencyTable.rows());
        categoriesRepository.upsert(categoriesTable.rows());
        vendorRepository.upsert(parsedCatalog.offerVendors());

        Set<Long> offerIds = offersRepository.upsert(offersTable.rows());
        offerMetaRepository.replaceForOffers(offerIds, parsedCatalog.offerMetas());
        offerParamsRepository.replaceForOffers(offerIds, parsedCatalog.offerParams());
    }

    private ParsedCatalog loadCatalog() {
        Instant now = Instant.now();
        ParsedCatalog localCache = cachedCatalog;
        if (localCache != null && loadedAt.plus(CACHE_TTL).isAfter(now)) {
            return localCache;
        }

        synchronized (this) {
            if (cachedCatalog != null && loadedAt.plus(CACHE_TTL).isAfter(Instant.now())) {
                return cachedCatalog;
            }
            cachedCatalog = catalogXmlParser.parse();
            loadedAt = Instant.now();
            return cachedCatalog;
        }
    }

    private TableData getTableData(String tableName) {
        return getTableData(loadCatalog(), tableName);
    }

    private TableData getTableData(ParsedCatalog parsedCatalog, String tableName) {
        String normalizedTableName = normalizeTableName(tableName);
        TableData tableData = parsedCatalog.table(normalizedTableName);
        if (tableData == null) {
            throw new IllegalArgumentException("Unknown table: " + tableName);
        }
        return tableData;
    }

    private String normalizeTableName(String tableName) {
        String value = tableName.toLowerCase(Locale.ROOT).trim();
        return switch (value) {
            case "currencies", "currency" -> "currency";
            case "category", "categories" -> "categories";
            case "offer", "offers" -> "offers";
            default -> value;
        };
    }

    private boolean tableExists(String tableName) {
        Field<Integer> countField = DSL.count().as("cnt");
        Integer count = dslContext.select(countField)
                .from(DSL.table(DSL.name("information_schema", "tables")))
                .where(DSL.field(DSL.name("table_schema"), String.class).eq(schemaName))
                .and(DSL.field(DSL.name("table_name"), String.class).eq(tableName))
                .fetchOne(countField);
        return count == null || count <= 0;
    }

    private Map<String, String> loadDbColumns(String tableName) {
        Field<String> columnNameField = DSL.field(DSL.name("column_name"), String.class);
        Field<String> dataTypeField = DSL.field(DSL.name("data_type"), String.class);
        Result<Record2<String, String>> result = dslContext.select(columnNameField, dataTypeField)
                .from(DSL.table(DSL.name("information_schema", "columns")))
                .where(DSL.field(DSL.name("table_schema"), String.class).eq(schemaName))
                .and(DSL.field(DSL.name("table_name"), String.class).eq(tableName))
                .orderBy(DSL.field(DSL.name("ordinal_position"), Integer.class))
                .fetch();

        LinkedHashMap<String, String> columns = new LinkedHashMap<>();
        for (Record2<String, String> row : result) {
            columns.put(row.value1(), row.value2());
        }
        return columns;
    }

    private boolean isCompatible(ColumnType expectedType, String dbDataType) {
        String normalizedDbType = dbDataType.toLowerCase(Locale.ROOT);
        return switch (expectedType) {
            case TEXT -> normalizedDbType.equals("text")
                    || normalizedDbType.equals("character varying")
                    || normalizedDbType.equals("character");
            case BIGINT -> normalizedDbType.equals("bigint")
                    || normalizedDbType.equals("integer")
                    || normalizedDbType.equals("smallint");
            case NUMERIC -> normalizedDbType.equals("numeric")
                    || normalizedDbType.equals("decimal")
                    || normalizedDbType.equals("double precision")
                    || normalizedDbType.equals("real")
                    || normalizedDbType.equals("bigint")
                    || normalizedDbType.equals("integer");
            case BOOLEAN -> normalizedDbType.equals("boolean");
        };
    }

    private String resolveColumnName(TableData tableData, String inputColumnName) {
        if (tableData.columns().containsKey(inputColumnName)) {
            return inputColumnName;
        }

        String normalized = columnNameNormalizer.normalize(inputColumnName);
        if (tableData.columns().containsKey(normalized)) {
            return normalized;
        }

        for (ColumnSpec columnSpec : tableData.columns().values()) {
            if (columnSpec.sourceKey().equals(inputColumnName)) {
                return columnSpec.columnName();
            }
        }
        return null;
    }

    private String quote(String identifier) {
        return dslContext.render(DSL.name(identifier));
    }

    private String qualifiedTable(String tableName) {
        return dslContext.render(DSL.name(schemaName, tableName));
    }
}
