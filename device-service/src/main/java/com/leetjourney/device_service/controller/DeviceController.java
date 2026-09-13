package com.leetjourney.device_service.controller;

import com.leetjourney.device_service.dto.DeviceDto;
import com.leetjourney.device_service.entity.Device;
import com.leetjourney.device_service.service.DeviceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/device")
public class DeviceController {
    private DeviceService deviceService;

    public DeviceController(DeviceService deviceService){
        this.deviceService=deviceService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<DeviceDto> getDeviceById(@PathVariable Long id){

        DeviceDto device = deviceService.getDeviceById(id);
        return ResponseEntity.ok(device);
    }

    @PostMapping
    public ResponseEntity<DeviceDto> createDevice(@RequestBody DeviceDto input){
        DeviceDto device = deviceService.createDevice(input);
        return ResponseEntity.ok(device);
    }

    @PutMapping("/{id}")
    public ResponseEntity<DeviceDto> updateDevice(@PathVariable Long id,@RequestBody DeviceDto deviceDto){
        DeviceDto device = deviceService.updateDevice(id, deviceDto);
        return ResponseEntity.ok(device);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deleteDeviceById(@PathVariable Long id){

        try {
            deviceService.deleteDevice(id);
            return ResponseEntity.ok("Device deleted succesfully");
        } catch (Exception e) {
            return new ResponseEntity<>("Device not found", HttpStatus.NOT_FOUND);
        }
    }
}
