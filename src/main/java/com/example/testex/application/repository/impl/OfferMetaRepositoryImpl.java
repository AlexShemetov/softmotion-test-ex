package com.example.testex.application.repository.impl;

import com.example.testex.application.repository.OfferMetaRepository;
import com.example.testex.domain.model.OfferMeta;
import com.example.testex.jooq.Tables;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.jooq.DSLContext;
import org.jooq.Query;
import org.springframework.stereotype.Repository;

@Repository
public class OfferMetaRepositoryImpl extends BaseJooqRepository implements OfferMetaRepository {

    public OfferMetaRepositoryImpl(DSLContext dslContext) {
        super(dslContext);
    }

    @Override
    public void replaceForOffers(Set<Long> offerIds, List<OfferMeta> offerMetas) {
        if (offerIds.isEmpty()) {
            return;
        }

        dslContext.deleteFrom(Tables.OFFER_META)
                .where(Tables.OFFER_META.OFFER_ID.in(offerIds))
                .execute();

        List<Query> queries = new ArrayList<>();
        for (OfferMeta offerMeta : offerMetas) {
            Long offerId = parseLongSafe(offerMeta.offerId());
            if (offerId == null || !offerIds.contains(offerId)) {
                continue;
            }
            queries.add(dslContext.insertInto(Tables.OFFER_META)
                    .set(Tables.OFFER_META.OFFER_ID, offerId)
                    .set(Tables.OFFER_META.URL, blankToNull(offerMeta.url()))
                    .set(Tables.OFFER_META.PICTURE, blankToNull(offerMeta.picture()))
                    .set(Tables.OFFER_META.NAME, blankToNull(offerMeta.name()))
                    .set(Tables.OFFER_META.DESCRIPTION, blankToNull(offerMeta.description())));
        }

        executeBatch(queries);
    }
}
