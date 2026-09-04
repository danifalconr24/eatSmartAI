package com.eatsmart.application;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.jboss.logging.Logger;

import com.eatsmart.domain.exception.AnalysisException;
import com.eatsmart.domain.exception.UnreadableReceiptException;
import com.eatsmart.domain.model.AnalyzeResponse;
import com.eatsmart.application.port.ReceiptAnalysisGateway;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

/**
 * Use case: analyze a supermarket receipt image.
 *
 * Orchestrates the configured {@link ReceiptAnalysisGateway} providers in
 * {@link Priority} order: the first enabled provider is tried, and technical
 * failures (including unparseable responses) fall back to the next one.
 * A valid business answer ("not a readable receipt") is never retried
 * against another provider.
 */
@ApplicationScoped
public class AnalyzeReceiptUseCase {

    private static final Logger LOG = Logger.getLogger(AnalyzeReceiptUseCase.class);

    @Inject
    ReceiptPromptBuilder promptBuilder;

    @Inject
    AnalysisResultParser resultParser;

    @Inject
    Instance<ReceiptAnalysisGateway> gateways;

    public AnalyzeResponse analyze(byte[] imageBytes, String mimeType,
            String goal, boolean budgetMatters, String allergies, String dietPreference)
            throws UnreadableReceiptException, AnalysisException {

        String prompt = promptBuilder.build(goal, budgetMatters, allergies, dietPreference);

        boolean anyEnabled = false;
        AnalysisException lastError = null;
        for (ReceiptAnalysisGateway gateway : gatewayByPriority()) {
            if (!gateway.isEnabled()) {
                LOG.debugf("Provider %s disabled, skipping", gateway.name());
                continue;
            }
            anyEnabled = true;
            try {
                LOG.infof("Analyzing receipt with %s (%d image bytes)", gateway.name(), imageBytes.length);
                long start = System.nanoTime();
                String rawText = gateway.analyze(imageBytes, mimeType, prompt);
                AnalyzeResponse result = resultParser.parse(rawText);
                long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
                LOG.infof("Receipt analyzed successfully with %s in %d ms (products=%d, score=%d)",
                        gateway.name(), elapsedMs, result.products().size(), result.score());
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
        // Generic user-facing message: provider details go to the log (chained cause),
        // never to the API response.
        throw new AnalysisException(
                "No se pudo completar el análisis. Inténtalo de nuevo en unos minutos.", lastError);
    }

    private List<ReceiptAnalysisGateway> gatewayByPriority() {
        List<Instance.Handle<ReceiptAnalysisGateway>> handles = new ArrayList<>();
        gateways.handles().forEach(handles::add);
        handles.sort(Comparator.comparingInt(AnalyzeReceiptUseCase::priorityOf));
        return handles.stream().map(Instance.Handle::get).toList();
    }

    private static int priorityOf(Instance.Handle<ReceiptAnalysisGateway> handle) {
        return Optional.ofNullable(handle.getBean().getBeanClass().getAnnotation(Priority.class))
                .map(Priority::value)
                .orElse(Integer.MAX_VALUE);
    }
}
