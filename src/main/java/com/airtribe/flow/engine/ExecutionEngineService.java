package com.airtribe.flow.engine;

import com.airtribe.flow.engine.model.WorkflowNode;
import com.airtribe.flow.kafka.WorkflowExecutionEvent;
import com.airtribe.flow.kafka.WorkflowProducer;
import com.airtribe.flow.model.ExecutionLog;
import com.airtribe.flow.model.ExecutionStatus;
import com.airtribe.flow.model.Workflow;
import com.airtribe.flow.model.WorkflowExecution;
import com.airtribe.flow.repository.ExecutionLogRepository;
import com.airtribe.flow.repository.WorkflowExecutionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ExecutionEngineService {

    private static final int MAX_RETRIES = 3;

    @Autowired
    private WorkflowExecutionRepository executionRepository;
    
    @Autowired
    private ExecutionLogRepository logRepository;
    
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private List<NodeExecutor> executors;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private WorkflowProducer workflowProducer;

    public void runWorkflow(Workflow workflow, WorkflowExecution execution) {
        String lockKey = "lock:execution:" + execution.getId();
        
        // Try to acquire the lock for 5 minutes
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "LOCKED", 5, TimeUnit.MINUTES);
        
        if (acquired == null || !acquired) {
            System.out.println("⚠️ Could not acquire lock for execution " + execution.getId() + ". Skipping.");
            return;
        }

        try {
            execution.setStatus(ExecutionStatus.RUNNING);
            executionRepository.save(execution);

            // Parse JSONB array into Java Objects
            List<WorkflowNode> nodesList = objectMapper.readValue(workflow.getNodes(), new TypeReference<List<WorkflowNode>>() {});
            
            // Convert list to a Map so we can jump around nodes easily
            Map<String, WorkflowNode> nodesMap = nodesList.stream()
                    .collect(Collectors.toMap(WorkflowNode::getId, Function.identity()));

            // Find the starting point
            String currentNodeId = execution.getLastNodeId() != null ? execution.getLastNodeId() : nodesList.get(0).getId();

            while (currentNodeId != null) {
                // Check if we should pause
                execution = executionRepository.findById(execution.getId()).get();
                if (execution.getStatus() == ExecutionStatus.PAUSED) {
                    System.out.println("⏸️ Execution " + execution.getId() + " PAUSED at node " + currentNodeId);
                    return; // Exit and wait for resume
                }

                WorkflowNode node = nodesMap.get(currentNodeId);
                if (node == null) break;

                // Save current progress
                execution.setLastNodeId(currentNodeId);
                executionRepository.save(execution);

                NodeExecutor executor = executors.stream()
                        .filter(ex -> ex.supports(node.getType()))
                        .findFirst()
                        .orElseThrow(() -> new RuntimeException("No executor found for node type: " + node.getType()));

                boolean success = false;
                int attempts = 0;

                while (!success && attempts < MAX_RETRIES) {
                    attempts++;
                    long startTime = System.currentTimeMillis();
                    try {
                        if (attempts > 1) {
                            long backoff = (long) Math.pow(2, attempts - 1) * 1000;
                            System.out.println("⏳ Retrying node " + node.getId() + " (Attempt " + attempts + "/" + MAX_RETRIES + ") after " + backoff + "ms backoff...");
                            Thread.sleep(backoff);
                        }

                        currentNodeId = executor.execute(node, execution);
                        saveLog(execution, node, ExecutionLog.NodeStatus.SUCCESS, System.currentTimeMillis() - startTime);
                        success = true;
                    } catch (Exception e) {
                        System.err.println("❌ Node " + node.getId() + " failed (Attempt " + attempts + "/" + MAX_RETRIES + "): " + e.getMessage());
                        saveLog(execution, node, ExecutionLog.NodeStatus.FAILED, System.currentTimeMillis() - startTime);
                        
                        if (attempts >= MAX_RETRIES) {
                            throw new RuntimeException("Max retries exceeded for node " + node.getId(), e);
                        }
                    }
                }
            }

            execution.setLastNodeId(null); // Clear on completion
            execution.setStatus(ExecutionStatus.COMPLETED);
        } catch (Exception e) {
            execution.setStatus(ExecutionStatus.FAILED);
            System.err.println("🚨 Workflow Execution " + execution.getId() + " PERMANENTLY FAILED. Routing to DLQ.");
            
            // Route to DLQ
            workflowProducer.sendToDLQ(new WorkflowExecutionEvent(workflow.getId(), execution.getId(), null));
        } finally {
            executionRepository.save(execution);
            // Always release the lock when done
            redisTemplate.delete(lockKey);
        }
    }

    private void saveLog(WorkflowExecution execution, WorkflowNode node, ExecutionLog.NodeStatus status, long duration) {
        ExecutionLog log = ExecutionLog.builder()
                .execution(execution)
                .nodeId(node.getId())
                .nodeType(node.getType())
                .status(status)
                .durationMs(duration)
                .build();
        logRepository.save(log);
    }
}
