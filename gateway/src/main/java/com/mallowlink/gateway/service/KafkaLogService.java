package com.mallowlink.gateway.service;

import com.mallowlink.gateway.model.InBoundLogginLogs;
import com.mallowlink.gateway.model.OutBoundLoggingLogs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaLogService {

    private static final String INBOUND_LOGS_TOPIC = "gateway-inbound-logs";
    private static final String OUTBOUND_LOGS_TOPIC = "gateway-outbound-logs";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void sendInboundLog(InBoundLogginLogs inboundLog) {
        try {
            kafkaTemplate.send(INBOUND_LOGS_TOPIC, inboundLog);
            log.debug("Sent inbound log to Kafka topic: {}", INBOUND_LOGS_TOPIC);
        } catch (Exception e) {
            log.error("Failed to send inbound log to Kafka", e);
        }
    }

    public void sendOutboundLog(OutBoundLoggingLogs outboundLog) {
        try {
            kafkaTemplate.send(OUTBOUND_LOGS_TOPIC, outboundLog);
            log.debug("Sent outbound log to Kafka topic: {}", OUTBOUND_LOGS_TOPIC);
        } catch (Exception e) {
            log.error("Failed to send outbound log to Kafka", e);
        }
    }
}