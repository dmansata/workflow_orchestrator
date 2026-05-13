package com.airtribe.flow.controller;

import com.airtribe.flow.model.Workflow;
import com.airtribe.flow.model.WorkflowExecution;
import com.airtribe.flow.repository.WorkflowExecutionRepository;
import com.airtribe.flow.service.WorkflowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workflows")
public class WorkflowController {

    @Autowired
    private WorkflowService workflowService;

    @Autowired
    private WorkflowExecutionRepository workflowExecutionRepository;

    private Long getCurrentUserId() {
        // Retrieve the authenticated userId that we injected via JwtAuthenticationFilter
        return (Long) org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication().getCredentials();
    }

    @PostMapping
    public ResponseEntity<Workflow> createWorkflow(@RequestBody WorkflowRequest request) {
        Workflow workflow = workflowService.createDraftWorkflow(
                getCurrentUserId(), 
                request.getName(), 
                request.getDescription(), 
                request.getNodes());
        return ResponseEntity.ok(workflow);
    }

    @GetMapping
    public ResponseEntity<List<Workflow>> getUserWorkflows() {
        return ResponseEntity.ok(workflowService.getUserWorkflows(getCurrentUserId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Workflow> getWorkflow(@PathVariable Long id) {
        return ResponseEntity.ok(workflowService.getWorkflow(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Workflow> updateWorkflow(@PathVariable Long id, @RequestBody WorkflowRequest request) {
        return ResponseEntity.ok(workflowService.updateWorkflowNodes(id, request.getNodes()));
    }

    @GetMapping("/{id}/executions")
    public ResponseEntity<List<WorkflowExecution>> getExecutionHistory(@PathVariable Long id) {
        return ResponseEntity.ok(workflowExecutionRepository.findByWorkflowIdOrderByCreatedAtDesc(id));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<Workflow> publishWorkflow(@PathVariable Long id) {
        return ResponseEntity.ok(workflowService.publishWorkflow(id));
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<WorkflowExecution> startWorkflow(@PathVariable Long id, @RequestBody(required = false) java.util.Map<String, Object> initialContext) {
        String contextJson = initialContext != null ? initialContext.toString() : null;
        WorkflowExecution execution = workflowService.triggerWorkflow(id, contextJson);
        return ResponseEntity.accepted().body(execution); // 202 Accepted because it runs asynchronously
    }
}
