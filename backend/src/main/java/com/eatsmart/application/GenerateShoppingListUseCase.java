package com.eatsmart.application;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.jboss.logging.Logger;

import com.eatsmart.domain.exception.AnalysisException;
import com.eatsmart.domain.model.ShoppingList;
import com.eatsmart.domain.port.ReceiptAnalysisGateway;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

/**
 * Use case: generate a suggested shopping list from a previous receipt
 * analysis (products + suggestions) and the user profile.
 *
 * Same orchestration as {@link AnalyzeReceiptUseCase}: gateways are tried in
 * {@link Priority} order, disabled providers are skipped and technical
 * failures ({@link AnalysisException}) fall back to the next one. There is no
 * image involved, so the text-only {@link ReceiptAnalysisGateway#generateText}
 * port method is used.
 */
@ApplicationScoped
public class GenerateShoppingListUseCase {

    private static final Logger LOG = Logger.getLogger(GenerateShoppingListUseCase.class);

    @Inject
    ShoppingListPromptBuilder promptBuilder;

    @Inject
    ShoppingListResultParser resultParser;

    @Inject
    Instance<ReceiptAnalysisGateway> gateways;

    public ShoppingList generate(List<String> products, String suggestions, String goal,
            boolean budgetMatters, String allergies, String dietPreference) throws AnalysisException {

        String prompt = promptBuilder.build(products, suggestions, goal, budgetMatters, allergies, dietPreference);

        boolean anyEnabled = false;
        AnalysisException lastError = null;
        for (ReceiptAnalysisGateway gateway : gatewayByPriority()) {
            if (!gateway.isEnabled()) {
                LOG.debugf("Proveedor %s deshabilitado (sin configurar), se omite", gateway.name());
                continue;
            }
            anyEnabled = true;
            try {
                LOG.infof("Generando lista de la compra con %s (%d productos)", gateway.name(), products.size());
                String rawText = gateway.generateText(prompt);
                return resultParser.parse(rawText);
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

    private List<ReceiptAnalysisGateway> gatewayByPriority() {
        List<Instance.Handle<ReceiptAnalysisGateway>> handles = new ArrayList<>();
        gateways.handles().forEach(handles::add);
        handles.sort(Comparator.comparingInt(GenerateShoppingListUseCase::priorityOf));
        return handles.stream().map(Instance.Handle::get).toList();
    }

    private static int priorityOf(Instance.Handle<ReceiptAnalysisGateway> handle) {
        return Optional.ofNullable(handle.getBean().getBeanClass().getAnnotation(Priority.class))
                .map(Priority::value)
                .orElse(Integer.MAX_VALUE);
    }
}
