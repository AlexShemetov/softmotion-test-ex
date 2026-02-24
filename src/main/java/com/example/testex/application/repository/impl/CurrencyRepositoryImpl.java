package com.example.testex.application.repository.impl;

import com.example.testex.application.repository.CurrencyRepository;
import com.example.testex.jooq.Tables;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jooq.DSLContext;
import org.jooq.Query;
import org.springframework.stereotype.Repository;

@Repository
public class CurrencyRepositoryImpl extends BaseJooqRepository implements CurrencyRepository {

    public CurrencyRepositoryImpl(DSLContext dslContext) {
        super(dslContext);
    }

    @Override
    public void upsert(List<Map<String, String>> rows) {
        List<Query> queries = new ArrayList<>();
        for (Map<String, String> row : rows) {
            String id = blankToNull(row.get("id"));
            if (id == null) {
                continue;
            }
            Long rate = parseLongSafe(row.get("rate"));
            queries.add(dslContext.insertInto(Tables.CURRENCY)
                    .set(Tables.CURRENCY.ID, id)
                    .set(Tables.CURRENCY.RATE, rate)
                    .onConflict(Tables.CURRENCY.ID)
                    .doUpdate()
                    .set(Tables.CURRENCY.RATE, rate));
        }
        executeBatch(queries);
    }
}
