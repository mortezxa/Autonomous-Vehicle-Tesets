package autopilot.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvFileSource;

import static org.junit.jupiter.api.Assertions.*;

class AdaptiveCruiseTest {

    private AdaptiveCruise adaptiveCruise;

    @BeforeEach
    void setUp() {
        adaptiveCruise = new AdaptiveCruise(100.0);
    }

    @Test
    void safeDistanceFor_ValidSpeed_ShouldReturnCorrectDistance() {
        double result = adaptiveCruise.safeDistanceFor(80.0);
        assertEquals(40.0, result);
    }

    @Test
    void update_InvalidSpeed_ShouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> adaptiveCruise.update(-10.0, 50.0));
    }

    @Test
    void update_InvalidDistance_ShouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> adaptiveCruise.update(50.0, -10.0));
    }

    @Test
    void update_BrakingHysteresis_ShouldHoldBrakeUntilReleaseDistance() {
        AdaptiveCruise.Decision initialDecision = adaptiveCruise.update(100.0, 40.0);
        assertEquals(AdaptiveCruise.Mode.BRAKE, initialDecision.mode());

        AdaptiveCruise.Decision hysteresisDecision = adaptiveCruise.update(100.0, 55.0);
        assertEquals(AdaptiveCruise.Mode.BRAKE, hysteresisDecision.mode());
        assertEquals(10.0, hysteresisDecision.amount());

        AdaptiveCruise.Decision releaseDecision = adaptiveCruise.update(100.0, 65.0);
        assertNotEquals(AdaptiveCruise.Mode.BRAKE, releaseDecision.mode());
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/cruise_data.csv", numLinesToSkip = 1)
    void update_VariousConditions_ShouldReturnCorrectDecision(double speed, double distance, double targetSpeed, AdaptiveCruise.Mode expectedMode, double expectedAmount) {
        AdaptiveCruise cruise = new AdaptiveCruise(targetSpeed);
        AdaptiveCruise.Decision decision = cruise.update(speed, distance);

        assertEquals(expectedMode, decision.mode());
        assertEquals(expectedAmount, decision.amount(), 0.001);
    }
}