package com.airtribe.flow.controller;

import com.airtribe.flow.model.WorkflowExecution;
import com.airtribe.flow.service.WorkflowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/executions")
public class ExecutionController {

    @Autowired
    private WorkflowService workflowService;

    @PostMapping("/{id}/pause")
    public ResponseEntity<WorkflowExecution> pauseExecution(@PathVariable Long id) {
        return ResponseEntity.ok(workflowService.pauseExecution(id));
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<WorkflowExecution> resumeExecution(@PathVariable Long id) {
        return ResponseEntity.ok(workflowService.resumeExecution(id));
    }
}
