package com.example.testex.application.repository.impl;

import com.example.testex.application.repository.OfferParamsRepository;
import com.example.testex.domain.model.OfferParam;
import com.example.testex.jooq.Tables;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.jooq.DSLContext;
import org.jooq.Query;
import org.springframework.stereotype.Repository;

@Repository
public class OfferParamsRepositoryImpl extends BaseJooqRepository implements OfferParamsRepository {

    public OfferParamsRepositoryImpl(DSLContext dslContext) {
        super(dslContext);
    }

    @Override
    public void replaceForOffers(Set<Long> offerIds, List<OfferParam> offerParams) {
        if (offerIds.isEmpty()) {
            return;
        }

        dslContext.deleteFrom(Tables.OFFER_PARAMS)
                .where(Tables.OFFER_PARAMS.OFFER_ID.in(offerIds))
                .execute();

        List<Query> queries = new ArrayList<>();
        for (OfferParam offerParam : offerParams) {
            Long offerId = parseLongSafe(offerParam.offerId());
            if (offerId == null || !offerIds.contains(offerId)) {
                continue;
            }
            String paramName = blankToNull(offerParam.paramName());
            if (paramName == null) {
                continue;
            }
            queries.add(dslContext.insertInto(Tables.OFFER_PARAMS)
                    .set(Tables.OFFER_PARAMS.OFFER_ID, offerId)
                    .set(Tables.OFFER_PARAMS.PARAM_NAME, paramName)
                    .set(Tables.OFFER_PARAMS.PARAM_VALUE, blankToNull(offerParam.paramValue())));
        }

        executeBatch(queries);
    }
}
