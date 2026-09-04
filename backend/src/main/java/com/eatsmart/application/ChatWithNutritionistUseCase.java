package com.eatsmart.application;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.jboss.logging.Logger;

import com.eatsmart.application.port.ChatGateway;
import com.eatsmart.domain.exception.AnalysisException;
import com.eatsmart.domain.model.ChatContext;
import com.eatsmart.domain.model.ChatMessage;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

/**
 * Use case: answer a user question about a previous analysis result
 * (receipt or product), keeping a multi-turn conversation.
 *
 * Same orchestration as {@link GenerateShoppingListUseCase}: gateways are
 * tried in {@link Priority} order, disabled providers are skipped and
 * technical failures ({@link AnalysisException}) fall back to the next one.
 * The chat answer is free text, so there is no business-failure path and no
 * result parser.
 */
@ApplicationScoped
public class ChatWithNutritionistUseCase {

    private static final Logger LOG = Logger.getLogger(ChatWithNutritionistUseCase.class);

    @Inject
    ChatPromptBuilder promptBuilder;

    @Inject
    Instance<ChatGateway> gateways;

    public String chat(ChatContext context, List<ChatMessage> history, String question)
            throws AnalysisException {

        String systemPrompt = promptBuilder.build(context);

        boolean anyEnabled = false;
        AnalysisException lastError = null;
        for (ChatGateway gateway : gatewayByPriority()) {
            if (!gateway.isEnabled()) {
                LOG.debugf("Provider %s disabled, skipping", gateway.name());
                continue;
            }
            anyEnabled = true;
            try {
                LOG.infof("Answering chat question with %s (%d previous messages)", gateway.name(), history.size());
                long start = System.nanoTime();
                String answer = gateway.chat(systemPrompt, history, question);
                long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
                LOG.infof("Chat answered successfully with %s in %d ms (answerLength=%d)",
                        gateway.name(), elapsedMs, answer.length());
                return answer;
            } catch (AnalysisException e) {
                LOG.warnf(e, "Provider %s failed, trying next", gateway.name());
                lastError = e;
            }
        }

        if (!anyEnabled) {
            LOG.error("No analysis provider configured");
            throw new AnalysisException("El servicio de análisis no está configurado en el servidor.", null);
        }
        throw new AnalysisException(
                "No se pudo obtener respuesta. Inténtalo de nuevo en unos minutos.", lastError);
    }

    private List<ChatGateway> gatewayByPriority() {
        List<Instance.Handle<ChatGateway>> handles = new ArrayList<>();
        gateways.handles().forEach(handles::add);
        handles.sort(Comparator.comparingInt(ChatWithNutritionistUseCase::priorityOf));
        return handles.stream().map(Instance.Handle::get).toList();
    }

    private static int priorityOf(Instance.Handle<ChatGateway> handle) {
        return Optional.ofNullable(handle.getBean().getBeanClass().getAnnotation(Priority.class))
                .map(Priority::value)
                .orElse(Integer.MAX_VALUE);
    }
}
