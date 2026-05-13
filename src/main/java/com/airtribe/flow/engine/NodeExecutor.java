package com.airtribe.flow.engine;

import com.airtribe.flow.engine.model.WorkflowNode;
import com.airtribe.flow.model.WorkflowExecution;

public interface NodeExecutor {
    // Tells the Engine if this class handles a specific node type (e.g. "HTTP")
    boolean supports(String type);

    // Executes the business logic and returns the ID of the next node to process
    String execute(WorkflowNode node, WorkflowExecution executionContext);
}
