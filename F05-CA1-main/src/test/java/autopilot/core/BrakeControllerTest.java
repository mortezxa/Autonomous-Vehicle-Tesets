package autopilot.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class BrakeControllerTest {

    private BrakeController brakeController;

    @BeforeEach
    void setUp() {
        brakeController = new BrakeController();
    }

    @Test
    void apply_NullGear_ShouldThrowNullPointerException() {
        assertThrows(NullPointerException.class, () -> brakeController.apply(50.0, 50.0, null));
    }

    @Test
    void apply_ParkGear_ShouldThrowIllegalStateException() {
        assertThrows(IllegalStateException.class, () -> brakeController.apply(50.0, 50.0, GearBox.Gear.P));
    }

    @ParameterizedTest
    @ValueSource(doubles = {-1.0, Double.POSITIVE_INFINITY, Double.NaN})
    void apply_InvalidPressure_ShouldThrowIllegalArgumentException(double invalidPressure) {
        assertThrows(IllegalArgumentException.class, () -> brakeController.apply(invalidPressure, 50.0, GearBox.Gear.D));
    }

    @ParameterizedTest
    @ValueSource(doubles = {-5.0, Double.POSITIVE_INFINITY, Double.NaN})
    void apply_InvalidSpeed_ShouldThrowIllegalArgumentException(double invalidSpeed) {
        assertThrows(IllegalArgumentException.class, () -> brakeController.apply(50.0, invalidSpeed, GearBox.Gear.D));
    }

    @ParameterizedTest
    @EnumSource(value = GearBox.Gear.class, names = {"D", "N", "R"})
    void apply_NormalConditions_ShouldApplyExactPressure(GearBox.Gear gear) {
        double result = brakeController.apply(50.0, 20.0, gear);
        assertEquals(50.0, result);
        assertEquals(50.0, brakeController.getPressure());
    }

    @Test
    void apply_PressureExceedsMax_ShouldBoundToMaxPressure() {
        double result = brakeController.apply(120.0, 20.0, GearBox.Gear.D);
        assertEquals(100.0, result);
    }

    @Test
    void apply_AbsConditionsMet_ShouldAlternateAbsPulses() {
        double firstApply = brakeController.apply(90.0, 50.0, GearBox.Gear.D);
        assertEquals(90.0, firstApply);

        double secondApply = brakeController.apply(90.0, 50.0, GearBox.Gear.D);
        assertEquals(60.0, secondApply);

        double thirdApply = brakeController.apply(90.0, 50.0, GearBox.Gear.D);
        assertEquals(90.0, thirdApply);
    }

    @Test
    void emergencyBrake_NullGear_ShouldThrowNullPointerException() {
        assertThrows(NullPointerException.class, () -> brakeController.emergencyBrake(null));
    }

    @Test
    void emergencyBrake_ParkGear_ShouldThrowIllegalStateException() {
        assertThrows(IllegalStateException.class, () -> brakeController.emergencyBrake(GearBox.Gear.P));
    }

    @ParameterizedTest
    @EnumSource(value = GearBox.Gear.class, names = {"D", "N", "R"})
    void emergencyBrake_ValidGear_ShouldApplyMaxPressureAndActivateWarning(GearBox.Gear gear) {
        brakeController.emergencyBrake(gear);
        assertEquals(100.0, brakeController.getPressure());
        assertTrue(brakeController.isWarningActive());
    }
}