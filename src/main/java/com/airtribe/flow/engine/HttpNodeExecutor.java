package com.airtribe.flow.engine;

import com.airtribe.flow.engine.model.WorkflowNode;
import com.airtribe.flow.model.WorkflowExecution;
import org.springframework.stereotype.Component;

@Component
public class HttpNodeExecutor implements NodeExecutor {

    @Override
    public boolean supports(String type) {
        return "HTTP".equalsIgnoreCase(type);
    }

    @Override
    public String execute(WorkflowNode node, WorkflowExecution executionContext) {
        // Here we would use RestTemplate or WebClient to make an actual HTTP call
        String url = (String) node.getConfig().get("url");
        String method = (String) node.getConfig().get("method");
        
        System.out.println("🚀 Executing HTTP Node: " + method + " " + url);
        // Simulate HTTP response
        
        return node.getNextNodeId();
    }
}
