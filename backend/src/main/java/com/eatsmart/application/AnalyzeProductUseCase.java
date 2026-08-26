package com.eatsmart.application;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.jboss.logging.Logger;

import com.eatsmart.domain.exception.AnalysisException;
import com.eatsmart.domain.exception.UnreadableReceiptException;
import com.eatsmart.domain.model.ProductAnalyzeResponse;
import com.eatsmart.domain.port.ReceiptAnalysisGateway;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

/**
 * Use case: analyze a single supermarket product photo.
 *
 * Orchestrates the configured {@link ReceiptAnalysisGateway} providers in
 * {@link Priority} order, same failover strategy as
 * {@link AnalyzeReceiptUseCase}: technical failures fall through to the next
 * enabled provider; a valid business answer ("not a recognizable product")
 * is never retried.
 *
 * Business rule: a healthier alternative is only offered when the score is
 * below {@value #LOW_SCORE_THRESHOLD}.
 */
@ApplicationScoped
public class AnalyzeProductUseCase {

    static final int LOW_SCORE_THRESHOLD = 7;

    private static final Logger LOG = Logger.getLogger(AnalyzeProductUseCase.class);

    @Inject
    ProductPromptBuilder promptBuilder;

    @Inject
    ProductResultParser resultParser;

    @Inject
    Instance<ReceiptAnalysisGateway> gateways;

    public ProductAnalyzeResponse analyze(byte[] imageBytes, String mimeType,
            String goal, boolean budgetMatters, String allergies, String dietPreference)
            throws UnreadableReceiptException, AnalysisException {

        String prompt = promptBuilder.build(goal, budgetMatters, allergies, dietPreference);

        boolean anyEnabled = false;
        AnalysisException lastError = null;
        for (ReceiptAnalysisGateway gateway : gatewayByPriority()) {
            if (!gateway.isEnabled()) {
                LOG.debugf("Proveedor %s deshabilitado (sin configurar), se omite", gateway.name());
                continue;
            }
            anyEnabled = true;
            try {
                LOG.infof("Analizando producto con %s (%d bytes de imagen)", gateway.name(), imageBytes.length);
                String rawText = gateway.analyze(imageBytes, mimeType, prompt);
                return dropAlternativeIfHealthy(resultParser.parse(rawText));
            } catch (UnreadableReceiptException e) {
                throw e;
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

    private static ProductAnalyzeResponse dropAlternativeIfHealthy(ProductAnalyzeResponse response) {
        if (response.score() >= LOW_SCORE_THRESHOLD && response.alternative() != null) {
            return new ProductAnalyzeResponse(response.product(), response.score(), response.nutrition(), null);
        }
        return response;
    }

    private List<ReceiptAnalysisGateway> gatewayByPriority() {
        List<Instance.Handle<ReceiptAnalysisGateway>> handles = new ArrayList<>();
        gateways.handles().forEach(handles::add);
        handles.sort(Comparator.comparingInt(AnalyzeProductUseCase::priorityOf));
        return handles.stream().map(Instance.Handle::get).toList();
    }

    private static int priorityOf(Instance.Handle<ReceiptAnalysisGateway> handle) {
        return Optional.ofNullable(handle.getBean().getBeanClass().getAnnotation(Priority.class))
                .map(Priority::value)
                .orElse(Integer.MAX_VALUE);
    }
}
