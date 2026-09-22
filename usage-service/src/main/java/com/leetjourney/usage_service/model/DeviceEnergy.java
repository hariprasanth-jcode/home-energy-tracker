package com.leetjourney.usage_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class DeviceEnergy{

   private Long deviceId;
   private  double energyConsumed;
   private Long userId;
    }
