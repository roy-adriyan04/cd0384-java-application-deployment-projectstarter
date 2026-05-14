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

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @Test
    void armingSystemAndActivatingSensorSetsPendingAlarm() {

        Sensor sensor = new Sensor("Front Door", SensorType.DOOR);

        when(securityRepository.getArmingStatus())
                .thenReturn(ArmingStatus.ARMED_HOME);

        when(securityRepository.getAlarmStatus())
                .thenReturn(AlarmStatus.NO_ALARM);

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository)
                .setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    @Test
    void pendingAlarmAndSensorActivatedAgainSetsAlarm() {

        Sensor sensor = new Sensor("Front Door", SensorType.DOOR);

        when(securityRepository.getArmingStatus())
                .thenReturn(ArmingStatus.ARMED_HOME);

        when(securityRepository.getAlarmStatus())
                .thenReturn(AlarmStatus.PENDING_ALARM);

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository)
                .setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    void pendingAlarmAndSensorDeactivatedReturnsToNoAlarm() {

        Sensor sensor = new Sensor("Front Door", SensorType.DOOR);
        sensor.setActive(true);

        when(securityRepository.getAlarmStatus())
                .thenReturn(AlarmStatus.PENDING_ALARM);

        securityService.changeSensorActivationStatus(sensor, false);

        verify(securityRepository)
                .setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test
    void alarmStateDoesNotChangeWhenAlreadyAlarm() {

        Sensor sensor = new Sensor("Front Door", SensorType.DOOR);

        when(securityRepository.getAlarmStatus())
                .thenReturn(AlarmStatus.ALARM);

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository, never())
                .setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    @Test
    void activatingAlreadyActiveSensorWhilePendingSetsAlarm() {

        Sensor sensor = new Sensor("Front Door", SensorType.DOOR);
        sensor.setActive(true);

        when(securityRepository.getAlarmStatus())
                .thenReturn(AlarmStatus.PENDING_ALARM);

        securityService.changeSensorActivationStatus(sensor, true);

        verify(securityRepository)
                .setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    void deactivatingInactiveSensorDoesNothing() {

        Sensor sensor = new Sensor("Front Door", SensorType.DOOR);

        securityService.changeSensorActivationStatus(sensor, false);

        verify(securityRepository, never())
                .setAlarmStatus(any());
    }

    @Test
    void catDetectedWhileArmedHomeSetsAlarm() {

        when(securityRepository.getArmingStatus())
                .thenReturn(ArmingStatus.ARMED_HOME);

        when(imageService.imageContainsCat(any(), anyFloat()))
                .thenReturn(true);

        securityService.processImage(null);

        verify(securityRepository)
                .setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    void noCatSetsNoAlarm() {

        when(imageService.imageContainsCat(any(), anyFloat()))
                .thenReturn(false);

        securityService.processImage(null);

        verify(securityRepository)
                .setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test
    void disarmingSystemSetsNoAlarm() {

        securityService.setArmingStatus(ArmingStatus.DISARMED);

        verify(securityRepository)
                .setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test
    void armingSystemResetsAllSensorsToInactive() {

        Sensor sensor = new Sensor("Front Door", SensorType.DOOR);
        sensor.setActive(true);

        Set<Sensor> sensors = new HashSet<>();
        sensors.add(sensor);

        when(securityRepository.getSensors())
                .thenReturn(sensors);

        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);

        assertFalse(sensor.getActive());
    }

    @Test
    void armedHomeWithCatSetsAlarm() {

        when(securityRepository.getArmingStatus())
                .thenReturn(ArmingStatus.ARMED_HOME);

        when(imageService.imageContainsCat(any(), anyFloat()))
                .thenReturn(true);

        securityService.processImage(null);

        verify(securityRepository)
                .setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    void noCatDoesNotClearAlarmWhenSensorsActive() {

        Sensor sensor = new Sensor("Front Door", SensorType.DOOR);
        sensor.setActive(true);

        Set<Sensor> sensors = new HashSet<>();
        sensors.add(sensor);

        when(securityRepository.getSensors())
                .thenReturn(sensors);

        when(imageService.imageContainsCat(any(), anyFloat()))
                .thenReturn(false);

        securityService.processImage(null);

        verify(securityRepository, never())
                .setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test
    void armingHomeWithExistingCatSetsAlarm() {

        when(imageService.imageContainsCat(any(), anyFloat()))
                .thenReturn(true);

        when(securityRepository.getArmingStatus())
                .thenReturn(ArmingStatus.ARMED_HOME);

        securityService.processImage(null);

        verify(securityRepository)
                .setAlarmStatus(AlarmStatus.ALARM);
    }
}