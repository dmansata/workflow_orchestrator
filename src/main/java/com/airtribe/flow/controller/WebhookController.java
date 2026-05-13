package com.airtribe.flow.controller;

import com.airtribe.flow.model.Workflow;
import com.airtribe.flow.model.WorkflowExecution;
import com.airtribe.flow.service.WorkflowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Webhook Controller.
 * Handles external triggers for workflows.
 * Security is handled via a 'secret' query parameter rather than JWT.
 */
@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {

    @Autowired
    private WorkflowService workflowService;

    @PostMapping("/{id}")
    public ResponseEntity<?> triggerViaWebhook(
            @PathVariable Long id,
            @RequestParam String secret,
            @RequestBody(required = false) String payload) {

        Workflow workflow = workflowService.getWorkflow(id);

        // Validate the secret key
        if (workflow.getWebhookSecret() == null || !workflow.getWebhookSecret().equals(secret)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("{\"error\": \"Invalid or missing webhook secret\"}");
        }

        // Trigger the workflow
        WorkflowExecution execution = workflowService.triggerWorkflow(id, payload);
        
        return ResponseEntity.ok(execution);
    }
}
