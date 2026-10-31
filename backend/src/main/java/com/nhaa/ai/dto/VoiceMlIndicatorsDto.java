package com.nhaa.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.HashMap;
import java.util.Map;

public class VoiceMlIndicatorsDto {

    @JsonProperty("stress")
    private Double stress;

    @JsonProperty("fear")
    private Double fear;

    @JsonProperty("anxiety")
    private Double anxiety;

    @JsonProperty("trauma_cue")
    private Double traumaCue;

    @JsonProperty("reduced_speaking_rate")
    private Double reducedSpeakingRate;

    @JsonProperty("acoustic_stress")
    private Double acousticStress;

    @JsonProperty("acoustic_distress")
    private Double acousticDistress;

    @JsonProperty("vocal_instability")
    private Double vocalInstability;

    public VoiceMlIndicatorsDto() {}

    public VoiceMlIndicatorsDto(Double stress, Double fear, Double anxiety, Double acousticDistress, Double vocalInstability) {
        this.stress = stress;
        this.fear = fear;
        this.anxiety = anxiety;
        this.acousticDistress = acousticDistress;
        this.vocalInstability = vocalInstability;
    }

    public VoiceMlIndicatorsDto(Double stress, Double fear, Double anxiety, Double traumaCue,
                                Double acousticStress, Double acousticDistress, Double vocalInstability) {
        this.stress = stress;
        this.fear = fear;
        this.anxiety = anxiety;
        this.traumaCue = traumaCue;
        this.reducedSpeakingRate = traumaCue;
        this.acousticStress = acousticStress;
        this.acousticDistress = acousticDistress;
        this.vocalInstability = vocalInstability;
    }

    public Double getStress() { return stress; }
    public void setStress(Double stress) { this.stress = stress; }

    public Double getFear() { return fear; }
    public void setFear(Double fear) { this.fear = fear; }

    public Double getAnxiety() { return anxiety; }
    public void setAnxiety(Double anxiety) { this.anxiety = anxiety; }

    public Double getTraumaCue() { return traumaCue != null ? traumaCue : reducedSpeakingRate; }
    public void setTraumaCue(Double traumaCue) { this.traumaCue = traumaCue; this.reducedSpeakingRate = traumaCue; }

    public Double getReducedSpeakingRate() { return reducedSpeakingRate != null ? reducedSpeakingRate : traumaCue; }
    public void setReducedSpeakingRate(Double r) { this.reducedSpeakingRate = r; this.traumaCue = r; }

    public Double getAcousticStress() { return acousticStress; }
    public void setAcousticStress(Double acousticStress) { this.acousticStress = acousticStress; }

    public Double getAcousticDistress() { return acousticDistress; }
    public void setAcousticDistress(Double acousticDistress) { this.acousticDistress = acousticDistress; }

    public Double getVocalInstability() { return vocalInstability; }
    public void setVocalInstability(Double vocalInstability) { this.vocalInstability = vocalInstability; }

    public Double getEmotionalDistress() { return acousticDistress != null ? acousticDistress : 0.0; }
    public Double getAnxietyRelated() { return anxiety != null ? anxiety : 0.0; }
    public Double getArousal() { return vocalInstability != null ? vocalInstability : 0.0; }

    public Map<String, Double> getIndicators() {
        Map<String, Double> map = new HashMap<>();
        map.put("stress", stress != null ? stress : 0.0);
        map.put("fear", fear != null ? fear : 0.0);
        map.put("anxiety", anxiety != null ? anxiety : 0.0);
        map.put("reduced_speaking_rate", getReducedSpeakingRate() != null ? getReducedSpeakingRate() : 0.0);
        map.put("acoustic_stress", acousticStress != null ? acousticStress : 0.0);
        map.put("acoustic_distress", acousticDistress != null ? acousticDistress : 0.0);
        map.put("vocal_instability", vocalInstability != null ? vocalInstability : 0.0);
        return map;
    }
}
