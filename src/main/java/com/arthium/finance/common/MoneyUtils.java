package com.arthium.finance.common;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Port of utils/utils.py money helpers. Amounts are stored in MongoDB as
 * integer cents and converted at the service boundary.
 */
public final class MoneyUtils {

    private MoneyUtils() {
    }

    /** convert_to_cents */
    public static long toCents(double dollars) {
        return BigDecimal.valueOf(dollars)
                .multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();
    }

    /** convert_to_dollar_unit */
    public static double toDollars(long cents) {
        return BigDecimal.valueOf(cents, 2).doubleValue();
    }

    /** calculate_saving_rate */
    public static double savingRate(double totalIncome, double totalExpense) {
        if (totalIncome <= 0) {
            return 0d;
        }
        double percentage = ((totalIncome - totalExpense) / totalIncome) * 100;
        return round(percentage, 2);
    }

    public static double round(double value, int scale) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return 0d;
        }
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }
}
