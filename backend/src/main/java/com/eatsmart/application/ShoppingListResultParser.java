package com.eatsmart.application;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.jboss.logging.Logger;

import com.eatsmart.domain.exception.AnalysisException;
import com.eatsmart.domain.model.ShoppingList;
import com.eatsmart.domain.model.ShoppingListCategory;
import com.eatsmart.domain.model.ShoppingListItem;
import com.eatsmart.domain.model.ShoppingListItemType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Maps the raw text returned by an AI provider into a {@link ShoppingList}.
 * Tolerant with markdown fences and surrounding prose; strict with the
 * expected JSON shape, fixed categories, item types and REPLACE rules.
 * Every validation failure is an {@link AnalysisException} so the use case
 * falls back to the next provider.
 */
@ApplicationScoped
public class ShoppingListResultParser {

    private static final Logger LOG = Logger.getLogger(ShoppingListResultParser.class);
    private static final int MAX_LOG_RAW = 500;

    @Inject
    ObjectMapper mapper;

    public ShoppingList parse(String text) throws AnalysisException {
        JsonNode root = parseJsonObject(text);
        if (root == null) {
            LOG.warnf("Provider response is not valid JSON: %s", truncate(text));
            throw new AnalysisException("El proveedor devolvió una respuesta no interpretable.", null);
        }

        JsonNode categories = root.path("categories");
        if (!categories.isArray() || categories.isEmpty()) {
            throw incomplete(text);
        }

        List<ShoppingListCategory> parsed = new ArrayList<>();
        Set<String> seenNames = new HashSet<>();
        for (JsonNode categoryNode : categories) {
            parsed.add(parseCategory(categoryNode, seenNames, text));
        }
        if (parsed.stream().allMatch(c -> c.items().isEmpty())) {
            throw incomplete(text);
        }
        return new ShoppingList(parsed);
    }

    private ShoppingListCategory parseCategory(JsonNode node, Set<String> seenNames, String raw)
            throws AnalysisException {
        String name = node.path("name").asText("");
        if (!ShoppingListCategory.ALLOWED_NAMES.contains(name)) {
            LOG.warnf("Provider returned disallowed category '%s' in %s", name, truncate(raw));
            throw new AnalysisException("El proveedor devolvió una respuesta incompleta.", null);
        }
        JsonNode items = node.path("items");
        if (!items.isArray()) {
            throw incomplete(raw);
        }
        List<ShoppingListItem> parsedItems = new ArrayList<>();
        for (JsonNode itemNode : items) {
            parsedItems.add(parseItem(itemNode, seenNames, raw));
        }
        return new ShoppingListCategory(name, parsedItems);
    }

    private ShoppingListItem parseItem(JsonNode node, Set<String> seenNames, String raw)
            throws AnalysisException {
        String name = node.path("name").asText("").trim();
        if (name.isEmpty()) {
            throw incomplete(raw);
        }
        if (!seenNames.add(name.toLowerCase(Locale.ROOT))) {
            LOG.warnf("Provider returned duplicate item '%s'", name);
            throw new AnalysisException("El proveedor devolvió una respuesta incompleta.", null);
        }

        ShoppingListItemType type;
        try {
            type = ShoppingListItemType.valueOf(node.path("type").asText(""));
        } catch (IllegalArgumentException e) {
            throw incomplete(raw);
        }

        String replaces = node.path("replaces").isTextual() ? node.path("replaces").asText().trim() : null;
        String reason = node.path("reason").isTextual() ? node.path("reason").asText().trim() : null;
        if (type == ShoppingListItemType.REPLACE) {
            if (replaces == null || replaces.isEmpty() || reason == null || reason.isEmpty()) {
                LOG.warnf("REPLACE item missing 'replaces'/'reason': %s", node);
                throw incomplete(raw);
            }
        } else if (replaces != null || reason != null) {
            LOG.warnf("%s item must not have 'replaces'/'reason': %s", type, node);
            throw incomplete(raw);
        }
        return new ShoppingListItem(name, type, replaces, reason);
    }

    private AnalysisException incomplete(String raw) {
        LOG.warnf("Provider response incomplete or invalid: %s", truncate(raw));
        return new AnalysisException("El proveedor devolvió una respuesta incompleta.", null);
    }

    private JsonNode parseJsonObject(String text) {
        if (text == null) {
            return null;
        }
        String cleaned = text.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceAll("^```(json)?\\s*", "").replaceAll("\\s*```$", "");
        }
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return null;
        }
        try {
            return mapper.readTree(cleaned.substring(start, end + 1));
        } catch (IOException e) {
            return null;
        }
    }

    private static String truncate(String text) {
        if (text == null || text.length() <= MAX_LOG_RAW) {
            return text;
        }
        return text.substring(0, MAX_LOG_RAW) + "...";
    }
}
