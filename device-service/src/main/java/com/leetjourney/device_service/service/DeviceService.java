package com.leetjourney.device_service.service;

import com.leetjourney.device_service.dto.DeviceDto;
import com.leetjourney.device_service.entity.Device;
import com.leetjourney.device_service.exception.DeviceNotFoundException;
import com.leetjourney.device_service.repository.DeviceRepository;
import org.springframework.stereotype.Service;

@Service
public class DeviceService {

    private DeviceRepository deviceRepository;

    public DeviceService(DeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    public DeviceDto getDeviceById(Long id){
     Device device=deviceRepository.findById(id)
                .orElseThrow(()->new IllegalArgumentException("Device not found with id "+id));

        return mapToDto(device);
    }

    private DeviceDto mapToDto(Device device){
        DeviceDto dto = new DeviceDto();
        dto.setId(device.getId());
        dto.setName(device.getName());
        dto.setDeviceType(device.getType());
        dto.setLocation(device.getLocation());
        dto.setUserId(device.getUserId());
        return dto;
    }

    public DeviceDto createDevice(DeviceDto input){
        Device device=Device.builder()
               .id(input.getId())
               .name(input.getName()).
               type(input.getDeviceType())
               .location(input.getLocation())
               .userId(input.getUserId())
               .build();

        deviceRepository.save(device);
        return mapToDto(device);
    }

    public DeviceDto updateDevice(Long id,DeviceDto deviceDto){

        Device device = deviceRepository
                .findById(id)
                .orElseThrow(() -> new DeviceNotFoundException("Device not found with id " + id));

        device.setName(deviceDto.getName());
        device.setType(deviceDto.getDeviceType());
        device.setLocation(deviceDto.getLocation());
        device.setUserId(deviceDto.getUserId());

        deviceRepository.save(device);

        return mapToDto(device);

    }

    public void deleteDevice(Long id){
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new DeviceNotFoundException("Device not found with id " + id));

        deviceRepository.delete(device);
    }

}
