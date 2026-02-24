package com.example.testex.domain.service;

import java.util.ArrayList;

public interface XmlCatalogService {

    ArrayList<String> getTableNames();

    String getTableDDL(String tableName);

    void update();

    void update(String tableName);

    ArrayList<String> getColumnNames(String tableName);

    boolean isColumnId(String tableName, String columnName);

    String getDDLChange(String tableName);
}
