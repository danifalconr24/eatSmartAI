package com.eatsmart.infrastructure.rest.dto;

import java.util.List;

import com.eatsmart.domain.model.ShoppingList;
import com.eatsmart.domain.model.ShoppingListCategory;
import com.eatsmart.domain.model.ShoppingListItem;
import com.eatsmart.domain.model.ShoppingListItemType;

/** Response body for POST /api/shopping-lists/generate. */
public record GenerateShoppingListResponse(List<Category> categories) {

    public record Category(String name, List<Item> items) {
    }

    public record Item(String name, ShoppingListItemType type, String replaces, String reason) {
    }

    public static GenerateShoppingListResponse from(ShoppingList list) {
        return new GenerateShoppingListResponse(list.categories().stream()
                .map(GenerateShoppingListResponse::from)
                .toList());
    }

    private static Category from(ShoppingListCategory category) {
        return new Category(category.name(), category.items().stream()
                .map(GenerateShoppingListResponse::from)
                .toList());
    }

    private static Item from(ShoppingListItem item) {
        return new Item(item.name(), item.type(), item.replaces(), item.reason());
    }
}
