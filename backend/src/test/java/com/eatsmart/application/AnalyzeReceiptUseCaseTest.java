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
import com.eatsmart.domain.model.AnalyzeResponse;
import com.eatsmart.domain.port.ReceiptAnalysisGateway;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AnalyzeReceiptUseCaseTest {

    @Mock
    ReceiptPromptBuilder promptBuilder;

    @Mock
    AnalysisResultParser resultParser;

    @Mock
    Instance<ReceiptAnalysisGateway> gatewaysInstance;

    @InjectMocks
    AnalyzeReceiptUseCase useCase;

    @Priority(1)
    static class FakePrimaryGateway implements ReceiptAnalysisGateway {
        String rawResponse = "raw";
        boolean enabled = true;
        @Override public String name() { return "OpenRouter"; }
        @Override public boolean isEnabled() { return enabled; }
        @Override public String analyze(byte[] imageBytes, String mimeType, String prompt)
                throws AnalysisException { return rawResponse; }
    }

    @Priority(2)
    static class FakeFallbackGateway implements ReceiptAnalysisGateway {
        String rawResponse = "raw";
        boolean enabled = true;
        @Override public String name() { return "Gemini"; }
        @Override public boolean isEnabled() { return enabled; }
        @Override public String analyze(byte[] imageBytes, String mimeType, String prompt)
                throws AnalysisException { return rawResponse; }
    }

    @SuppressWarnings("unchecked")
    private Instance.Handle<ReceiptAnalysisGateway> handle(ReceiptAnalysisGateway gw) {
        Instance.Handle<ReceiptAnalysisGateway> h =
                org.mockito.Mockito.mock(Instance.Handle.class);
        Bean<ReceiptAnalysisGateway> bean =
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
        doReturn(new AnalyzeResponse(List.of("leche"), "sug")).when(resultParser).parse("raw");

        AnalyzeResponse response = useCase.analyze(new byte[]{1}, "image/jpeg",
                "LOSE", false, "", "NONE");

        assertThat(response.products()).containsExactly("leche");
        assertThat(response.suggestions()).isEqualTo("sug");
    }

    @Test
    void analyze_primaryDisabled_skipsToNext() throws Exception {
        FakePrimaryGateway primary = new FakePrimaryGateway();
        primary.enabled = false;
        FakeFallbackGateway fallback = new FakeFallbackGateway();
        doReturn(List.of(handle(primary), handle(fallback))).when(gatewaysInstance).handles();
        doReturn("prompt").when(promptBuilder).build("LOSE", false, "", "NONE");
        doReturn(new AnalyzeResponse(List.of("pan"), "sug2")).when(resultParser).parse("raw");

        AnalyzeResponse response = useCase.analyze(new byte[]{1}, "image/jpeg",
                "LOSE", false, "", "NONE");

        assertThat(response.products()).containsExactly("pan");
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
        doReturn(new AnalyzeResponse(List.of("pan"), "sug2")).when(resultParser).parse("raw");

        AnalyzeResponse response = useCase.analyze(new byte[]{1}, "image/jpeg",
                "LOSE", false, "", "NONE");

        assertThat(response.products()).containsExactly("pan");
    }

    @Test
    void analyze_unreadableReceipt_throwsImmediately() throws Exception {
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
}
