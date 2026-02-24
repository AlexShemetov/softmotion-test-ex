package com.example.testex.application.repository;

import com.example.testex.domain.model.OfferMeta;
import java.util.List;
import java.util.Set;

public interface OfferMetaRepository {

    void replaceForOffers(Set<Long> offerIds, List<OfferMeta> offerMetas);
}
