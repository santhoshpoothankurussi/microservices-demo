package com.sap.tamagotchi.service;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sap.tamagotchi.model.Care;
import com.sap.tamagotchi.model.Device;
import com.sap.tamagotchi.model.IoTMessage;
import com.sap.tamagotchi.publisher.PublisherService;

@Service
@EnableScheduling
public class TamagotchiService {

    public static final long DEVICE_EVENT_PROCESSOR_SCHEDULE = 5000;

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final Map<String, Device> deviceRegistry = Collections.synchronizedMap(new HashMap<>());

    private final PublisherService publisherService;

    @Autowired
    public TamagotchiService(PublisherService publisherService) {
        this.publisherService = publisherService;
    }

    public Device getDevice(String deviceId) {
        return deviceRegistry.get(deviceId);
    }

    public Collection<Device> getDevices() {
        return deviceRegistry.values();
    }

    public Set<String> getDeviceIds() {
        return deviceRegistry.keySet();
    }

    public Device createDevice(Device device) {
        deviceRegistry.put(device.getId(), device);
        return device;
    }

    public void takeCare(String deviceId, Care care) {
        Device device = deviceRegistry.get(deviceId);
        if (device == null) {
            return;
        }
        device.changeHealthScore(care.getFeed());
        device.changeHealthScore(care.getPet());
    }

    @Scheduled(fixedDelay = DEVICE_EVENT_PROCESSOR_SCHEDULE)
    private void processDeviceEvents() {
        deviceRegistry
                .values()
                .parallelStream()
                .filter(Device::hasMessages)
                .forEach(device -> {
                    while (device.getMessages().peek() != null) {
                        try {
                            publisherService.publish(device.getMessages().poll());
                        } catch (Exception ex) {
                            LOGGER.error("processing device events failed: {}", ex.getMessage());
                        }
                    }
                });

        // remove dead devices
        deviceRegistry
                .values()
                .parallelStream()
                .filter(device -> !device.isAlive())
                .forEach(device -> {
                    deviceRegistry.remove(device.getId());
                    sendTamagotchiDefunctNotifiction(device.getId());
                });
    }

    private void sendTamagotchiDefunctNotifiction(String id) {

        Device device = deviceRegistry.get(id);

        IoTMessage m = new IoTMessage() {

            @JsonProperty("message")
            private String message = String.format("Tamagotchi %s of %s passed away", device.getOwner(), device.getOwner());

            @Override
            public String getTopic() {
                return "tamagotchi-defunct";
            }

            public String getMessage() {
                return message;
            }

        };

        try {
            publisherService.publish(m);
        } catch (Exception ex) {
            LOGGER.error("sendTamagotchiDefunctNotifiction failed: {}", ex.getMessage());
        }
    }

}
