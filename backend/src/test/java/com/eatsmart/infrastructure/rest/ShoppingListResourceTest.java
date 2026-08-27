package com.eatsmart.infrastructure.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.eatsmart.application.GenerateShoppingListUseCase;
import com.eatsmart.domain.exception.AnalysisException;
import com.eatsmart.domain.model.ShoppingList;
import com.eatsmart.domain.model.ShoppingListCategory;
import com.eatsmart.domain.model.ShoppingListItem;
import com.eatsmart.domain.model.ShoppingListItemType;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class ShoppingListResourceTest {

    @InjectMock
    GenerateShoppingListUseCase generateShoppingList;

    private static Map<String, Object> validRequest() {
        return Map.of(
                "products", List.of("pan blanco", "yogur azucarado"),
                "suggestions", "## Mejoras\npan blanco → pan integral",
                "goal", "LOSE");
    }

    @Test
    void generate_missingProducts_returnsBadRequest() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("suggestions", "sug", "goal", "LOSE"))
                .when()
                .post("/api/shopping-lists/generate")
                .then()
                .statusCode(400)
                .body("message", equalTo("Faltan los productos del ticket. Envía una lista no vacía en 'products'."));
    }

    @Test
    void generate_emptyProducts_returnsBadRequest() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("products", List.of(), "suggestions", "sug", "goal", "LOSE"))
                .when()
                .post("/api/shopping-lists/generate")
                .then()
                .statusCode(400)
                .body("message", equalTo("Faltan los productos del ticket. Envía una lista no vacía en 'products'."));
    }

    @Test
    void generate_missingSuggestions_returnsBadRequest() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("products", List.of("pan"), "goal", "LOSE"))
                .when()
                .post("/api/shopping-lists/generate")
                .then()
                .statusCode(400)
                .body("message", equalTo("Faltan las sugerencias del análisis. Envía un texto no vacío en 'suggestions'."));
    }

    @Test
    void generate_invalidGoal_returnsBadRequest() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("products", List.of("pan"), "suggestions", "sug", "goal", "INVALID"))
                .when()
                .post("/api/shopping-lists/generate")
                .then()
                .statusCode(400)
                .body("message", equalTo("Objetivo no válido. Valores admitidos: LOSE, MAINTAIN, GAIN."));
    }

    @Test
    void generate_invalidDiet_returnsBadRequest() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("products", List.of("pan"), "suggestions", "sug",
                        "goal", "LOSE", "dietPreference", "KETO"))
                .when()
                .post("/api/shopping-lists/generate")
                .then()
                .statusCode(400)
                .body("message", equalTo("Preferencia dietética no válida. Valores admitidos: NONE, VEGETARIAN, VEGAN, OTHER."));
    }

    @Test
    void generate_analysisException_returnsBadGateway() throws Exception {
        when(generateShoppingList.generate(Mockito.anyList(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyBoolean(), Mockito.anyString(), Mockito.anyString()))
                .thenThrow(new AnalysisException("Service unavailable", null));

        given()
                .contentType(ContentType.JSON)
                .body(validRequest())
                .when()
                .post("/api/shopping-lists/generate")
                .then()
                .statusCode(502)
                .body("message", equalTo("Service unavailable"));
    }

    @Test
    void generate_success_returnsOk() throws Exception {
        ShoppingList list = new ShoppingList(List.of(
                new ShoppingListCategory("Panadería y cereales", List.of(
                        new ShoppingListItem("Pan integral", ShoppingListItemType.REPLACE,
                                "Pan blanco", "Más fibra"))),
                new ShoppingListCategory("Fruta y verdura", List.of(
                        new ShoppingListItem("Manzanas", ShoppingListItemType.KEEP, null, null)))));
        when(generateShoppingList.generate(Mockito.anyList(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyBoolean(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(list);

        given()
                .contentType(ContentType.JSON)
                .body(validRequest())
                .when()
                .post("/api/shopping-lists/generate")
                .then()
                .statusCode(200)
                .body("categories.size()", equalTo(2))
                .body("categories[0].name", equalTo("Panadería y cereales"))
                .body("categories[0].items[0].name", equalTo("Pan integral"))
                .body("categories[0].items[0].type", equalTo("REPLACE"))
                .body("categories[0].items[0].replaces", equalTo("Pan blanco"))
                .body("categories[1].items[0].type", equalTo("KEEP"))
                .body("categories[1].items[0].replaces", nullValue());
    }
}
