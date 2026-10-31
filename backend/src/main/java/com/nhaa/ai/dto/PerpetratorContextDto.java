package com.nhaa.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class PerpetratorContextDto {

    private String relationshipToVictim = "UNKNOWN";
    private Boolean knownToVictim = false;
    private Boolean multiplePerpetrators = false;

    public PerpetratorContextDto() {}

    public PerpetratorContextDto(String relationshipToVictim, Boolean knownToVictim, Boolean multiplePerpetrators) {
        this.relationshipToVictim = relationshipToVictim != null ? relationshipToVictim : "UNKNOWN";
        this.knownToVictim = knownToVictim != null ? knownToVictim : false;
        this.multiplePerpetrators = multiplePerpetrators != null ? multiplePerpetrators : false;
    }

    public String getRelationshipToVictim() {
        return relationshipToVictim;
    }

    public void setRelationshipToVictim(String relationshipToVictim) {
        this.relationshipToVictim = relationshipToVictim;
    }

    public Boolean getKnownToVictim() {
        return knownToVictim;
    }

    public void setKnownToVictim(Boolean knownToVictim) {
        this.knownToVictim = knownToVictim != null ? knownToVictim : false;
    }

    public Boolean getMultiplePerpetrators() {
        return multiplePerpetrators;
    }

    public void setMultiplePerpetrators(Boolean multiplePerpetrators) {
        this.multiplePerpetrators = multiplePerpetrators != null ? multiplePerpetrators : false;
    }
}
