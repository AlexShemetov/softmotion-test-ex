package com.example.testex.standalone;

import com.example.testex.domain.service.XmlCatalogService;
import java.util.ArrayList;
import java.util.Arrays;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class ConsoleRunner implements CommandLineRunner {

    private final XmlCatalogService xmlCatalogService;

    public ConsoleRunner(XmlCatalogService xmlCatalogService) {
        this.xmlCatalogService = xmlCatalogService;
    }

    @Override
    public void run(String... args) {
        if (args.length == 0) {
            runDemo();
            return;
        }

        String command = args[0].trim().toLowerCase();
        switch (command) {
            case "list" -> printTableNames();
            case "ddl" -> printDdl(args);
            case "columns" -> printColumns(args);
            case "is-id" -> printIsId(args);
            case "ddl-change" -> printDdlChange(args);
            case "update" -> runUpdate(args);
            case "help" -> printHelp();
            default -> {
                System.out.println("Unknown command: " + args[0]);
                printHelp();
            }
        }
    }

    private void runDemo() {
        System.out.println("Demo mode: all functions");
        ArrayList<String> tableNames = xmlCatalogService.getTableNames();
        System.out.println("Tables: " + tableNames);
        for (String tableName : tableNames) {
            System.out.println("----- " + tableName + " -----");
            System.out.println("DDL:");
            System.out.println(xmlCatalogService.getTableDDL(tableName));
            ArrayList<String> columns = xmlCatalogService.getColumnNames(tableName);
            System.out.println("Columns: " + columns);
            System.out.println("isColumnId(" + tableName + ", id): " + xmlCatalogService.isColumnId(tableName, "id"));
            System.out.println("DDL change:");
            System.out.println(xmlCatalogService.getDDLChange(tableName));
        }
        xmlCatalogService.update();
        System.out.println("Update done for all tables.");
    }

    private void printTableNames() {
        System.out.println(xmlCatalogService.getTableNames());
    }

    private void printDdl(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: ddl <tableName>");
            return;
        }
        System.out.println(xmlCatalogService.getTableDDL(args[1]));
    }

    private void printColumns(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: columns <tableName>");
            return;
        }
        System.out.println(xmlCatalogService.getColumnNames(args[1]));
    }

    private void printIsId(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: is-id <tableName> <columnName>");
            return;
        }
        boolean result = xmlCatalogService.isColumnId(args[1], args[2]);
        System.out.println(result);
    }

    private void printDdlChange(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: ddl-change <tableName>");
            return;
        }
        System.out.println(xmlCatalogService.getDDLChange(args[1]));
    }

    private void runUpdate(String[] args) {
        if (args.length == 1) {
            xmlCatalogService.update();
            System.out.println("Updated all tables.");
            return;
        }
        if (args.length == 2) {
            xmlCatalogService.update(args[1]);
            System.out.println("Updated table: " + args[1]);
            return;
        }
        System.out.println("Usage: update [tableName]");
        System.out.println("Received args: " + Arrays.toString(args));
    }

    private void printHelp() {
        System.out.println("Commands:");
        System.out.println("  list");
        System.out.println("  ddl <tableName>");
        System.out.println("  columns <tableName>");
        System.out.println("  is-id <tableName> <columnName>");
        System.out.println("  ddl-change <tableName>");
        System.out.println("  update [tableName]");
    }
}
