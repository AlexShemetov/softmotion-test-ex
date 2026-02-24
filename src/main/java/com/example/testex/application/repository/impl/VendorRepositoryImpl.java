package com.example.testex.application.repository.impl;

import com.example.testex.application.repository.VendorRepository;
import com.example.testex.domain.model.OfferVendor;
import com.example.testex.jooq.Tables;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jooq.DSLContext;
import org.jooq.Query;
import org.springframework.stereotype.Repository;

@Repository
public class VendorRepositoryImpl extends BaseJooqRepository implements VendorRepository {

    public VendorRepositoryImpl(DSLContext dslContext) {
        super(dslContext);
    }

    @Override
    public void upsert(List<OfferVendor> offerVendors) {
        LinkedHashMap<String, String> uniqueVendorByCode = new LinkedHashMap<>();
        for (OfferVendor offerVendor : offerVendors) {
            String code = blankToNull(offerVendor.vendorCode());
            if (code == null) {
                continue;
            }
            uniqueVendorByCode.putIfAbsent(code, blankToNull(offerVendor.vendor()));
        }

        List<Query> queries = new ArrayList<>();
        for (Map.Entry<String, String> entry : uniqueVendorByCode.entrySet()) {
            queries.add(dslContext.insertInto(Tables.VENDOR)
                    .set(Tables.VENDOR.CODE, entry.getKey())
                    .set(Tables.VENDOR.NAME, entry.getValue())
                    .onConflict(Tables.VENDOR.CODE)
                    .doUpdate()
                    .set(Tables.VENDOR.NAME, entry.getValue()));
        }

        executeBatch(queries);
    }
}
