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
                LOG.debugf("Proveedor %s deshabilitado (sin configurar), se omite", gateway.name());
                continue;
            }
            anyEnabled = true;
            try {
                LOG.infof("Respondiendo duda de chat con %s (%d mensajes previos)", gateway.name(), history.size());
                return gateway.chat(systemPrompt, history, question);
            } catch (AnalysisException e) {
                LOG.warnf(e, "Falló el proveedor %s, probando el siguiente", gateway.name());
                lastError = e;
            }
        }

        if (!anyEnabled) {
            LOG.error("No hay ningún proveedor de análisis configurado");
            throw new AnalysisException("El servicio de análisis no está configurado en el servidor.", null);
        }
        throw lastError;
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
