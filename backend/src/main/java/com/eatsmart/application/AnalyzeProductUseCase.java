package com.eatsmart.application;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.jboss.logging.Logger;

import com.eatsmart.domain.exception.AnalysisException;
import com.eatsmart.domain.exception.UnreadableReceiptException;
import com.eatsmart.domain.model.ProductAnalyzeResponse;
import com.eatsmart.application.port.ProductAnalysisGateway;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

/**
 * Use case: analyze a single supermarket product photo.
 *
 * Orchestrates the configured {@link ProductAnalysisGateway} providers in
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
    Instance<ProductAnalysisGateway> gateways;

    public ProductAnalyzeResponse analyze(byte[] imageBytes, String mimeType,
            String goal, boolean budgetMatters, String allergies, String dietPreference)
            throws UnreadableReceiptException, AnalysisException {

        String prompt = promptBuilder.build(goal, budgetMatters, allergies, dietPreference);

        boolean anyEnabled = false;
        AnalysisException lastError = null;
        for (ProductAnalysisGateway gateway : gatewayByPriority()) {
            if (!gateway.isEnabled()) {
                LOG.debugf("Provider %s disabled, skipping", gateway.name());
                continue;
            }
            anyEnabled = true;
            try {
                LOG.infof("Analyzing product with %s (%d image bytes)", gateway.name(), imageBytes.length);
                long start = System.nanoTime();
                String rawText = gateway.analyze(imageBytes, mimeType, prompt);
                ProductAnalyzeResponse result = dropAlternativeIfHealthy(resultParser.parse(rawText));
                long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
                LOG.infof("Product analyzed successfully with %s in %d ms (product=%s, score=%d, alternative=%b)",
                        gateway.name(), elapsedMs, result.product(), result.score(), result.alternative() != null);
                return result;
            } catch (UnreadableReceiptException e) {
                throw e;
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
                "No se pudo completar el análisis. Inténtalo de nuevo en unos minutos.", lastError);
    }

    private static ProductAnalyzeResponse dropAlternativeIfHealthy(ProductAnalyzeResponse response) {
        if (response.score() >= LOW_SCORE_THRESHOLD && response.alternative() != null) {
            return new ProductAnalyzeResponse(response.product(), response.score(), response.nutrition(), null);
        }
        return response;
    }

    private List<ProductAnalysisGateway> gatewayByPriority() {
        List<Instance.Handle<ProductAnalysisGateway>> handles = new ArrayList<>();
        gateways.handles().forEach(handles::add);
        handles.sort(Comparator.comparingInt(AnalyzeProductUseCase::priorityOf));
        return handles.stream().map(Instance.Handle::get).toList();
    }

    private static int priorityOf(Instance.Handle<ProductAnalysisGateway> handle) {
        return Optional.ofNullable(handle.getBean().getBeanClass().getAnnotation(Priority.class))
                .map(Priority::value)
                .orElse(Integer.MAX_VALUE);
    }
}
