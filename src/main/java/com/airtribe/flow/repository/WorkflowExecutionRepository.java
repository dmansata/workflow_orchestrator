package com.airtribe.flow.repository;

import com.airtribe.flow.model.WorkflowExecution;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkflowExecutionRepository extends JpaRepository<WorkflowExecution, Long> {
    List<WorkflowExecution> findByWorkflowId(Long workflowId);
    List<WorkflowExecution> findByWorkflowIdOrderByCreatedAtDesc(Long workflowId);
}
