package com.example.testex.application.repository;

import java.util.List;
import java.util.Map;

public interface CategoriesRepository {

    void upsert(List<Map<String, String>> rows);
}
