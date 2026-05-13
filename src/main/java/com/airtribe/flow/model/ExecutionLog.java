package com.airtribe.flow.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "execution_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExecutionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "execution_id", nullable = false)
    private WorkflowExecution execution;

    @Column(nullable = false)
    private String nodeId; // The ID of the node within the JSONB array

    @Column(nullable = false)
    private String nodeType; // HTTP, CONDITION, DELAY, NOTIFY

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NodeStatus status;

    @Column(columnDefinition = "text")
    private String inputSummary;

    @Column(columnDefinition = "text")
    private String outputSummary;

    private Long durationMs;

    @CreationTimestamp
    private LocalDateTime executedAt;

    public enum NodeStatus {
        SUCCESS,
        FAILED,
        SKIPPED
    }
}
