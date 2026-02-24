package com.example.testex.application.repository;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface OffersRepository {

    Set<Long> upsert(List<Map<String, String>> rows);
}
