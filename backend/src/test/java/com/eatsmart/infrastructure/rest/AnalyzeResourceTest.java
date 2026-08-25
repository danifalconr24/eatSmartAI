package com.eatsmart.infrastructure.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.eatsmart.application.AnalyzeReceiptUseCase;
import com.eatsmart.domain.exception.AnalysisException;
import com.eatsmart.domain.exception.UnreadableReceiptException;
import com.eatsmart.domain.model.AnalyzeResponse;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class AnalyzeResourceTest {

    @InjectMock
    AnalyzeReceiptUseCase analyzeReceipt;

    @Test
    void analyze_missingImage_returnsBadRequest() {
        given()
                .contentType(ContentType.MULTIPART)
                .multiPart("goal", "LOSE")
                .when()
                .post("/api/analyze")
                .then()
                .statusCode(400)
                .body("message", equalTo("Falta la imagen del ticket. Envía una foto en el campo 'image'."));
    }

    @Test
    void analyze_missingGoal_returnsBadRequest() {
        given()
                .contentType(ContentType.MULTIPART)
                .multiPart("image", "ticket.jpg", new byte[]{1}, "image/jpeg")
                .when()
                .post("/api/analyze")
                .then()
                .statusCode(400)
                .body("message", equalTo("Objetivo no válido. Valores admitidos: LOSE, MAINTAIN, GAIN."));
    }

    @Test
    void analyze_invalidGoal_returnsBadRequest() {
        given()
                .contentType(ContentType.MULTIPART)
                .multiPart("image", "ticket.jpg", new byte[]{1}, "image/jpeg")
                .multiPart("goal", "INVALID")
                .when()
                .post("/api/analyze")
                .then()
                .statusCode(400)
                .body("message", equalTo("Objetivo no válido. Valores admitidos: LOSE, MAINTAIN, GAIN."));
    }

    @Test
    void analyze_invalidDiet_returnsBadRequest() {
        given()
                .contentType(ContentType.MULTIPART)
                .multiPart("image", "ticket.jpg", new byte[]{1}, "image/jpeg")
                .multiPart("goal", "LOSE")
                .multiPart("dietPreference", "KETO")
                .when()
                .post("/api/analyze")
                .then()
                .statusCode(400)
                .body("message", equalTo("Preferencia dietética no válida. Valores admitidos: NONE, VEGETARIAN, VEGAN, OTHER."));
    }

    @Test
    void analyze_unreadableReceipt_returnsBadRequest() throws Exception {
        when(analyzeReceipt.analyze(Mockito.any(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyBoolean(),
                Mockito.anyString(), Mockito.anyString()))
                .thenThrow(new UnreadableReceiptException("No es un ticket"));

        given()
                .contentType(ContentType.MULTIPART)
                .multiPart("image", "ticket.jpg", new byte[]{1}, "image/jpeg")
                .multiPart("goal", "LOSE")
                .when()
                .post("/api/analyze")
                .then()
                .statusCode(400)
                .body("message", equalTo("No es un ticket"));
    }

    @Test
    void analyze_analysisException_returnsBadGateway() throws Exception {
        when(analyzeReceipt.analyze(Mockito.any(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyBoolean(),
                Mockito.anyString(), Mockito.anyString()))
                .thenThrow(new AnalysisException("Service unavailable", null));

        given()
                .contentType(ContentType.MULTIPART)
                .multiPart("image", "ticket.jpg", new byte[]{1}, "image/jpeg")
                .multiPart("goal", "LOSE")
                .when()
                .post("/api/analyze")
                .then()
                .statusCode(502)
                .body("message", equalTo("Service unavailable"));
    }

    @Test
    void analyze_success_returnsOk() throws Exception {
        AnalyzeResponse response = new AnalyzeResponse(List.of("leche", "pan"), "Compra equilibrada");
        when(analyzeReceipt.analyze(Mockito.any(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyBoolean(),
                Mockito.anyString(), Mockito.anyString()))
                .thenReturn(response);

        given()
                .contentType(ContentType.MULTIPART)
                .multiPart("image", "ticket.jpg", new byte[]{1}, "image/jpeg")
                .multiPart("goal", "LOSE")
                .multiPart("dietPreference", "VEGETARIAN")
                .multiPart("budgetMatters", "true")
                .multiPart("allergies", "Gluten")
                .when()
                .post("/api/analyze")
                .then()
                .statusCode(200)
                .body("products.size()", equalTo(2))
                .body("suggestions", equalTo("Compra equilibrada"));
    }

    @Test
    void analyze_goalMaintain_returnsOk() throws Exception {
        AnalyzeResponse response = new AnalyzeResponse(List.of("fruta"), "Bien");
        when(analyzeReceipt.analyze(Mockito.any(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyBoolean(),
                Mockito.anyString(), Mockito.anyString()))
                .thenReturn(response);

        given()
                .contentType(ContentType.MULTIPART)
                .multiPart("image", "ticket.jpg", new byte[]{1}, "image/jpeg")
                .multiPart("goal", "MAINTAIN")
                .when()
                .post("/api/analyze")
                .then()
                .statusCode(200);
    }

    @Test
    void analyze_goalGain_returnsOk() throws Exception {
        AnalyzeResponse response = new AnalyzeResponse(List.of("proteina"), "Bien");
        when(analyzeReceipt.analyze(Mockito.any(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyBoolean(),
                Mockito.anyString(), Mockito.anyString()))
                .thenReturn(response);

        given()
                .contentType(ContentType.MULTIPART)
                .multiPart("image", "ticket.jpg", new byte[]{1}, "image/jpeg")
                .multiPart("goal", "GAIN")
                .when()
                .post("/api/analyze")
                .then()
                .statusCode(200);
    }
}
