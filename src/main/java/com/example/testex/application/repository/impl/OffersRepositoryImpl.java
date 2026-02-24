package com.example.testex.application.repository.impl;

import com.example.testex.application.repository.OffersRepository;
import com.example.testex.jooq.Tables;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jooq.DSLContext;
import org.jooq.Query;
import org.springframework.stereotype.Repository;

@Repository
public class OffersRepositoryImpl extends BaseJooqRepository implements OffersRepository {

    public OffersRepositoryImpl(DSLContext dslContext) {
        super(dslContext);
    }

    @Override
    public Set<Long> upsert(List<Map<String, String>> rows) {
        Set<Long> offerIds = new LinkedHashSet<>();
        List<Query> queries = new ArrayList<>();

        for (Map<String, String> row : rows) {
            Long id = parseLongSafe(row.get("id"));
            if (id == null) {
                continue;
            }
            offerIds.add(id);

            Long categoryId = parseLongSafe(row.get("category_id"));
            String currencyId = blankToNull(row.get("currency_id"));
            String vendorCode = blankToNull(row.get("vendor_code"));
            Boolean available = parseBooleanSafe(row.get("available"));
            Long price = parseLongSafe(row.get("price"));
            Long count = parseLongSafe(row.get("count"));

            queries.add(dslContext.insertInto(Tables.OFFERS)
                    .set(Tables.OFFERS.ID, id)
                    .set(Tables.OFFERS.CATEGORY_ID, categoryId)
                    .set(Tables.OFFERS.CURRENCY_ID, currencyId)
                    .set(Tables.OFFERS.VENDOR_CODE, vendorCode)
                    .set(Tables.OFFERS.AVAILABLE, available)
                    .set(Tables.OFFERS.PRICE, price)
                    .set(Tables.OFFERS.COUNT, count)
                    .onConflict(Tables.OFFERS.ID)
                    .doUpdate()
                    .set(Tables.OFFERS.CATEGORY_ID, categoryId)
                    .set(Tables.OFFERS.CURRENCY_ID, currencyId)
                    .set(Tables.OFFERS.VENDOR_CODE, vendorCode)
                    .set(Tables.OFFERS.AVAILABLE, available)
                    .set(Tables.OFFERS.PRICE, price)
                    .set(Tables.OFFERS.COUNT, count));
        }

        executeBatch(queries);
        return offerIds;
    }
}
