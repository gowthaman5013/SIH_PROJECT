package com.nhaa.ai.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_events")
public class AuditEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String caseId;
    private LocalDateTime timestamp;
    private String actor;
    private String role;
    private String action;
    private String previousState;
    private String newState;

    @Column(length = 2000)
    private String reason;

    private String correlationId;

    public AuditEventEntity() {}

    public AuditEventEntity(String caseId, String actor, String role, String action,
                            String previousState, String newState, String reason) {
        this.caseId = caseId;
        this.timestamp = LocalDateTime.now();
        this.actor = actor;
        this.role = role;
        this.action = action;
        this.previousState = previousState;
        this.newState = newState;
        this.reason = reason;
        this.correlationId = "REQ-" + System.currentTimeMillis();
    }

    public Long getId() { return id; }
    public String getCaseId() { return caseId; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getActor() { return actor; }
    public String getRole() { return role; }
    public String getAction() { return action; }
    public String getPreviousState() { return previousState; }
    public String getNewState() { return newState; }
    public String getReason() { return reason; }
    public String getCorrelationId() { return correlationId; }
}
