package com.airtribe.flow.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowExecutionEvent {
    private Long workflowId;
    private Long executionId;
    // We could pass initial trigger data/variables here!
    private String initialContextJson; 
}
