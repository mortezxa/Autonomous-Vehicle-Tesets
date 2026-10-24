package autopilot.core;

import autopilot.exception.SensorFailureException;
import autopilot.sensor.LaneSensor;
import autopilot.sensor.ObstacleSensor;
import autopilot.sensor.SpeedSensor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class AutoDriverTest {

    private static final double TARGET_SPEED_KMH = 80.0;
    private static final double NORMAL_DISTANCE_METERS = 100.0;
    private static final double SAFE_MODE_BRAKE_PRESSURE = 30.0;

    private SpeedSensor defaultSpeedSensor;
    private ObstacleSensor defaultObstacleSensor;
    private LaneSensor defaultLaneSensor;

    @BeforeEach
    void setUp() {
        defaultSpeedSensor = () -> TARGET_SPEED_KMH;
        defaultObstacleSensor = () -> NORMAL_DISTANCE_METERS;
        defaultLaneSensor = () -> 0.0;
    }

    @Test
    void tick_UnderNormalConditions_ShouldNotEnterSafeMode() {
        AutoDriver autoDriver = new AutoDriver(defaultSpeedSensor, defaultObstacleSensor, defaultLaneSensor, TARGET_SPEED_KMH);
        
        AutoDriver.DriveResult result = autoDriver.tick(GearBox.Gear.D);

        assertNotNull(result);
        assertFalse(result.safeMode());
        assertEquals(0.0, result.brakePressure());
    }

    @ParameterizedTest
    @CsvSource({
            "1.0, -1.0",
            "-1.0, 1.0"
    })
    void tick_WhenLaneOffsetOccurs_ShouldSteerInOppositeDirection(double offset, double expectedSignMultiplier) {
        LaneSensor laneSensor = () -> offset;
        AutoDriver autoDriver = new AutoDriver(defaultSpeedSensor, defaultObstacleSensor, laneSensor, TARGET_SPEED_KMH);

        AutoDriver.DriveResult result = autoDriver.tick(GearBox.Gear.D);

        assertTrue(result.steeringAngleDegrees() * expectedSignMultiplier > 0.0);
    }

    static Stream<AutoDriver> provideFaultySensors() {
        SpeedSensor faultySpeedSensor = () -> { throw new SensorFailureException("Speed sensor failed"); };
        ObstacleSensor faultyObstacleSensor = () -> { throw new SensorFailureException("Obstacle sensor failed"); };
        
        return Stream.of(
                new AutoDriver(faultySpeedSensor, () -> NORMAL_DISTANCE_METERS, () -> 0.0, TARGET_SPEED_KMH),
                new AutoDriver(() -> TARGET_SPEED_KMH, faultyObstacleSensor, () -> 0.0, TARGET_SPEED_KMH)
        );
    }

    @ParameterizedTest
    @MethodSource("provideFaultySensors")
    void tick_WhenSensorFails_ShouldEnterSafeMode(AutoDriver faultyAutoDriver) {
        AutoDriver.DriveResult result = faultyAutoDriver.tick(GearBox.Gear.D);

        assertTrue(result.safeMode());
        assertEquals(SAFE_MODE_BRAKE_PRESSURE, result.brakePressure());
        assertEquals(AdaptiveCruise.Mode.BRAKE, result.cruiseMode());
    }
}
