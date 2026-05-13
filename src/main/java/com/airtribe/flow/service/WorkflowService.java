package com.airtribe.flow.service;

import com.airtribe.flow.kafka.WorkflowExecutionEvent;
import com.airtribe.flow.kafka.WorkflowProducer;
import com.airtribe.flow.model.ExecutionStatus;
import com.airtribe.flow.model.User;
import com.airtribe.flow.model.Workflow;
import com.airtribe.flow.model.WorkflowExecution;
import com.airtribe.flow.model.WorkflowStatus;
import com.airtribe.flow.repository.UserRepository;
import com.airtribe.flow.repository.WorkflowExecutionRepository;
import com.airtribe.flow.repository.WorkflowRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class WorkflowService {

    @Autowired
    private WorkflowRepository workflowRepository;
    
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WorkflowExecutionRepository executionRepository;

    @Autowired
    private WorkflowProducer workflowProducer;

    public Workflow createDraftWorkflow(Long userId, String name, String description, String initialNodesJson) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        Workflow workflow = Workflow.builder()
                .name(name)
                .description(description)
                .status(WorkflowStatus.DRAFT)
                .nodes(initialNodesJson)
                .user(user)
                .webhookSecret(UUID.randomUUID().toString())
                .build();
        return workflowRepository.save(workflow);
    }

    public Workflow getWorkflow(Long id) {
        return workflowRepository.findById(id).orElseThrow(() -> new RuntimeException("Workflow not found"));
    }

    public List<Workflow> getUserWorkflows(Long userId) {
        return workflowRepository.findByUserId(userId);
    }

    public Workflow updateWorkflowNodes(Long id, String nodesJson) {
        Workflow workflow = getWorkflow(id);
        if (workflow.getStatus() == WorkflowStatus.PUBLISHED) {
            throw new RuntimeException("Cannot edit a published workflow");
        }
        workflow.setNodes(nodesJson);
        return workflowRepository.save(workflow);
    }

    public Workflow publishWorkflow(Long id) {
        Workflow workflow = getWorkflow(id);
        workflow.setStatus(WorkflowStatus.PUBLISHED);
        return workflowRepository.save(workflow);
    }

    public WorkflowExecution triggerWorkflow(Long workflowId, String initialContext) {
        Workflow workflow = getWorkflow(workflowId);
        
        if (workflow.getStatus() != WorkflowStatus.PUBLISHED) {
            throw new RuntimeException("Cannot execute a draft workflow. Please publish it first.");
        }

        // 1. Create a PENDING execution record
        WorkflowExecution execution = WorkflowExecution.builder()
                .workflow(workflow)
                .status(ExecutionStatus.PENDING)
                .contextData(initialContext)
                .build();
        execution = executionRepository.save(execution);

        // 2. Publish Event to Kafka
        WorkflowExecutionEvent event = new WorkflowExecutionEvent(workflow.getId(), execution.getId(), initialContext);
        workflowProducer.sendExecutionEvent(event);

        return execution;
    }

    public WorkflowExecution pauseExecution(Long executionId) {
        WorkflowExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new RuntimeException("Execution not found"));
        execution.setStatus(ExecutionStatus.PAUSED);
        return executionRepository.save(execution);
    }

    public WorkflowExecution resumeExecution(Long executionId) {
        WorkflowExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new RuntimeException("Execution not found"));
        
        if (execution.getStatus() != ExecutionStatus.PAUSED) {
            throw new RuntimeException("Only paused executions can be resumed");
        }

        execution.setStatus(ExecutionStatus.PENDING);
        execution = executionRepository.save(execution);

        // Re-publish to Kafka to pick it up again
        WorkflowExecutionEvent event = new WorkflowExecutionEvent(
                execution.getWorkflow().getId(), 
                execution.getId(), 
                execution.getContextData()
        );
        workflowProducer.sendExecutionEvent(event);

        return execution;
    }
}
