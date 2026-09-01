package com.eatsmart.infrastructure.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.eatsmart.application.ChatWithNutritionistUseCase;
import com.eatsmart.domain.exception.AnalysisException;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class ChatResourceTest {

    @InjectMock
    ChatWithNutritionistUseCase chatWithNutritionist;

    private static Map<String, Object> validReceiptRequest() {
        return Map.of(
                "products", List.of("pan blanco", "yogur azucarado"),
                "suggestions", "## Mejoras\npan blanco → pan integral",
                "goal", "LOSE",
                "question", "¿Qué pan me recomiendas?");
    }

    private static Map<String, Object> validProductRequest() {
        return Map.of(
                "product", "Yogur azucarado",
                "nutrition", "Mucho azúcar añadido",
                "score", 4,
                "goal", "MAINTAIN",
                "question", "¿Tiene mucho azúcar?");
    }

    @Test
    void chat_missingQuestion_returnsBadRequest() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("products", List.of("pan"), "suggestions", "sug", "goal", "LOSE"))
                .when()
                .post("/api/chat")
                .then()
                .statusCode(400)
                .body("message", equalTo("Falta la pregunta del usuario. Envía un texto no vacío en 'question'."));
    }

    @Test
    void chat_missingProducts_returnsBadRequest() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("suggestions", "sug", "goal", "LOSE", "question", "¿algo?"))
                .when()
                .post("/api/chat")
                .then()
                .statusCode(400)
                .body("message", equalTo("Faltan los productos del ticket. Envía una lista no vacía en 'products'."));
    }

    @Test
    void chat_productContextMissingNutrition_returnsBadRequest() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("product", "Yogur", "goal", "LOSE", "question", "¿algo?"))
                .when()
                .post("/api/chat")
                .then()
                .statusCode(400)
                .body("message", equalTo("Falta la información nutricional del producto. Envía un texto no vacío en 'nutrition'."));
    }

    @Test
    void chat_invalidGoal_returnsBadRequest() {
        given()
                .contentType(ContentType.JSON)
                .body(Map.of("products", List.of("pan"), "suggestions", "sug",
                        "goal", "INVALID", "question", "¿algo?"))
                .when()
                .post("/api/chat")
                .then()
                .statusCode(400)
                .body("message", equalTo("Objetivo no válido. Valores admitidos: LOSE, MAINTAIN, GAIN."));
    }

    @Test
    void chat_analysisException_returnsBadGateway() throws Exception {
        when(chatWithNutritionist.chat(Mockito.any(), Mockito.anyList(), Mockito.anyString()))
                .thenThrow(new AnalysisException("Service unavailable", null));

        given()
                .contentType(ContentType.JSON)
                .body(validReceiptRequest())
                .when()
                .post("/api/chat")
                .then()
                .statusCode(502)
                .body("message", equalTo("Service unavailable"));
    }

    @Test
    void chat_receiptContext_success_returnsOk() throws Exception {
        when(chatWithNutritionist.chat(Mockito.any(), Mockito.anyList(), Mockito.anyString()))
                .thenReturn("Mejor pan integral por su fibra.");

        given()
                .contentType(ContentType.JSON)
                .body(validReceiptRequest())
                .when()
                .post("/api/chat")
                .then()
                .statusCode(200)
                .body("answer", equalTo("Mejor pan integral por su fibra."));
    }

    @Test
    void chat_productContext_success_returnsOk() throws Exception {
        when(chatWithNutritionist.chat(Mockito.any(), Mockito.anyList(), Mockito.anyString()))
                .thenReturn("Sí, tiene 12g de azúcar por ración.");

        given()
                .contentType(ContentType.JSON)
                .body(validProductRequest())
                .when()
                .post("/api/chat")
                .then()
                .statusCode(200)
                .body("answer", equalTo("Sí, tiene 12g de azúcar por ración."));
    }
}
