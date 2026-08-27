package com.eatsmart.domain.model;

import java.util.List;

/** A suggested shopping list grouped by fixed categories. */
public record ShoppingList(List<ShoppingListCategory> categories) {
}
