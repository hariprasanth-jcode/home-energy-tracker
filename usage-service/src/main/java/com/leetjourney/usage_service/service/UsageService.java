package com.leetjourney.usage_service.service;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.QueryApi;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;
import com.leetjourney.usage_service.client.DeviceClient;
import com.leetjourney.usage_service.client.UserClient;
import com.leetjourney.usage_service.dto.DeviceDto;
import com.leetjourney.usage_service.dto.UserDto;
import com.leetjourney.usage_service.kafka.event.AlertingEvent;
import com.leetjourney.usage_service.kafka.event.EnergyUsageEvent;
import com.leetjourney.usage_service.model.DeviceEnergy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UsageService {


    private InfluxDBClient  influxDBClient;

    private DeviceClient deviceClient;

    private UserClient userClient;

    private final KafkaTemplate<String,AlertingEvent>  kafkaTemplate;

    @Value("${influx.bucket}")
    private String influxBucket;

    @Value("${influx.org}")
    private String influxOrg;

    public UsageService(InfluxDBClient influxDBClient,DeviceClient deviceClient, UserClient userClient,
                        KafkaTemplate<String,AlertingEvent>  kafkaTemplate){
        this.influxDBClient = influxDBClient;
        this.deviceClient = deviceClient;
        this.userClient = userClient;
        this.kafkaTemplate = kafkaTemplate;
    }
    @KafkaListener(topics = "energy-usage", groupId = "${spring.kafka.consumer.group-id:usage-service}")
    public void energyUsageEvent(EnergyUsageEvent energyUsageEvent) {

        if (energyUsageEvent == null) {
            log.warn("Received null energy usage event (deserialization may have failed)");
            return;
        }

        log.info("Received energy usage event: {}", energyUsageEvent);

        java.time.Instant eventTime = energyUsageEvent.timestamp() != null 
                ? energyUsageEvent.timestamp() 
                : java.time.Instant.now();

        Point point=Point.measurement("energy_usage")
                .addTag("deviceId",String.valueOf(energyUsageEvent.deviceId()))
                .addField("energyConsumed", energyUsageEvent.energyConsumed())
                .time(eventTime, WritePrecision.MS);

        influxDBClient.getWriteApiBlocking().writePoint(influxBucket,influxOrg,point);

    }

    @Scheduled(cron ="*/10 * * * * *")
    public void aggregateDeviceEnergyUsage(){
        final Instant now=Instant.now();
        final Instant oneHourAgo=now.minusSeconds(3600);

        String fluxQuery = String.format("""
        from(bucket: "%s")
          |> range(start: time(v: "%s"), stop: time(v: "%s"))
          |> filter(fn: (r) => r["_measurement"] == "energy_usage")
          |> filter(fn: (r) => r["_field"] == "energyConsumed")
          |> group(columns: ["deviceId"])
          |> sum(column: "_value")
        """, influxBucket, oneHourAgo.toString(), now);


        QueryApi queryApi = influxDBClient.getQueryApi();
        List<FluxTable> tables=queryApi.query(fluxQuery,influxOrg);

       List<DeviceEnergy> deviceEnergies=new ArrayList();

       for(FluxTable table:tables){

           for(FluxRecord record:table.getRecords()){

            String deviceIdStr  =(String) record.getValueByKey("deviceId");
               Double energyConsumed = record
                       .getValueByKey("_value") instanceof Number ? ((Number)
                       record.getValueByKey("_value")).doubleValue() : 0.0;

               deviceEnergies.add(DeviceEnergy.builder()
                       .deviceId(Long.valueOf(deviceIdStr))
                       .energyConsumed(energyConsumed)
                       .build());
           }
       }
       log.info("Aggregated devie energies over the past hour : {}", deviceEnergies);

        // fetch the device from device service
        for(DeviceEnergy deviceEnergy:deviceEnergies){
            try {
                final DeviceDto deviceResponse=deviceClient.getDeviceById(deviceEnergy.getDeviceId());
                if(deviceResponse!=null && deviceResponse.id()!=null){
                    deviceEnergy.setUserId(deviceResponse.userId());
                } else {
                    log.warn("Device not found for ID : {}",deviceEnergy.getDeviceId());
                }
            } catch (Exception e) {
                log.warn("Failed to fetch device for ID : {} ", deviceEnergy.getDeviceId());
            }
        }

        // remove devices with null userId
        deviceEnergies.removeIf(de->de.getUserId()==null);

        // Get user-device mapping and aggregate per user
        Map<Long,List<DeviceEnergy>> userDeviceEnergyMap
                = deviceEnergies.stream()
                .collect(Collectors.groupingBy(DeviceEnergy::getUserId));

        log.info("User-Device Energy Map : {}",userDeviceEnergyMap);

        // get users energy consumption threshold
        List<Long> userIds=new ArrayList<>(userDeviceEnergyMap.keySet());

        final Map<Long,Double> userThresholdMap = new HashMap<>();
        final Map<Long, String> userEmailMap = new HashMap<>();

        for(final Long userId : userIds){
            try{
               UserDto user = userClient.getUserById(userId);

               if(user==null || user.id()==null || !user.alerting()){
                   log.warn("User not found or alerting disabled for ID : {}",userId);
                   continue;
               }

               userThresholdMap.put(userId,user.energyAlertingThreshold());
               userEmailMap.put(user.id(),user.email());
            } catch (Exception e) {
                log.warn("Failed to fetch user for ID : {} ",userId);
            }
        }
        log.info("User-Threshold Map : {}",userThresholdMap);

        // check the threshold against aggregated usage
        final List<Long> alertedUsers = new ArrayList<>(userThresholdMap.keySet());

        for(final Long userId:alertedUsers){
            final Double threshold = userThresholdMap.get(userId);
            final List<DeviceEnergy> devices = userDeviceEnergyMap.get(userId);

            final Double totalConsumption=devices.stream()
                    .mapToDouble(DeviceEnergy::getEnergyConsumed)
                    .sum();

            if(totalConsumption>threshold){
                log.info("ALERT : User ID {} has exceeded threshold! Total Consumption : {}, Threshold : {}",
                        userId,totalConsumption,threshold);

                // put message on kafka alert-topic
                final AlertingEvent alertingEvent=AlertingEvent.builder()
                        .userId(userId)
                        .message("Energy consumption threshold exceeded")
                        .threshold(threshold)
                        .energyConsumed(totalConsumption)
                        .email(userEmailMap.get(userId))
                        .build();

                kafkaTemplate.send("energy-alerts", String.valueOf(userId), alertingEvent)
                        .whenComplete((result, ex) -> {
                            if (ex != null) {
                                log.error("Failed to send alert event to Kafka: ", ex);
                            } else {
                                log.info("Successfully sent alert event to Kafka: offset={}", 
                                        result.getRecordMetadata().offset());
                            }
                        });

            }else {
                log.info("User ID {} is within the energy threshold . Total Consumption : {}, Threshold : {}",
                        userId,totalConsumption,threshold);
            }
        }
    }
}
