package com.example.testex.application.repository;

import com.example.testex.domain.model.OfferVendor;
import java.util.List;

public interface VendorRepository {

    void upsert(List<OfferVendor> offerVendors);
}
