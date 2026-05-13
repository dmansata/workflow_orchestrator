package com.airtribe.flow.engine.model;

import lombok.Data;
import java.util.Map;

@Data
public class WorkflowNode {
    private String id;
    private String type; // e.g., HTTP, DELAY, CONDITION
    private Map<String, Object> config; 
    
    // For normal sequential flow
    private String nextNodeId; 
    
    // For branching (Condition Node)
    private String trueNodeId;
    private String falseNodeId;
}
