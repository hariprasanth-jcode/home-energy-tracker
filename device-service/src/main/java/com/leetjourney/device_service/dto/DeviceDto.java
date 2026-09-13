package com.leetjourney.device_service.dto;

import com.leetjourney.device_service.model.DeviceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class DeviceDto {

    private Long id;
    private String name;
    private String location;
    private DeviceType deviceType;
    private Long userId;

}
