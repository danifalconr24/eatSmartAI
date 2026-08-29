package com.eatsmart.architecture;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.eatsmart", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final String DOMAIN = "com.eatsmart.domain..";
    private static final String APPLICATION = "com.eatsmart.application..";
    private static final String INFRASTRUCTURE = "com.eatsmart.infrastructure..";
    private static final String REST = "com.eatsmart.infrastructure.rest..";
    private static final String GEMINI = "com.eatsmart.infrastructure.gemini..";
    private static final String OPENROUTER = "com.eatsmart.infrastructure.openrouter..";

    @ArchTest
    static final ArchRule domain_does_not_depend_on_application =
            noClasses().that().resideInAPackage(DOMAIN)
                    .should().dependOnClassesThat().resideInAPackage(APPLICATION)
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule domain_does_not_depend_on_infrastructure =
            noClasses().that().resideInAPackage(DOMAIN)
                    .should().dependOnClassesThat().resideInAPackage(INFRASTRUCTURE)
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule application_does_not_depend_on_infrastructure =
            noClasses().that().resideInAPackage(APPLICATION)
                    .should().dependOnClassesThat().resideInAPackage(INFRASTRUCTURE)
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule rest_does_not_depend_on_gemini =
            noClasses().that().resideInAPackage(REST)
                    .should().dependOnClassesThat().resideInAPackage(GEMINI)
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule rest_does_not_depend_on_openrouter =
            noClasses().that().resideInAPackage(REST)
                    .should().dependOnClassesThat().resideInAPackage(OPENROUTER)
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule gemini_does_not_depend_on_openrouter =
            noClasses().that().resideInAPackage(GEMINI)
                    .should().dependOnClassesThat().resideInAPackage(OPENROUTER)
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule openrouter_does_not_depend_on_gemini =
            noClasses().that().resideInAPackage(OPENROUTER)
                    .should().dependOnClassesThat().resideInAPackage(GEMINI)
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule application_port_interfaces_have_noImplementationDependencies =
            classes().that().resideInAPackage("com.eatsmart.application.port..")
                    .should().onlyDependOnClassesThat()
                    .resideInAnyPackage("com.eatsmart.domain..", "java..", "jakarta..")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule domain_model_records_have_noImplementationDependencies =
            classes().that().resideInAPackage("com.eatsmart.domain.model..")
                    .should().onlyDependOnClassesThat()
                    .resideInAnyPackage("com.eatsmart.domain..", "java..")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule domain_exceptions_have_noImplementationDependencies =
            classes().that().resideInAPackage("com.eatsmart.domain.exception..")
                    .should().onlyDependOnClassesThat()
                    .resideInAnyPackage("java..")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule gemini_gateway_implements_all_analysis_gateway_ports =
            classes().that().haveSimpleName("GeminiGateway")
                    .should().implement("com.eatsmart.application.port.ReceiptAnalysisGateway")
                    .andShould().implement("com.eatsmart.application.port.ProductAnalysisGateway")
                    .andShould().implement("com.eatsmart.application.port.ShoppingListGenerationGateway")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule openrouter_gateway_implements_all_analysis_gateway_ports =
            classes().that().haveSimpleName("OpenRouterGateway")
                    .should().implement("com.eatsmart.application.port.ReceiptAnalysisGateway")
                    .andShould().implement("com.eatsmart.application.port.ProductAnalysisGateway")
                    .andShould().implement("com.eatsmart.application.port.ShoppingListGenerationGateway")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule infrastructure_classes_should_be_application_scoped =
            classes().that().resideInAPackage(INFRASTRUCTURE)
                    .and().areNotInterfaces()
                    .and().haveSimpleNameNotContaining("ErrorResponse")
                    .should().beAnnotatedWith("jakarta.enterprise.context.ApplicationScoped")
                    .orShould().beAnnotatedWith("jakarta.ws.rs.Path")
                    .allowEmptyShould(true)
                    .as("Infrastructure beans should be @ApplicationScoped or JAX-RS resources");

    @ArchTest
    static final ArchRule application_classes_should_be_application_scoped =
            classes().that().resideInAPackage(APPLICATION)
                    .and().areNotInterfaces()
                    .should().beAnnotatedWith("jakarta.enterprise.context.ApplicationScoped")
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule dto_records_should_not_be_annotated_with_cdi_annotations =
            classes().that().resideInAPackage("com.eatsmart.infrastructure..dto..")
                    .should().notBeAnnotatedWith("jakarta.enterprise.context.ApplicationScoped")
                    .andShould().notBeAnnotatedWith("jakarta.inject.Inject")
                    .allowEmptyShould(true);
}
