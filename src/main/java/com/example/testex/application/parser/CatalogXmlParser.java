package com.example.testex.application.parser;

import com.example.testex.application.config.XmlParserSecurityProperties;
import com.example.testex.domain.model.ColumnSpec;
import com.example.testex.domain.model.ColumnType;
import com.example.testex.domain.model.OfferMeta;
import com.example.testex.domain.model.OfferParam;
import com.example.testex.domain.model.OfferVendor;
import com.example.testex.domain.model.ParsedCatalog;
import com.example.testex.domain.model.TableData;
import com.example.testex.domain.util.ColumnNameNormalizer;
import groovy.xml.XmlSlurper;
import groovy.xml.XmlUtil;
import groovy.xml.slurpersupport.GPathResult;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

@Component
public class CatalogXmlParser {

    private final String xmlUrl;
    private final ColumnNameNormalizer columnNameNormalizer;
    private final XmlParserSecurityProperties xmlParserSecurityProperties;

    public CatalogXmlParser(
            @Value("${app.xml-url}") String xmlUrl,
            XmlParserSecurityProperties xmlParserSecurityProperties
    ) {
        this.xmlUrl = xmlUrl;
        this.xmlParserSecurityProperties = xmlParserSecurityProperties;
        this.columnNameNormalizer = new ColumnNameNormalizer();
    }

    public ParsedCatalog parse() {
        Document document = parseDocumentWithXmlSlurper();
        List<OfferVendor> offerVendors = new ArrayList<>();
        List<OfferMeta> offerMetas = new ArrayList<>();
        List<OfferParam> offerParams = new ArrayList<>();

        LinkedHashMap<String, TableData> tables = new LinkedHashMap<>();
        tables.put("currency", buildTableData("currency", readCurrencyRows(document)));
        tables.put("categories", buildTableData("categories", readCategoryRows(document)));
        tables.put("offers", buildTableData("offers", readOfferRows(document, offerVendors, offerMetas, offerParams)));
        return new ParsedCatalog(tables, offerVendors, offerMetas, offerParams);
    }

