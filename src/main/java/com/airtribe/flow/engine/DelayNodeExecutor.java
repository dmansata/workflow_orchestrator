package com.airtribe.flow.engine;

import com.airtribe.flow.engine.model.WorkflowNode;
import com.airtribe.flow.model.WorkflowExecution;
import org.springframework.stereotype.Component;

@Component
public class DelayNodeExecutor implements NodeExecutor {

    @Override
    public boolean supports(String type) {
        return "DELAY".equalsIgnoreCase(type);
    }

    @Override
    public String execute(WorkflowNode node, WorkflowExecution executionContext) {
        Integer seconds = (Integer) node.getConfig().getOrDefault("seconds", 1);
        
        System.out.println("⏳ Delaying workflow for " + seconds + " seconds...");
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Delay was interrupted", e);
        }
        
        return node.getNextNodeId();
    }
}
