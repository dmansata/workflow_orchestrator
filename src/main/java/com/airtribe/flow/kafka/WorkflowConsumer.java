package com.airtribe.flow.kafka;

import com.airtribe.flow.engine.ExecutionEngineService;
import com.airtribe.flow.model.Workflow;
import com.airtribe.flow.model.WorkflowExecution;
import com.airtribe.flow.repository.WorkflowExecutionRepository;
import com.airtribe.flow.repository.WorkflowRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class WorkflowConsumer {

    @Autowired
    private ExecutionEngineService executionEngine;

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private WorkflowExecutionRepository executionRepository;

    @KafkaListener(topics = "workflow-execution-events", groupId = "flow-execution-group")
    public void consume(WorkflowExecutionEvent event) {
        System.out.println("📥 Received event from Kafka to start execution ID: " + event.getExecutionId());

        Optional<Workflow> workflowOpt = workflowRepository.findById(event.getWorkflowId());
        Optional<WorkflowExecution> executionOpt = executionRepository.findById(event.getExecutionId());

        if (workflowOpt.isPresent() && executionOpt.isPresent()) {
            // Let the Execution Engine do its magic!
            executionEngine.runWorkflow(workflowOpt.get(), executionOpt.get());
        } else {
            System.err.println("❌ Could not find Workflow or Execution record in database!");
        }
    }
}