    private Document parseDocumentWithXmlSlurper() {
        try {
            XmlSlurper xmlSlurper = new XmlSlurper();
            String xmlContent;
            try (InputStream inputStream = URI.create(xmlUrl).toURL().openStream()) {
                xmlContent = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
            }
            xmlContent = xmlContent.replaceAll("(?is)<!DOCTYPE[^>]*>", "");
            GPathResult parsed = xmlSlurper.parseText(xmlContent);
            String serialized = XmlUtil.serialize(parsed);

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            for (XmlParserSecurityProperties.Feature feature : xmlParserSecurityProperties.getFeatures()) {
                factory.setFeature(feature.getUri(), feature.isEnabled());
            }
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            try (ByteArrayInputStream stream = new ByteArrayInputStream(serialized.getBytes(StandardCharsets.UTF_8))) {
                return builder.parse(stream);
            }
        } catch (SAXException exception) {
            throw new IllegalStateException("Failed to parse XML with XmlSlurper", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load XML catalog", exception);
        }
    }

    private List<Map<String, String>> readCurrencyRows(Document document) {
        List<Map<String, String>> rows = new ArrayList<>();
        NodeList nodes = selectNodes(document, "/yml_catalog/shop/currencies/currency");
        for (int index = 0; index < nodes.getLength(); index++) {
            Element element = (Element) nodes.item(index);
            LinkedHashMap<String, String> row = new LinkedHashMap<>();
            addAttributes(element, row);
            rows.add(row);
        }
        return rows;
    }

    private List<Map<String, String>> readCategoryRows(Document document) {
        List<Map<String, String>> rows = new ArrayList<>();
        NodeList nodes = selectNodes(document, "/yml_catalog/shop/categories/category");
        for (int index = 0; index < nodes.getLength(); index++) {
            Element element = (Element) nodes.item(index);
            LinkedHashMap<String, String> row = new LinkedHashMap<>();
            addAttributes(element, row);
            String name = safeText(element.getTextContent());
            if (!name.isEmpty()) {
                row.put("name", name);
            }
            rows.add(row);
        }
        return rows;
    }

    private List<Map<String, String>> readOfferRows(
            Document document,
            List<OfferVendor> offerVendors,
            List<OfferMeta> offerMetas,
            List<OfferParam> offerParams
    ) {
        List<Map<String, String>> rows = new ArrayList<>();
        NodeList nodes = selectNodes(document, "/yml_catalog/shop/offers/offer");
        for (int index = 0; index < nodes.getLength(); index++) {
            Element offerElement = (Element) nodes.item(index);
            String offerId = safeText(offerElement.getAttribute("id"));
            String available = safeText(offerElement.getAttribute("available"));

            String categoryId = "";
            String currencyId = "";
            String price = "";
            String count = "";

            String vendor = "";
            String vendorCode = "";
            String url = "";
            String picture = "";
            String name = "";
            String description = "";

            NodeList children = offerElement.getChildNodes();
            for (int childIndex = 0; childIndex < children.getLength(); childIndex++) {
                Node childNode = children.item(childIndex);
                if (childNode.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }
                Element childElement = (Element) childNode;
                String childName = childElement.getTagName();
                String value = safeText(childElement.getTextContent());
                if ("param".equals(childName)) {
                    String paramName = safeText(childElement.getAttribute("name"));
                    String normalizedName = paramName.isEmpty() ? "unnamed" : paramName;
                    if (!offerId.isEmpty()) {
                        offerParams.add(new OfferParam(offerId, normalizedName, value));
                    }
                    continue;
                }

                switch (childName) {
                    case "categoryId" -> categoryId = value;
                    case "currencyId" -> currencyId = value;
                    case "price" -> price = value;
                    case "count" -> count = value;
                    case "vendor" -> vendor = value;
                    case "vendorCode" -> vendorCode = value;
                    case "url" -> url = value;
                    case "picture" -> picture = value;
                    case "name" -> name = value;
                    case "description" -> description = value;
                    default -> {
                    }
                }
            }

            LinkedHashMap<String, String> row = new LinkedHashMap<>();
            row.put("id", offerId);
            row.put("categoryId", categoryId);
            row.put("currencyId", currencyId);
            row.put("vendorCode", vendorCode);
            row.put("available", available);
            row.put("price", price);
            row.put("count", count);
            rows.add(row);

            if (!offerId.isEmpty()) {
                offerVendors.add(new OfferVendor(offerId, vendor, vendorCode));
                offerMetas.add(new OfferMeta(
                        offerId,
                        url,
                        picture,
                        name,
                        description
                ));
            }
        }
        return rows;
    }

    private TableData buildTableData(String tableName, List<Map<String, String>> rawRows) {
        LinkedHashSet<String> sourceKeys = new LinkedHashSet<>();
        for (Map<String, String> rawRow : rawRows) {
            sourceKeys.addAll(rawRow.keySet());
        }

        LinkedHashMap<String, String> sourceToColumn = new LinkedHashMap<>();
        Set<String> usedColumnNames = new LinkedHashSet<>();
        for (String sourceKey : sourceKeys) {
            String baseColumnName = columnNameNormalizer.normalize(sourceKey);
            String candidate = baseColumnName;
            int suffix = 1;
            while (usedColumnNames.contains(candidate)) {
                candidate = baseColumnName + "_" + suffix;
                suffix++;
            }
            usedColumnNames.add(candidate);
            sourceToColumn.put(sourceKey, candidate);
        }

        LinkedHashMap<String, List<String>> valuesByColumn = new LinkedHashMap<>();
        for (String sourceKey : sourceKeys) {
            String columnName = sourceToColumn.get(sourceKey);
            valuesByColumn.put(columnName, new ArrayList<>());
        }
        for (Map<String, String> rawRow : rawRows) {
            for (String sourceKey : sourceKeys) {
                String columnName = sourceToColumn.get(sourceKey);
                valuesByColumn.get(columnName).add(rawRow.get(sourceKey));
            }
        }

        LinkedHashMap<String, ColumnSpec> columns = new LinkedHashMap<>();
        for (String sourceKey : sourceKeys) {
            String columnName = sourceToColumn.get(sourceKey);
            ColumnType columnType = inferColumnType(valuesByColumn.get(columnName));
            columns.put(columnName, new ColumnSpec(sourceKey, columnName, columnType));
        }

        List<Map<String, String>> normalizedRows = new ArrayList<>();
        for (Map<String, String> rawRow : rawRows) {
            LinkedHashMap<String, String> normalizedRow = new LinkedHashMap<>();
            for (String sourceKey : sourceKeys) {
                normalizedRow.put(sourceToColumn.get(sourceKey), rawRow.get(sourceKey));
            }
            normalizedRows.add(normalizedRow);
        }

        String idColumn = sourceToColumn.getOrDefault("id", null);
        if (idColumn == null && !columns.isEmpty()) {
            idColumn = columns.keySet().iterator().next();
        }

        return new TableData(tableName, idColumn, columns, normalizedRows);
    }

    private ColumnType inferColumnType(List<String> values) {
        boolean allBoolean = true;
        boolean allInteger = true;
        boolean allDecimal = true;
        int nonEmptyValues = 0;

        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            nonEmptyValues++;

            if (!isBoolean(value)) {
                allBoolean = false;
            }

            if (!isInteger(value)) {
                allInteger = false;
            }

            if (!isDecimal(value)) {
                allDecimal = false;
            }
        }

        if (nonEmptyValues == 0) {
            return ColumnType.TEXT;
        }
        if (allBoolean) {
            return ColumnType.BOOLEAN;
        }
        if (allInteger) {
            return ColumnType.BIGINT;
        }
        if (allDecimal) {
            return ColumnType.NUMERIC;
        }
        return ColumnType.TEXT;
    }

    private boolean isBoolean(String value) {
        String normalized = value.trim();
        return "true".equalsIgnoreCase(normalized) || "false".equalsIgnoreCase(normalized);
    }

    private boolean isInteger(String value) {
        String normalized = value.trim().replace(" ", "").replace(",", "");
        return normalized.matches("-?\\d+");
    }

    private boolean isDecimal(String value) {
        String normalized = value.trim().replace(" ", "").replace(",", ".");
        return normalized.matches("-?\\d+(\\.\\d+)?");
    }

    private NodeList selectNodes(Document document, String xpathExpression) {
        try {
            XPath xpath = XPathFactory.newInstance().newXPath();
            return (NodeList) xpath.evaluate(xpathExpression, document, XPathConstants.NODESET);
        } catch (XPathExpressionException exception) {
            throw new IllegalStateException("Invalid xpath: " + xpathExpression, exception);
        }
    }

    private void addAttributes(Element element, Map<String, String> targetRow) {
        for (int attributeIndex = 0; attributeIndex < element.getAttributes().getLength(); attributeIndex++) {
            Node attribute = element.getAttributes().item(attributeIndex);
            targetRow.put(attribute.getNodeName(), safeText(attribute.getNodeValue()));
        }
    }

    private String safeText(String value) {
        return value == null ? "" : value.trim();
    }
}
