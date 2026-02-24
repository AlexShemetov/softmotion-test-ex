package com.example.testex.application.repository;

import com.example.testex.domain.model.OfferParam;
import java.util.List;
import java.util.Set;

public interface OfferParamsRepository {

    void replaceForOffers(Set<Long> offerIds, List<OfferParam> offerParams);
}
