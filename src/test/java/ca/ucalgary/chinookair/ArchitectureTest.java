package ca.ucalgary.chinookair;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.dependencies.SliceAssignment;
import com.tngtech.archunit.library.dependencies.SliceIdentifier;
import java.util.List;

/**
 * The layer rules of the design report (package diagram, section G) as tests. If a class
 * breaks a rule, the build fails. The presentation and control packages may still be empty;
 * their rules then simply have nothing to check yet.
 */
@AnalyzeClasses(packages = "ca.ucalgary.chinookair", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    private static final List<String> LAYERS =
            List.of("presentation", "business.control", "business.entity", "data", "util");

    /** Puts every class into the layer of its package; the root package (AppContext, main) is left out. */
    private static final SliceAssignment BY_LAYER = new SliceAssignment() {
        @Override
        public String getDescription() {
            return "layer";
        }

        @Override
        public SliceIdentifier getIdentifierOf(JavaClass javaClass) {
            String pkg = javaClass.getPackageName();
            for (String layer : LAYERS) {
                String full = "ca.ucalgary.chinookair." + layer;
                if (pkg.equals(full) || pkg.startsWith(full + ".")) {
                    return SliceIdentifier.of(layer);
                }
            }
            return SliceIdentifier.ignore();
        }
    };

    // ---- presentation

    @ArchTest
    static final ArchRule PRESENTATION_DOES_NOT_USE_THE_DATA_LAYER = noClasses()
            .that().resideInAPackage("..presentation..")
            .should().dependOnClassesThat().resideInAPackage("..data..")
            .allowEmptyShould(true);

    // ---- entities

    @ArchTest
    static final ArchRule ENTITIES_DO_NOT_KNOW_THE_OTHER_LAYERS_OR_SWING_OR_JDBC = noClasses()
            .that().resideInAPackage("..business.entity..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "javax.swing..", "java.sql..", "..presentation..", "..data..", "..business.control..");

    // ---- controllers

    @ArchTest
    static final ArchRule CONTROLLERS_DO_NOT_KNOW_SWING_JDBC_OR_THE_SCREENS = noClasses()
            .that().resideInAPackage("..business.control..")
            .should().dependOnClassesThat().resideInAnyPackage("javax.swing..", "java.sql..", "..presentation..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule CONTROLLERS_NEVER_USE_A_SQLITE_CLASS = noClasses()
            .that().resideInAPackage("..business.control..")
            .should().dependOnClassesThat().haveSimpleNameStartingWith("Sqlite")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule NOTHING_OUTSIDE_THE_DATA_LAYER_USES_A_SQLITE_CLASS = noClasses()
            .that().resideOutsideOfPackage("..data..")
            .should().dependOnClassesThat().haveSimpleNameStartingWith("Sqlite");

    // ---- utilities

    @ArchTest
    static final ArchRule UTILITIES_DEPEND_ON_NO_LAYER = noClasses()
            .that().resideInAPackage("..util..")
            .should().dependOnClassesThat().resideInAnyPackage("..business..", "..data..", "..presentation..");

    // ---- no cycles

    @ArchTest
    static final ArchRule THERE_ARE_NO_CYCLES_BETWEEN_THE_LAYERS = slices()
            .assignedFrom(BY_LAYER)
            .should().beFreeOfCycles();

    // ---- visibility, as marked in the package diagram

    @ArchTest
    static final ArchRule SQLITE_DAO_CLASSES_ARE_NOT_PUBLIC = classes()
            .that().haveSimpleNameStartingWith("Sqlite").and().haveSimpleNameEndingWith("Dao")
            .should().notBePublic();

    @ArchTest
    static final ArchRule DAO_INTERFACES_ARE_PUBLIC = classes()
            .that().haveSimpleNameEndingWith("Dao").and().areInterfaces()
            .should().bePublic();

    @ArchTest
    static final ArchRule RESERVATION_STATES_AND_PAYMENT_STRATEGIES_ARE_NOT_PUBLIC = classes()
            .that().resideInAPackage("..business.entity..")
            .and().haveSimpleNameEndingWith("State")
            .or().haveSimpleName("CreditCardPayment")
            .or().haveSimpleName("DebitCardPayment")
            .should().notBePublic();
}
