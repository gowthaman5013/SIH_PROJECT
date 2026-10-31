package com.nhaa.ai.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "active_logs")
public class ActiveLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String caseId;
    private LocalDateTime timestamp;
    private String stage;
    private String status; // COMPLETED, WARNING, IN_PROGRESS
    private String actorService;

    @Column(length = 2000)
    private String eventMessage;

    private String severity; // INFO, WARN, ERROR

    public ActiveLogEntity() {}

    public ActiveLogEntity(String caseId, String stage, String status, String actorService, String eventMessage, String severity) {
        this.caseId = caseId;
        this.timestamp = LocalDateTime.now();
        this.stage = stage;
        this.status = status;
        this.actorService = actorService;
        this.eventMessage = eventMessage;
        this.severity = severity != null ? severity : "INFO";
    }

    public Long getId() { return id; }
    public String getCaseId() { return caseId; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getStage() { return stage; }
    public String getStatus() { return status; }
    public String getActorService() { return actorService; }
    public String getEventMessage() { return eventMessage; }
    public String getSeverity() { return severity; }
}
