package com.nhaa.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SocialContextDto {

    private String socialBoycott = "UNKNOWN";
    private String socialIsolation = "UNKNOWN";
    private String socialExclusion = "UNKNOWN";
    private String displacement = "UNKNOWN";

    public SocialContextDto() {}

    public SocialContextDto(String socialBoycott, String socialIsolation, String displacement) {
        this.socialBoycott = parseTriState(socialBoycott);
        this.socialIsolation = parseTriState(socialIsolation);
        this.displacement = parseTriState(displacement);
    }

    public String getSocialBoycott() {
        return socialBoycott;
    }

    public void setSocialBoycott(Object socialBoycott) {
        this.socialBoycott = parseTriState(socialBoycott);
    }

    public String getSocialIsolation() {
        return socialIsolation;
    }

    public void setSocialIsolation(Object socialIsolation) {
        this.socialIsolation = parseTriState(socialIsolation);
    }

    public String getSocialExclusion() {
        return socialExclusion;
    }

    public void setSocialExclusion(Object val) {
        this.socialExclusion = parseTriState(val);
    }

    public String getDisplacement() {
        return displacement;
    }

    public void setDisplacement(Object displacement) {
        this.displacement = parseTriState(displacement);
    }

    private static String parseTriState(Object val) {
        if (val instanceof Boolean b) {
            return b ? "TRUE" : "FALSE";
        } else if (val != null) {
            String s = val.toString().trim().toUpperCase();
            if (s.equals("TRUE") || s.equals("FALSE")) return s;
        }
        return "UNKNOWN";
    }
}
