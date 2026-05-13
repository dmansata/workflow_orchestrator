package com.airtribe.flow.engine;

import com.airtribe.flow.engine.model.WorkflowNode;
import com.airtribe.flow.model.WorkflowExecution;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Notify Node Executor.
 * Simulates sending notifications (Email/Slack).
 * In a real app, this would call an SMTP server or Slack Webhook.
 */
@Component
public class NotifyNodeExecutor implements NodeExecutor {

    @Override
    public boolean supports(String type) {
        return "NOTIFY".equalsIgnoreCase(type);
    }

    @Override
    public String execute(WorkflowNode node, WorkflowExecution execution) {
        Map<String, Object> config = node.getConfig();
        String channel = (String) config.getOrDefault("channel", "EMAIL");
        String message = (String) config.getOrDefault("message", "Workflow Notification");
        String recipient = (String) config.getOrDefault("recipient", "user@example.com");

        System.out.println("--------------------------------------------------");
        System.out.println("🔔 [NOTIFICATION SENT]");
        System.out.println("📬 Channel: " + channel);
        System.out.println("👤 Recipient: " + recipient);
        System.out.println("📝 Message: " + message);
        System.out.println("--------------------------------------------------");

        return node.getNextNodeId();
    }
}
