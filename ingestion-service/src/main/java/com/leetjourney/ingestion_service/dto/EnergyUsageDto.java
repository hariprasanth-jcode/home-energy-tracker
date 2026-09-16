package com.leetjourney.ingestion_service.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Builder
public record EnergyUsageDto (

    Long deviceId,
    double energyConsumed,
    @JsonFormat(shape=JsonFormat.Shape.STRING)
    Instant timestamp
)
{}
