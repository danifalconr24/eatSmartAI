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

import com.eatsmart.application.port.ChatGateway;
import com.eatsmart.domain.exception.AnalysisException;
import com.eatsmart.domain.model.ChatContext;
import com.eatsmart.domain.model.ChatMessage;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ChatWithNutritionistUseCaseTest {

    private static final ChatContext CONTEXT = new ChatContext(
            List.of("pan blanco"), "sugerencias", null, null, 5, "LOSE", false, "", "NONE");
    private static final List<ChatMessage> HISTORY = List.of(
            ChatMessage.user("¿Es bueno el pan?"), ChatMessage.assistant("Mejor integral."));

    @Mock
    ChatPromptBuilder promptBuilder;

    @Mock
    Instance<ChatGateway> gatewaysInstance;

    @InjectMocks
    ChatWithNutritionistUseCase useCase;

    @Priority(1)
    static class FakePrimaryGateway implements ChatGateway {
        boolean enabled = true;
        @Override public String name() { return "OpenRouter"; }
        @Override public boolean isEnabled() { return enabled; }
        @Override public String chat(String systemPrompt, List<ChatMessage> history, String question)
                throws AnalysisException {
            return "respuesta";
        }
    }

    @Priority(2)
    static class FakeFallbackGateway implements ChatGateway {
        boolean enabled = true;
        @Override public String name() { return "Gemini"; }
        @Override public boolean isEnabled() { return enabled; }
        @Override public String chat(String systemPrompt, List<ChatMessage> history, String question)
                throws AnalysisException {
            return "respuesta fallback";
        }
    }

    @SuppressWarnings("unchecked")
    private Instance.Handle<ChatGateway> handle(ChatGateway gw) {
        Instance.Handle<ChatGateway> h = org.mockito.Mockito.mock(Instance.Handle.class);
        Bean<ChatGateway> bean = org.mockito.Mockito.mock(Bean.class);
        doReturn(bean).when(h).getBean();
        doReturn((Class) gw.getClass()).when(bean).getBeanClass();
        doReturn(gw).when(h).get();
        return h;
    }

    @Test
    void chat_primarySucceeds_returnsAnswer() throws Exception {
        FakePrimaryGateway primary = new FakePrimaryGateway();
        doReturn(List.of(handle(primary))).when(gatewaysInstance).handles();
        doReturn("system prompt").when(promptBuilder).build(CONTEXT);

        String answer = useCase.chat(CONTEXT, HISTORY, "¿Qué mejorar?");

        assertThat(answer).isEqualTo("respuesta");
    }

    @Test
    void chat_primaryDisabled_skipsToNext() throws Exception {
        FakePrimaryGateway primary = new FakePrimaryGateway();
        primary.enabled = false;
        FakeFallbackGateway fallback = new FakeFallbackGateway();
        doReturn(List.of(handle(primary), handle(fallback))).when(gatewaysInstance).handles();
        doReturn("system prompt").when(promptBuilder).build(CONTEXT);

        String answer = useCase.chat(CONTEXT, HISTORY, "¿Qué mejorar?");

        assertThat(answer).isEqualTo("respuesta fallback");
    }

    @Test
    void chat_primaryFails_fallsBackToSecondary() throws Exception {
        FakePrimaryGateway primary = new FakePrimaryGateway() {
            @Override public String chat(String systemPrompt, List<ChatMessage> history, String question)
                    throws AnalysisException {
                throw new AnalysisException("fail", null);
            }
        };
        FakeFallbackGateway fallback = new FakeFallbackGateway();
        doReturn(List.of(handle(primary), handle(fallback))).when(gatewaysInstance).handles();
        doReturn("system prompt").when(promptBuilder).build(CONTEXT);

        String answer = useCase.chat(CONTEXT, HISTORY, "¿Qué mejorar?");

        assertThat(answer).isEqualTo("respuesta fallback");
    }

    @Test
    void chat_noEnabledProviders_throwsAnalysisException() {
        FakePrimaryGateway primary = new FakePrimaryGateway();
        primary.enabled = false;
        doReturn(List.of(handle(primary))).when(gatewaysInstance).handles();
        doReturn("system prompt").when(promptBuilder).build(CONTEXT);

        assertThatThrownBy(() -> useCase.chat(CONTEXT, HISTORY, "¿Qué mejorar?"))
                .isInstanceOf(AnalysisException.class)
                .hasMessageContaining("no está configurado");
    }

    @Test
    void chat_allProvidersFail_throwsLastAnalysisException() {
        FakePrimaryGateway primary = new FakePrimaryGateway() {
            @Override public String chat(String systemPrompt, List<ChatMessage> history, String question)
                    throws AnalysisException {
                throw new AnalysisException("fail", null);
            }
        };
        doReturn(List.of(handle(primary))).when(gatewaysInstance).handles();
        doReturn("system prompt").when(promptBuilder).build(CONTEXT);

        assertThatThrownBy(() -> useCase.chat(CONTEXT, HISTORY, "¿Qué mejorar?"))
                .isInstanceOf(AnalysisException.class)
                .hasMessageContaining("fail");
    }
}
