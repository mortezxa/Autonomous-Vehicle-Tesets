package autopilot.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class SteeringControllerTest {

    private SteeringController steeringController;

    @BeforeEach
    void setUp() {
        steeringController = new SteeringController();
    }

    @ParameterizedTest
    @ValueSource(doubles = {Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NaN})
    void update_InvalidAngle_ShouldThrowIllegalArgumentException(double invalidAngle) {
        assertThrows(IllegalArgumentException.class, () -> steeringController.update(invalidAngle, 50.0));
    }

    @ParameterizedTest
    @ValueSource(doubles = {-10.0, Double.POSITIVE_INFINITY, Double.NaN})
    void update_InvalidSpeed_ShouldThrowIllegalArgumentException(double invalidSpeed) {
        assertThrows(IllegalArgumentException.class, () -> steeringController.update(30.0, invalidSpeed));
    }

    @ParameterizedTest
    @CsvSource({
            "30.0, 30.0",
            "-45.0, -45.0",
            "60.0, 60.0"
    })
    void update_AngleWithinTickLimit_ShouldReachTargetImmediately(double requestedAngle, double expectedAngle) {
        double result = steeringController.update(requestedAngle, 50.0);
        assertEquals(expectedAngle, result);
        assertEquals(expectedAngle, steeringController.getCurrentAngleDegrees());
    }

    @Test
    void update_AngleExceedsTickLimit_ShouldMoveByMaxChangePerTick() {
        double result = steeringController.update(100.0, 50.0);
        assertEquals(60.0, result);
        assertEquals(100.0, steeringController.getTargetAngleDegrees());

        result = steeringController.update(100.0, 50.0);
        assertEquals(100.0, result);
    }

    @Test
    void update_HighSpeed_ShouldClampToHighSpeedLimit() {
        double result1 = steeringController.update(150.0, 130.0);
        assertEquals(60.0, result1);
        assertEquals(90.0, steeringController.getTargetAngleDegrees());

        double result2 = steeringController.update(150.0, 130.0);
        assertEquals(90.0, result2);
    }

    @Test
    void update_LowSpeed_ShouldClampToPhysicalLimit() {
        steeringController.update(500.0, 50.0);
        assertEquals(450.0, steeringController.getTargetAngleDegrees());
    }

    @Test
    void selfCenter_ShouldMoveTowardZeroByMaxChangePerTick() {
        steeringController.update(100.0, 50.0);
        steeringController.update(100.0, 50.0);
        assertEquals(100.0, steeringController.getCurrentAngleDegrees());

        double result1 = steeringController.selfCenter();
        assertEquals(40.0, result1);
        assertEquals(0.0, steeringController.getTargetAngleDegrees());

        double result2 = steeringController.selfCenter();
        assertEquals(0.0, result2);
    }
}