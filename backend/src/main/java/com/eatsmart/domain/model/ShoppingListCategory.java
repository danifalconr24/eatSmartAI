package com.eatsmart.domain.model;

import java.util.List;

/** A fixed shopping category with its items. */
public record ShoppingListCategory(String name, List<ShoppingListItem> items) {

    /** Fixed categories allowed in generated shopping lists, in display order. */
    public static final List<String> ALLOWED_NAMES = List.of(
            "Fruta y verdura",
            "Proteínas",
            "Lácteos y alternativas",
            "Despensa",
            "Panadería y cereales",
            "Congelados",
            "Bebidas",
            "Otros");
}
