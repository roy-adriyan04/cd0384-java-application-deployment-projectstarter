package com.udacity.catpoint.service;

import com.udacity.catpoint.data.AlarmStatus;
import com.udacity.catpoint.data.ArmingStatus;
import com.udacity.catpoint.data.SecurityRepository;
import com.udacity.catpoint.data.Sensor;
import com.udacity.catpoint.data.SensorType;
import com.udacity.catpoint.imageservice.ImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {

    @Mock
    private SecurityRepository securityRepository;

    @Mock
    private ImageService imageService;

    private SecurityService securityService;

    @BeforeEach
    void setUp() {
        securityService = new SecurityService(securityRepository, imageService);
    }

    private Sensor createSensor(String name, boolean active) {
        Sensor sensor = new Sensor(name, SensorType.DOOR);
        sensor.setActive(active);
        return sensor;
    }

    // Req 1: Armed + inactive sensor activated → PENDING_ALARM
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void armedSystemAndSensorActivated_setsPendingAlarm(ArmingStatus armingStatus) {
        Sensor sensor = createSensor("Front Door", false);
        when(securityRepository.getArmingStatus()).thenReturn(armingStatus);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    // Req 2: Armed + sensor activated + already PENDING_ALARM → ALARM
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void armedSystemAndSensorActivatedWhilePending_setsAlarm(ArmingStatus armingStatus) {
        Sensor sensor = createSensor("Front Door", false);
        when(securityRepository.getArmingStatus()).thenReturn(armingStatus);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    // Req 3: PENDING_ALARM + all sensors inactive → NO_ALARM
    @Test
    void pendingAlarmAndAllSensorsInactive_returnsNoAlarm() {
        Sensor sensor = createSensor("Front Door", true);
        Set<Sensor> sensors = new HashSet<>();
        sensors.add(sensor);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(securityRepository.getSensors()).thenReturn(sensors);

        securityService.changeSensorActivationStatus(sensor, false);

        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    // Req 3 (partial): PENDING_ALARM but another sensor still active → stays PENDING
    @Test
    void pendingAlarmAndAnotherSensorStillActive_staysPending() {
        Sensor sensor1 = createSensor("Front Door", true);
        Sensor sensor2 = createSensor("Back Window", true);
        Set<Sensor> sensors = new HashSet<>();
        sensors.add(sensor1);
        sensors.add(sensor2);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(securityRepository.getSensors()).thenReturn(sensors);

        securityService.changeSensorActivationStatus(sensor1, false);

        verify(securityRepository, never()).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    // Req 4: ALARM active + sensor activated → no alarm change
    @Test
    void alarmActiveAndSensorActivated_noAlarmChange() {
        Sensor sensor = createSensor("Front Door", false);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository, never()).setAlarmStatus(any());
    }

    // Req 4: ALARM active + sensor deactivated → no alarm change
    @Test
    void alarmActiveAndSensorDeactivated_noAlarmChange() {
        Sensor sensor = createSensor("Front Door", true);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);

        securityService.changeSensorActivationStatus(sensor, false);

        verify(securityRepository, never()).setAlarmStatus(any());
    }

    // Req 5: Already active sensor activated again while PENDING → ALARM
    @Test
    void activeSensorActivatedAgainWhilePending_setsAlarm() {
        Sensor sensor = createSensor("Front Door", true);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    // Req 6: Already inactive sensor deactivated → no alarm change
    @Test
    void inactiveSensorDeactivated_noAlarmChange() {
        Sensor sensor = createSensor("Front Door", false);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);

        securityService.changeSensorActivationStatus(sensor, false);

        verify(securityRepository, never()).setAlarmStatus(any());
    }

    // Req 7: Cat detected while ARMED_HOME → ALARM
    @Test
    void catDetectedWhileArmedHome_setsAlarm() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);

        securityService.processImage(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB));

        verify(securityRepository).setAlarmStatus(AlarmStatus.ALARM);
    }

    // Req 8: No cat + no active sensors → NO_ALARM
    @Test
    void noCatAndNoActiveSensors_setsNoAlarm() {
        Set<Sensor> sensors = new HashSet<>();
        sensors.add(createSensor("Front Door", false));
        when(securityRepository.getSensors()).thenReturn(sensors);
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(false);

        securityService.processImage(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB));

        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    // Req 8 (negative): No cat but sensors active → do NOT set NO_ALARM
    @Test
    void noCatButSensorsActive_doesNotSetNoAlarm() {
        Set<Sensor> sensors = new HashSet<>();
        sensors.add(createSensor("Front Door", true));
        when(securityRepository.getSensors()).thenReturn(sensors);
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(false);

        securityService.processImage(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB));

        verify(securityRepository, never()).setAlarmStatus(any());
    }

    // Req 9: System disarmed → NO_ALARM
    @Test
    void systemDisarmed_setsNoAlarm() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);

        verify(securityRepository).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    // Req 10: System armed → all sensors reset to inactive
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void systemArmed_resetsAllSensorsToInactive(ArmingStatus armingStatus) {
        Sensor sensor1 = createSensor("Front Door", true);
        Sensor sensor2 = createSensor("Back Window", true);
        Set<Sensor> sensors = new HashSet<>();
        sensors.add(sensor1);
        sensors.add(sensor2);
        when(securityRepository.getSensors()).thenReturn(sensors);

        securityService.setArmingStatus(armingStatus);

        assertFalse(sensor1.getActive());
        assertFalse(sensor2.getActive());
    }

    // Req 11: Armed-home while camera already shows cat → ALARM
    @Test
    void armedHomeWhileCatAlreadyDetected_setsAlarm() {
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        securityService.processImage(new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB));

        when(securityRepository.getSensors()).thenReturn(new HashSet<>());
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);

        verify(securityRepository, atLeastOnce()).setAlarmStatus(AlarmStatus.ALARM);
    }
}