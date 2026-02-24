package com.example.testex.application.repository.impl;

import com.example.testex.application.repository.CategoriesRepository;
import com.example.testex.jooq.Tables;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jooq.DSLContext;
import org.jooq.Query;
import org.springframework.stereotype.Repository;

@Repository
public class CategoriesRepositoryImpl extends BaseJooqRepository implements CategoriesRepository {

    public CategoriesRepositoryImpl(DSLContext dslContext) {
        super(dslContext);
    }

    @Override
    public void upsert(List<Map<String, String>> rows) {
        List<Query> queries = new ArrayList<>();
        for (Map<String, String> row : rows) {
            Long id = parseLongSafe(row.get("id"));
            if (id == null) {
                continue;
            }
            Long parentId = parseLongSafe(row.get("parent_id"));
            String name = blankToNull(row.get("name"));

            queries.add(dslContext.insertInto(Tables.CATEGORIES)
                    .set(Tables.CATEGORIES.ID, id)
                    .set(Tables.CATEGORIES.PARENT_ID, parentId)
                    .set(Tables.CATEGORIES.NAME, name)
                    .onConflict(Tables.CATEGORIES.ID)
                    .doUpdate()
                    .set(Tables.CATEGORIES.PARENT_ID, parentId)
                    .set(Tables.CATEGORIES.NAME, name));
        }
        executeBatch(queries);
    }
}
