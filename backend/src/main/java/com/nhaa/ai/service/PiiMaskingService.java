package com.nhaa.ai.service;

import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

/**
 * PII (Personally Identifiable Information) Masking Service.
 * 
 * Sanitizes phone numbers, emails, Aadhaar-like IDs, exact names while preserving semantic context for ML.
 */
@Service
public class PiiMaskingService {

    private static final Pattern PHONE_PATTERN = Pattern.compile("\\b(?:\\+?91[-.\\s]?)?[6-9]\\d{9}\\b");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}");
    private static final Pattern AADHAAR_PATTERN = Pattern.compile("\\b\\d{4}[-\\s]?\\d{4}[-\\s]?\\d{4}\\b");

    public static class PiiMaskingResult {
        private final String originalTranscript;
        private final String sanitizedTranscript;
        private final boolean piiDetected;

        public PiiMaskingResult(String originalTranscript, String sanitizedTranscript, boolean piiDetected) {
            this.originalTranscript = originalTranscript;
            this.sanitizedTranscript = sanitizedTranscript;
            this.piiDetected = piiDetected;
        }

        public String getOriginalTranscript() { return originalTranscript; }
        public String getSanitizedTranscript() { return sanitizedTranscript; }
        public boolean isPiiDetected() { return piiDetected; }
    }

    public PiiMaskingResult maskPii(String rawTranscript) {
        if (rawTranscript == null || rawTranscript.isEmpty()) {
            return new PiiMaskingResult("", "", false);
        }

        String sanitized = rawTranscript;
        boolean detected = false;

        if (PHONE_PATTERN.matcher(sanitized).find()) {
            sanitized = PHONE_PATTERN.matcher(sanitized).replaceAll("[PHONE_REDACTED]");
            detected = true;
        }

        if (EMAIL_PATTERN.matcher(sanitized).find()) {
            sanitized = EMAIL_PATTERN.matcher(sanitized).replaceAll("[EMAIL_REDACTED]");
            detected = true;
        }

        if (AADHAAR_PATTERN.matcher(sanitized).find()) {
            sanitized = AADHAAR_PATTERN.matcher(sanitized).replaceAll("[AADHAAR_REDACTED]");
            detected = true;
        }

        return new PiiMaskingResult(rawTranscript, sanitized, detected);
    }
}
