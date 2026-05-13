package com.airtribe.flow.controller;

import lombok.Data;

@Data
public class WorkflowRequest {
    private String name;
    private String description;
    private String nodes;
}
