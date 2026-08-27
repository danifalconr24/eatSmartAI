package com.eatsmart.infrastructure.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.eatsmart.application.AnalyzeProductUseCase;
import com.eatsmart.domain.exception.AnalysisException;
import com.eatsmart.domain.exception.UnreadableReceiptException;
import com.eatsmart.domain.model.ProductAnalyzeResponse;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
class ProductResourceTest {

    @InjectMock
    AnalyzeProductUseCase analyzeProduct;

    @Test
    void analyze_missingImage_returnsBadRequest() {
        given()
                .contentType(ContentType.MULTIPART)
                .multiPart("goal", "LOSE")
                .when()
                .post("/api/analyze/product")
                .then()
                .statusCode(400)
                .body("message", equalTo("Falta la imagen del producto. Envía una foto en el campo 'image'."));
    }

    @Test
    void analyze_missingGoal_returnsBadRequest() {
        given()
                .contentType(ContentType.MULTIPART)
                .multiPart("image", "product.jpg", new byte[]{1}, "image/jpeg")
                .when()
                .post("/api/analyze/product")
                .then()
                .statusCode(400)
                .body("message", equalTo("Objetivo no válido. Valores admitidos: LOSE, MAINTAIN, GAIN."));
    }

    @Test
    void analyze_invalidGoal_returnsBadRequest() {
        given()
                .contentType(ContentType.MULTIPART)
                .multiPart("image", "product.jpg", new byte[]{1}, "image/jpeg")
                .multiPart("goal", "INVALID")
                .when()
                .post("/api/analyze/product")
                .then()
                .statusCode(400)
                .body("message", equalTo("Objetivo no válido. Valores admitidos: LOSE, MAINTAIN, GAIN."));
    }

    @Test
    void analyze_invalidDiet_returnsBadRequest() {
        given()
                .contentType(ContentType.MULTIPART)
                .multiPart("image", "product.jpg", new byte[]{1}, "image/jpeg")
                .multiPart("goal", "LOSE")
                .multiPart("dietPreference", "KETO")
                .when()
                .post("/api/analyze/product")
                .then()
                .statusCode(400)
                .body("message", equalTo("Preferencia dietética no válida. Valores admitidos: NONE, VEGETARIAN, VEGAN, OTHER."));
    }

    @Test
    void analyze_unreadableProduct_returnsBadRequest() throws Exception {
        when(analyzeProduct.analyze(Mockito.any(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyBoolean(),
                Mockito.anyString(), Mockito.anyString()))
                .thenThrow(new UnreadableReceiptException("No se reconoce el producto"));

        given()
                .contentType(ContentType.MULTIPART)
                .multiPart("image", "product.jpg", new byte[]{1}, "image/jpeg")
                .multiPart("goal", "LOSE")
                .when()
                .post("/api/analyze/product")
                .then()
                .statusCode(400)
                .body("message", equalTo("No se reconoce el producto"));
    }

    @Test
    void analyze_analysisException_returnsBadGateway() throws Exception {
        when(analyzeProduct.analyze(Mockito.any(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyBoolean(),
                Mockito.anyString(), Mockito.anyString()))
                .thenThrow(new AnalysisException("Service unavailable", null));

        given()
                .contentType(ContentType.MULTIPART)
                .multiPart("image", "product.jpg", new byte[]{1}, "image/jpeg")
                .multiPart("goal", "LOSE")
                .when()
                .post("/api/analyze/product")
                .then()
                .statusCode(502)
                .body("message", equalTo("Service unavailable"));
    }

    @Test
    void analyze_successScoreBelowThreshold_returnsAlternative() throws Exception {
        ProductAnalyzeResponse.Alternative alt = new ProductAnalyzeResponse.Alternative("galletas integrales", "menos azúcar");
        ProductAnalyzeResponse response = new ProductAnalyzeResponse("galletas oreo", 4, "## Info nutricional", alt);
        when(analyzeProduct.analyze(Mockito.any(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyBoolean(),
                Mockito.anyString(), Mockito.anyString()))
                .thenReturn(response);

        given()
                .contentType(ContentType.MULTIPART)
                .multiPart("image", "product.jpg", new byte[]{1}, "image/jpeg")
                .multiPart("goal", "LOSE")
                .when()
                .post("/api/analyze/product")
                .then()
                .statusCode(200)
                .body("product", equalTo("galletas oreo"))
                .body("score", equalTo(4))
                .body("alternative.name", equalTo("galletas integrales"));
    }

    @Test
    void analyze_successScoreAboveThreshold_alternativeNull() throws Exception {
        ProductAnalyzeResponse response = new ProductAnalyzeResponse("leche entera", 8, "## Info nutricional", null);
        when(analyzeProduct.analyze(Mockito.any(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyBoolean(),
                Mockito.anyString(), Mockito.anyString()))
                .thenReturn(response);

        given()
                .contentType(ContentType.MULTIPART)
                .multiPart("image", "product.jpg", new byte[]{1}, "image/jpeg")
                .multiPart("goal", "MAINTAIN")
                .when()
                .post("/api/analyze/product")
                .then()
                .statusCode(200)
                .body("product", equalTo("leche entera"))
                .body("score", equalTo(8))
                .body("alternative", nullValue());
    }

    @Test
    void analyze_withDietAndAllergies_returnsOk() throws Exception {
        ProductAnalyzeResponse response = new ProductAnalyzeResponse("yogur natural", 7, "## Info", null);
        when(analyzeProduct.analyze(Mockito.any(), Mockito.anyString(),
                Mockito.anyString(), Mockito.anyBoolean(),
                Mockito.anyString(), Mockito.anyString()))
                .thenReturn(response);

        given()
                .contentType(ContentType.MULTIPART)
                .multiPart("image", "product.jpg", new byte[]{1}, "image/jpeg")
                .multiPart("goal", "LOSE")
                .multiPart("dietPreference", "VEGAN")
                .multiPart("budgetMatters", "true")
                .multiPart("allergies", "Lactosa")
                .when()
                .post("/api/analyze/product")
                .then()
                .statusCode(200);
    }
}
