package com.airtribe.flow.engine;

import com.airtribe.flow.engine.model.WorkflowNode;
import com.airtribe.flow.model.WorkflowExecution;
import org.springframework.stereotype.Component;

@Component
public class ConditionNodeExecutor implements NodeExecutor {

    @Override
    public boolean supports(String type) {
        return "CONDITION".equalsIgnoreCase(type);
    }

    @Override
    public String execute(WorkflowNode node, WorkflowExecution executionContext) {
        System.out.println("🔀 Evaluating Condition Node");
        
        // Example structure: {"field": "$.user.age", "operator": ">", "value": 18}
        String fieldPath = (String) node.getConfig().get("field");
        String operator = (String) node.getConfig().get("operator");
        Object expectedValue = node.getConfig().get("value");
        
        // In a real system, you'd extract the field from the executionContext.getContextData()
        // For demonstration, let's pretend the condition is always TRUE.
        boolean conditionMet = true; 
        
        if (conditionMet) {
            System.out.println("✅ Condition met! Going to true path.");
            return node.getTrueNodeId();
        } else {
            System.out.println("❌ Condition failed! Going to false path.");
            return node.getFalseNodeId();
        }
    }
}
