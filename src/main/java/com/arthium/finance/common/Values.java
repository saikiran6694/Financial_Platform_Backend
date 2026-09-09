package com.arthium.finance.common;

/** Small coercions for values coming back from raw BSON aggregation results. */
public final class Values {

    private Values() {
    }

    public static long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return 0L;
    }

    public static double asDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return 0d;
    }

    public static String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
