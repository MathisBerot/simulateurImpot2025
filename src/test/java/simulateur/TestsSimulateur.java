package test.java.simulateur;

import com.simulateur.AdaptateurSimulateur;
import com.simulateur.ICalculateurImpot;
import com.simulateur.SituationFamiliale;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TestsSimulateur {

    private static ICalculateurImpot simulateur;

    @BeforeAll
    public static void setUp() {
        simulateur = new AdaptateurSimulateur();
    }

    public static Stream<Arguments> donneesPartsFoyerFiscal() {
        return Stream.of(
                Arguments.of(30000, "CELIBATAIRE", 1, 0, false, 1.5),
                Arguments.of(40000, "MARIE", 2, 0, false, 3),
                Arguments.of(50000, "VEUF", 0, 1, true, 2),
                Arguments.of(45000, "DIVORCE", 3, 0, true, 3.5),
                Arguments.of(35000, "PACSE", 1, 1, false, 3)
        );
    }

    @DisplayName("Test des parts fiscales avec de nouvelles données")
    @ParameterizedTest
    @MethodSource("donneesPartsFoyerFiscal")
    public void testNouvellesParts(int revenu1, String situation, int enfants, int enfantsHandi, boolean parentIsole, double expectedParts) {
        simulateur.setRevenusNetDeclarant1(revenu1);
        simulateur.setRevenusNetDeclarant2(0);
        simulateur.setSituationFamiliale(SituationFamiliale.valueOf(situation));
        simulateur.setNbEnfantsACharge(enfants);
        simulateur.setNbEnfantsSituationHandicap(enfantsHandi);
        simulateur.setParentIsole(parentIsole);
        simulateur.calculImpotSurRevenuNet();
        assertEquals(expectedParts, simulateur.getNbPartsFoyerFiscal());
    }

    public static Stream<Arguments> donneesAbattementFoyerFiscal() {
        return Stream.of(
                Arguments.of(3000, "CELIBATAIRE", 0, 0, false, 495),     // < 495
                Arguments.of(15000, "MARIE", 0, 0, false, 1500),         // 10%
                Arguments.of(180000, "VEUF", 0, 0, false, 14171)         // > plafond
        );
    }

    @DisplayName("Test des abattements fiscaux")
    @ParameterizedTest
    @MethodSource("donneesAbattementFoyerFiscal")
    public void testNouveauxAbattements(int revenu1, String situation, int enfants, int enfantsHandi, boolean parentIsole, int expectedAbattement) {
        simulateur.setRevenusNetDeclarant1(revenu1);
        simulateur.setRevenusNetDeclarant2(0);
        simulateur.setSituationFamiliale(SituationFamiliale.valueOf(situation));
        simulateur.setNbEnfantsACharge(enfants);
        simulateur.setNbEnfantsSituationHandicap(enfantsHandi);
        simulateur.setParentIsole(parentIsole);
        simulateur.calculImpotSurRevenuNet();
        assertEquals(expectedAbattement, simulateur.getAbattement());
    }

    public static Stream<Arguments> donneesRevenusFoyerFiscal() {
        return Stream.of(
                Arguments.of(10000, "CELIBATAIRE", 0, 0, false, 0),       // tranche 0
                Arguments.of(22000, "CELIBATAIRE", 0, 0, false, 308),     // tranche 11%
                Arguments.of(42000, "MARIE", 0, 0, false, 2372),          // tranche 30%
                Arguments.of(85000, "DIVORCE", 0, 0, false, 13782),       // tranche 41%
                Arguments.of(160000, "VEUF", 0, 0, false, 46812)          // tranche 45%
        );
    }

    @DisplayName("Test de calcul d'impôt sur plusieurs tranches")
    @ParameterizedTest
    @MethodSource("donneesRevenusFoyerFiscal")
    public void testNouvelleImpositionTranches(int revenu, String situation, int enfants, int enfantsHandi, boolean parentIsole, int expectedImpot) {
        simulateur.setRevenusNetDeclarant1(revenu);
        simulateur.setRevenusNetDeclarant2(0);
        simulateur.setSituationFamiliale(SituationFamiliale.valueOf(situation));
        simulateur.setNbEnfantsACharge(enfants);
        simulateur.setNbEnfantsSituationHandicap(enfantsHandi);
        simulateur.setParentIsole(parentIsole);
        simulateur.calculImpotSurRevenuNet();
        assertEquals(expectedImpot, simulateur.getImpotSurRevenuNet());
    }

    public static Stream<Arguments> donneesRobustesse() {
        return Stream.of(
                Arguments.of(-100, 0, "CELIBATAIRE", 0, 0, false),     // revenu négatif
                Arguments.of(25000, 0, null, 1, 0, false),             // situation familiale null
                Arguments.of(30000, 0, "MARIE", -2, 0, false),         // enfants négatifs
                Arguments.of(45000, 0, "DIVORCE", 0, -1, true)         // enfants handicap négatif
        );
    }

    @DisplayName("Test de robustesse avec entrées invalides")
    @ParameterizedTest
    @MethodSource("donneesRobustesse")
    public void testRobustesseValeursInvalides(int revenu1, int revenu2, String situation, int enfants, int enfantsHandi, boolean parentIsole) {
        simulateur.setRevenusNetDeclarant1(revenu1);
        simulateur.setRevenusNetDeclarant2(revenu2);
        if (situation != null)
            simulateur.setSituationFamiliale(SituationFamiliale.valueOf(situation));
        else
            simulateur.setSituationFamiliale(null);
        simulateur.setNbEnfantsACharge(enfants);
        simulateur.setNbEnfantsSituationHandicap(enfantsHandi);
        simulateur.setParentIsole(parentIsole);
        assertThrows(IllegalArgumentException.class, () -> simulateur.calculImpotSurRevenuNet());
    }

    @DisplayName("Test de cas divers via fichier CSV")
    @ParameterizedTest(name = "revenu1={0}, revenu2={1}, situation={2}, enfants={3}, enfantsHandi={4}, parentIsole={5}, impotAttendu={6}")
    @CsvFileSource(resources = "/casImposition.csv", numLinesToSkip = 1)
    public void testAutresCasCSV(int revenu1, int revenu2, String situation, int enfants, int enfantsHandi, boolean parentIsole, int impotAttendu) {
        simulateur.setRevenusNetDeclarant1(revenu1);
        simulateur.setRevenusNetDeclarant2(revenu2);
        simulateur.setSituationFamiliale(SituationFamiliale.valueOf(situation));
        simulateur.setNbEnfantsACharge(enfants);
        simulateur.setNbEnfantsSituationHandicap(enfantsHandi);
        simulateur.setParentIsole(parentIsole);
        simulateur.calculImpotSurRevenuNet();
        assertEquals(impotAttendu, simulateur.getImpotSurRevenuNet());
    }
}