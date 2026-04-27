package Entities.event;

public enum EventReclamationStatus {
    OPEN,
    AI_RESPONDED,
    IN_PROGRESS,
    ESCALATED,
    RESOLVED,
    REJECTED;

    public static EventReclamationStatus fromDatabase(String raw) {
        if (raw == null || raw.isBlank()) {
            return OPEN;
        }
        String normalized = raw.trim().toUpperCase();
        if ("AI_RESPONSE".equals(normalized) || "AI_RESPONDED".equals(normalized)) {
            return AI_RESPONDED;
        }
        try {
            return EventReclamationStatus.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            return OPEN;
        }
    }
}
