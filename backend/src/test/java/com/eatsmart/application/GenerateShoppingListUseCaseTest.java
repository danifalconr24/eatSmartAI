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
import com.eatsmart.domain.model.ShoppingList;
import com.eatsmart.domain.model.ShoppingListCategory;
import com.eatsmart.domain.model.ShoppingListItem;
import com.eatsmart.domain.model.ShoppingListItemType;
import com.eatsmart.application.port.ShoppingListGenerationGateway;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GenerateShoppingListUseCaseTest {

    private static final List<String> PRODUCTS = List.of("pan blanco");

    @Mock
    ShoppingListPromptBuilder promptBuilder;

    @Mock
    ShoppingListResultParser resultParser;

    @Mock
    Instance<ShoppingListGenerationGateway> gatewaysInstance;

    @InjectMocks
    GenerateShoppingListUseCase useCase;

    @Priority(1)
    static class FakePrimaryGateway implements ShoppingListGenerationGateway {
        String rawResponse = "raw";
        boolean enabled = true;
        @Override public String name() { return "OpenRouter"; }
        @Override public boolean isEnabled() { return enabled; }
        @Override public String generateText(String prompt) throws AnalysisException { return rawResponse; }
    }

    @Priority(2)
    static class FakeFallbackGateway implements ShoppingListGenerationGateway {
        String rawResponse = "raw";
        boolean enabled = true;
        @Override public String name() { return "Gemini"; }
        @Override public boolean isEnabled() { return enabled; }
        @Override public String generateText(String prompt) throws AnalysisException { return rawResponse; }
    }

    @SuppressWarnings("unchecked")
    private Instance.Handle<ShoppingListGenerationGateway> handle(ShoppingListGenerationGateway gw) {
        Instance.Handle<ShoppingListGenerationGateway> h =
                org.mockito.Mockito.mock(Instance.Handle.class);
        Bean<ShoppingListGenerationGateway> bean =
                org.mockito.Mockito.mock(Bean.class);
        doReturn(bean).when(h).getBean();
        doReturn((Class) gw.getClass()).when(bean).getBeanClass();
        doReturn(gw).when(h).get();
        return h;
    }

    private static ShoppingList sampleList() {
        return new ShoppingList(List.of(new ShoppingListCategory("Despensa",
                List.of(new ShoppingListItem("Pan integral", ShoppingListItemType.REPLACE,
                        "Pan blanco", "Más fibra")))));
    }

    @Test
    void generate_primarySucceeds_returnsResult() throws Exception {
        FakePrimaryGateway primary = new FakePrimaryGateway();
        doReturn(List.of(handle(primary))).when(gatewaysInstance).handles();
        doReturn("prompt").when(promptBuilder)
                .build(PRODUCTS, "sug", "LOSE", false, "", "NONE");
        doReturn(sampleList()).when(resultParser).parse("raw");

        ShoppingList result = useCase.generate(PRODUCTS, "sug", "LOSE", false, "", "NONE");

        assertThat(result.categories()).hasSize(1);
        assertThat(result.categories().get(0).items().get(0).name()).isEqualTo("Pan integral");
    }

    @Test
    void generate_primaryDisabled_skipsToNext() throws Exception {
        FakePrimaryGateway primary = new FakePrimaryGateway();
        primary.enabled = false;
        FakeFallbackGateway fallback = new FakeFallbackGateway();
        doReturn(List.of(handle(primary), handle(fallback))).when(gatewaysInstance).handles();
        doReturn("prompt").when(promptBuilder)
                .build(PRODUCTS, "sug", "LOSE", false, "", "NONE");
        doReturn(sampleList()).when(resultParser).parse("raw");

        ShoppingList result = useCase.generate(PRODUCTS, "sug", "LOSE", false, "", "NONE");

        assertThat(result.categories()).isNotEmpty();
    }

    @Test
    void generate_primaryFails_fallsBackToSecondary() throws Exception {
        FakePrimaryGateway primary = new FakePrimaryGateway() {
            @Override public String generateText(String prompt) throws AnalysisException {
                throw new AnalysisException("fail", null);
            }
        };
        FakeFallbackGateway fallback = new FakeFallbackGateway();
        doReturn(List.of(handle(primary), handle(fallback))).when(gatewaysInstance).handles();
        doReturn("prompt").when(promptBuilder)
                .build(PRODUCTS, "sug", "LOSE", false, "", "NONE");
        doReturn(sampleList()).when(resultParser).parse("raw");

        ShoppingList result = useCase.generate(PRODUCTS, "sug", "LOSE", false, "", "NONE");

        assertThat(result.categories()).isNotEmpty();
    }

    @Test
    void generate_noEnabledProviders_throwsAnalysisException() {
        FakePrimaryGateway primary = new FakePrimaryGateway();
        primary.enabled = false;
        doReturn(List.of(handle(primary))).when(gatewaysInstance).handles();

        assertThatThrownBy(() -> useCase.generate(PRODUCTS, "sug", "LOSE", false, "", "NONE"))
                .isInstanceOf(AnalysisException.class)
                .hasMessageContaining("no está configurado");
    }

    @Test
    void generate_allProvidersFail_throwsLastAnalysisException() {
        FakePrimaryGateway primary = new FakePrimaryGateway() {
            @Override public String generateText(String prompt) throws AnalysisException {
                throw new AnalysisException("fail", null);
            }
        };
        doReturn(List.of(handle(primary))).when(gatewaysInstance).handles();
        doReturn("prompt").when(promptBuilder)
                .build(PRODUCTS, "sug", "LOSE", false, "", "NONE");

        assertThatThrownBy(() -> useCase.generate(PRODUCTS, "sug", "LOSE", false, "", "NONE"))
                .isInstanceOf(AnalysisException.class);
    }

    @Test
    void generate_parserRejectsResponse_fallsBackToNext() throws Exception {
        FakePrimaryGateway primary = new FakePrimaryGateway();
        FakeFallbackGateway fallback = new FakeFallbackGateway();
        fallback.rawResponse = "raw2";
        doReturn(List.of(handle(primary), handle(fallback))).when(gatewaysInstance).handles();
        doReturn("prompt").when(promptBuilder)
                .build(PRODUCTS, "sug", "LOSE", false, "", "NONE");
        doThrow(new AnalysisException("bad", null)).when(resultParser).parse("raw");
        doReturn(sampleList()).when(resultParser).parse("raw2");

        ShoppingList result = useCase.generate(PRODUCTS, "sug", "LOSE", false, "", "NONE");

        assertThat(result.categories()).isNotEmpty();
    }
}
