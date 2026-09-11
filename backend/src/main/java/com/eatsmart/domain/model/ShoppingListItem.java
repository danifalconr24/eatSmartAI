package com.eatsmart.domain.model;

import java.util.List;

/**
 * A single shopping list item. For {@link ShoppingListItemType#REPLACE} items,
 * {@code replaces} and {@code reason} are mandatory; for other types they are null.
 */
public record ShoppingListItem(String name, ShoppingListItemType type, String replaces, String reason,
        List<Source> sources) {
}
