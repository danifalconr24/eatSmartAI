package com.eatsmart.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

import java.util.List;

import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.Bean;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.eatsmart.domain.exception.AnalysisException;
import com.eatsmart.domain.exception.UnreadableReceiptException;
import com.eatsmart.domain.model.ProductAnalyzeResponse;
import com.eatsmart.application.port.ProductAnalysisGateway;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AnalyzeProductUseCaseTest {

    @Mock
    ProductPromptBuilder promptBuilder;

    @Mock
    ProductResultParser resultParser;

    @Mock
    Instance<ProductAnalysisGateway> gatewaysInstance;

    @InjectMocks
    AnalyzeProductUseCase useCase;

    @Priority(1)
    static class FakePrimaryGateway implements ProductAnalysisGateway {
        String rawResponse = "raw";
        boolean enabled = true;
        @Override public String name() { return "OpenRouter"; }
        @Override public boolean isEnabled() { return enabled; }
        @Override public String analyze(byte[] imageBytes, String mimeType, String prompt)
                throws AnalysisException { return rawResponse; }
    }

    @Priority(2)
    static class FakeFallbackGateway implements ProductAnalysisGateway {
        String rawResponse = "raw";
        boolean enabled = true;
        @Override public String name() { return "Gemini"; }
        @Override public boolean isEnabled() { return enabled; }
        @Override public String analyze(byte[] imageBytes, String mimeType, String prompt)
                throws AnalysisException { return rawResponse; }
    }

    @SuppressWarnings("unchecked")
    private Instance.Handle<ProductAnalysisGateway> handle(ProductAnalysisGateway gw) {
        Instance.Handle<ProductAnalysisGateway> h =
                org.mockito.Mockito.mock(Instance.Handle.class);
        Bean<ProductAnalysisGateway> bean =
                org.mockito.Mockito.mock(Bean.class);
        doReturn(bean).when(h).getBean();
        doReturn((Class) gw.getClass()).when(bean).getBeanClass();
        doReturn(gw).when(h).get();
        return h;
    }

    @Test
    void analyze_primarySucceeds_returnsResult() throws Exception {
        FakePrimaryGateway primary = new FakePrimaryGateway();
        doReturn(List.of(handle(primary))).when(gatewaysInstance).handles();
        doReturn("prompt").when(promptBuilder).build("LOSE", false, "", "NONE");
        ProductAnalyzeResponse parsed = new ProductAnalyzeResponse(
                "galletas oreo", 4, "## Info", new ProductAnalyzeResponse.Alternative("integrales", "mejor"));
        doReturn(parsed).when(resultParser).parse("raw");

        ProductAnalyzeResponse response = useCase.analyze(new byte[]{1}, "image/jpeg",
                "LOSE", false, "", "NONE");

        assertThat(response.product()).isEqualTo("galletas oreo");
        assertThat(response.score()).isEqualTo(4);
        assertThat(response.alternative()).isNotNull();
    }

    @Test
    void analyze_primaryDisabled_skipsToNext() throws Exception {
        FakePrimaryGateway primary = new FakePrimaryGateway();
        primary.enabled = false;
        FakeFallbackGateway fallback = new FakeFallbackGateway();
        doReturn(List.of(handle(primary), handle(fallback))).when(gatewaysInstance).handles();
        doReturn("prompt").when(promptBuilder).build("LOSE", false, "", "NONE");
        ProductAnalyzeResponse parsed = new ProductAnalyzeResponse("leche", 8, "## Info", null);
        doReturn(parsed).when(resultParser).parse("raw");

        ProductAnalyzeResponse response = useCase.analyze(new byte[]{1}, "image/jpeg",
                "LOSE", false, "", "NONE");

        assertThat(response.product()).isEqualTo("leche");
    }

    @Test
    void analyze_primaryFails_fallsBackToSecondary() throws Exception {
        FakePrimaryGateway primary = new FakePrimaryGateway() {
            @Override public String analyze(byte[] imageBytes, String mimeType, String prompt)
                    throws AnalysisException { throw new AnalysisException("fail", null); }
        };
        FakeFallbackGateway fallback = new FakeFallbackGateway();
        doReturn(List.of(handle(primary), handle(fallback))).when(gatewaysInstance).handles();
        doReturn("prompt").when(promptBuilder).build("LOSE", false, "", "NONE");
        ProductAnalyzeResponse parsed = new ProductAnalyzeResponse("yogur", 6, "## Info", null);
        doReturn(parsed).when(resultParser).parse("raw");

        ProductAnalyzeResponse response = useCase.analyze(new byte[]{1}, "image/jpeg",
                "LOSE", false, "", "NONE");

        assertThat(response.product()).isEqualTo("yogur");
    }

    @Test
    void analyze_unreadableProduct_throwsImmediately() throws Exception {
        FakePrimaryGateway primary = new FakePrimaryGateway();
        doReturn(List.of(handle(primary))).when(gatewaysInstance).handles();
        doReturn("prompt").when(promptBuilder).build("LOSE", false, "", "NONE");
        doThrow(new UnreadableReceiptException("bad")).when(resultParser).parse("raw");

        assertThatThrownBy(() -> useCase.analyze(new byte[]{1}, "image/jpeg",
                "LOSE", false, "", "NONE"))
                .isInstanceOf(UnreadableReceiptException.class);
    }

    @Test
    void analyze_noEnabledProviders_throwsAnalysisException() {
        FakePrimaryGateway primary = new FakePrimaryGateway();
        primary.enabled = false;
        doReturn(List.of(handle(primary))).when(gatewaysInstance).handles();

        assertThatThrownBy(() -> useCase.analyze(new byte[]{1}, "image/jpeg",
                "LOSE", false, "", "NONE"))
                .isInstanceOf(AnalysisException.class)
                .hasMessageContaining("no está configurado");
    }

    @Test
    void analyze_allProvidersFail_throwsLastAnalysisException() {
        FakePrimaryGateway primary = new FakePrimaryGateway() {
            @Override public String analyze(byte[] imageBytes, String mimeType, String prompt)
                    throws AnalysisException { throw new AnalysisException("fail", null); }
        };
        doReturn(List.of(handle(primary))).when(gatewaysInstance).handles();
        doReturn("prompt").when(promptBuilder).build("LOSE", false, "", "NONE");

        assertThatThrownBy(() -> useCase.analyze(new byte[]{1}, "image/jpeg",
                "LOSE", false, "", "NONE"))
                .isInstanceOf(AnalysisException.class);
    }

    @Test
    void analyze_scoreAboveThreshold_dropsAlternative() throws Exception {
        FakePrimaryGateway primary = new FakePrimaryGateway();
        doReturn(List.of(handle(primary))).when(gatewaysInstance).handles();
        doReturn("prompt").when(promptBuilder).build("LOSE", false, "", "NONE");
        ProductAnalyzeResponse parsed = new ProductAnalyzeResponse(
                "leche", 8, "## Info", new ProductAnalyzeResponse.Alternative("soja", "razón"));
        doReturn(parsed).when(resultParser).parse("raw");

        ProductAnalyzeResponse response = useCase.analyze(new byte[]{1}, "image/jpeg",
                "LOSE", false, "", "NONE");

        assertThat(response.score()).isEqualTo(8);
        assertThat(response.alternative()).isNull();
    }

    @Test
    void analyze_scoreBelowThreshold_keepsAlternative() throws Exception {
        FakePrimaryGateway primary = new FakePrimaryGateway();
        doReturn(List.of(handle(primary))).when(gatewaysInstance).handles();
        doReturn("prompt").when(promptBuilder).build("LOSE", false, "", "NONE");
        ProductAnalyzeResponse.Alternative alt = new ProductAnalyzeResponse.Alternative("integrales", "mejor");
        ProductAnalyzeResponse parsed = new ProductAnalyzeResponse("oreo", 4, "## Info", alt);
        doReturn(parsed).when(resultParser).parse("raw");

        ProductAnalyzeResponse response = useCase.analyze(new byte[]{1}, "image/jpeg",
                "LOSE", false, "", "NONE");

        assertThat(response.score()).isEqualTo(4);
        assertThat(response.alternative()).isNotNull();
        assertThat(response.alternative().name()).isEqualTo("integrales");
    }

    @Test
    void analyze_scoreExactlyThreshold_dropsAlternative() throws Exception {
        FakePrimaryGateway primary = new FakePrimaryGateway();
        doReturn(List.of(handle(primary))).when(gatewaysInstance).handles();
        doReturn("prompt").when(promptBuilder).build("LOSE", false, "", "NONE");
        ProductAnalyzeResponse parsed = new ProductAnalyzeResponse(
                "galletas", 7, "## Info", new ProductAnalyzeResponse.Alternative("otras", "x"));
        doReturn(parsed).when(resultParser).parse("raw");

        ProductAnalyzeResponse response = useCase.analyze(new byte[]{1}, "image/jpeg",
                "LOSE", false, "", "NONE");

        assertThat(response.alternative()).isNull();
    }
}
