package com.airtribe.flow.kafka;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class WorkflowProducer {

    private static final String TOPIC = "workflow-execution-events";

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    public void sendExecutionEvent(WorkflowExecutionEvent event) {
        System.out.println("📤 Publishing event to Kafka: Workflow ID " + event.getWorkflowId());
        kafkaTemplate.send(TOPIC, String.valueOf(event.getWorkflowId()), event);
    }

    public void sendToDLQ(WorkflowExecutionEvent event) {
        System.out.println("🚨 Sending event to Dead Letter Queue (DLQ): Workflow ID " + event.getWorkflowId());
        kafkaTemplate.send("workflow-dlq", String.valueOf(event.getWorkflowId()), event);
    }
}
