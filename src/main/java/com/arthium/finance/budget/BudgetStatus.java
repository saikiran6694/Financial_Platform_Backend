package com.arthium.finance.budget;

import com.fasterxml.jackson.annotation.JsonValue;

/** on_track | warning | exceeded — the values the dashboard already expects. */
public enum BudgetStatus {

    ON_TRACK("on_track"),
    WARNING("warning"),
    EXCEEDED("exceeded");

    private final String value;

    BudgetStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    /** Ordered so EXCEEDED outranks WARNING when de-duplicating alert emails. */
    public int rank() {
        return switch (this) {
            case ON_TRACK -> 0;
            case WARNING -> 1;
            case EXCEEDED -> 2;
        };
    }

    public static BudgetStatus fromValue(String raw) {
        if (raw == null) {
            return null;
        }
        for (BudgetStatus status : values()) {
            if (status.value.equalsIgnoreCase(raw) || status.name().equalsIgnoreCase(raw)) {
                return status;
            }
        }
        return null;
    }
}
