package com.arthium.finance.common;

import java.util.UUID;

/** UUID validity check, replacing Mongo's {@code ObjectId.isValid}. */
public final class Ids {

    private Ids() {
    }

    public static boolean isValid(String id) {
        if (id == null) {
            return false;
        }
        try {
            UUID.fromString(id);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
