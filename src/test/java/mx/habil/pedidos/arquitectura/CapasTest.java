package mx.habil.pedidos.arquitectura;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * La frontera: quién puede conocer a quién.
 *
 * <p>El compilador no la vigila. Para Java, todas las clases de este proyecto pueden importarse
 * entre sí; si el cocinero importa al mesero, compila. Y Quarkus tampoco: CDI inyecta lo que se le
 * pida, venga de la capa que venga. Estas reglas leen las clases ya compiladas y fallan cuando una
 * flecha apunta hacia afuera.
 */
@AnalyzeClasses(packages = "mx.habil.pedidos", importOptions = ImportOption.DoNotIncludeTests.class)
class CapasTest {

    private static final String API = "mx.habil.pedidos.api..";
    private static final String FACADE = "mx.habil.pedidos.facade..";
    private static final String SERVICE = "mx.habil.pedidos.service..";
    private static final String REPOSITORY = "mx.habil.pedidos.model.repository..";
    private static final String CLIENT = "mx.habil.pedidos.client..";
    private static final String REQUEST = "mx.habil.pedidos.model.request..";
    private static final String RESPONSE = "mx.habil.pedidos.model.response..";
    private static final String ENTITY = "mx.habil.pedidos.model.entity..";
    private static final String VO = "mx.habil.pedidos.model.vo..";
    private static final String UTILS = "mx.habil.pedidos.utils..";

    /** Regla 2: las flechas solo hacia adentro. El cocinero nunca llama al mesero. */
    @ArchTest
    static final ArchRule lasFlechasSoloVanHaciaAdentro = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("api").definedBy(API)
            .layer("facade").definedBy(FACADE)
            .layer("service").definedBy(SERVICE)
            .layer("repository").definedBy(REPOSITORY)
            .layer("client").definedBy(CLIENT)
            .whereLayer("api").mayNotBeAccessedByAnyLayer()
            .whereLayer("facade").mayOnlyBeAccessedByLayers("api")
            .whereLayer("service").mayOnlyBeAccessedByLayers("api", "facade")
            .whereLayer("repository").mayOnlyBeAccessedByLayers("service")
            .whereLayer("client").mayOnlyBeAccessedByLayers("facade", "service")
            .because("las flechas solo van hacia adentro: api -> facade -> service -> repository o client");

    /** Regla 3, primera mitad: la comanda y el ticket no pasan de la facade. */
    @ArchTest
    static final ArchRule laCocinaNoConoceNiLaComandaNiElTicket = noClasses()
            .that().resideInAnyPackage(SERVICE, REPOSITORY, CLIENT, ENTITY, VO)
            .should().dependOnClassesThat().resideInAnyPackage(REQUEST, RESPONSE)
            .because("lo que entra y lo que sale son del mesero y del jefe de cocina; el cocinero trabaja con vo y entity");

    /**
     * Regla 3, segunda mitad: la ficha del archivo nunca sale por la puerta.
     *
     * <p>Se pregunta por dependencias, no por el tipo que devuelve un método: así también se ve una
     * entity escondida dentro de {@code List<PedidoEntity>} o de {@code RestResponse<PedidoEntity>}.
     */
    @ArchTest
    static final ArchRule elMeseroNoConoceLaFichaDelArchivo = noClasses()
            .that().resideInAPackage(API)
            .should().dependOnClassesThat().resideInAPackage(ENTITY)
            .because("lo que se guarda y lo que sale son cosas distintas: por la puerta sale el response, nunca la entity");

    /** El cajón de herramientas y las medidas exactas no conocen la cocina. */
    @ArchTest
    static final ArchRule elCajonYLasMedidasNoConocenLaCocina = noClasses()
            .that().resideInAnyPackage(UTILS, VO)
            .should().dependOnClassesThat().resideInAnyPackage(
                    API, FACADE, SERVICE, REPOSITORY, CLIENT, REQUEST, RESPONSE, ENTITY,
                    "mx.habil.pedidos.mapper..", "mx.habil.pedidos.config..",
                    "mx.habil.pedidos.exception..", "mx.habil.pedidos.observability..")
            .because("utils y vo se usan desde cualquier capa; si conocieran a una, arrastrarían a todas");
}
