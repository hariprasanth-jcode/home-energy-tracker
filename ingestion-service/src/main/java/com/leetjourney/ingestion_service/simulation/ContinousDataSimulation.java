package com.leetjourney.ingestion_service.simulation;

import com.leetjourney.ingestion_service.dto.EnergyUsageDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Random;

@Slf4j
@Component
public class ContinousDataSimulation implements CommandLineRunner {

    private final RestTemplate restTemplate=new RestTemplate();

    private final Random random=new Random();

    @Value("${simulation.request-per-interval}")
    private int requestPerInterval;

    @Value("${simulation.endpoint}")
    private String ingestionEndpoint;
    @Override
    public void run(String... args) throws Exception {

        log.info("ContinousDataSimulation ... started");

    }

   // @Scheduled(fixedRateString = "${simulation.interval-ms}")
    public void sendMockData(){

        for(int i=0;i<requestPerInterval;i++){

            EnergyUsageDto dto = EnergyUsageDto.builder()
                    .deviceId(random.nextLong(1, 6))
                    .energyConsumed(Math.round(random.nextDouble(0.0, 2.0) * 100.0 / 100.0))
                    .timestamp(LocalDateTime.now()
                            .atZone(ZoneId.systemDefault()).toInstant())
                    .build();


            try{

                HttpHeaders headers=new HttpHeaders();

                headers.setContentType(MediaType.APPLICATION_JSON);

                HttpEntity<EnergyUsageDto> request = new HttpEntity<>(dto, headers);

                restTemplate.postForEntity(ingestionEndpoint,request,Void.class);

                log.info("Sent Mock data : {}",dto);
            } catch (Exception e) {
                log.error("failed to send data : {} ",e.getMessage());
            }
        }


    }
}
