package com.arthium.finance.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MoneyUtilsTest {

    @Test
    void toCents_convertsWholeDollarAmount() {
        assertThat(MoneyUtils.toCents(10.0)).isEqualTo(1000L);
    }

    @Test
    void toCents_roundsHalfUp() {
        assertThat(MoneyUtils.toCents(19.995)).isEqualTo(2000L);
    }

    @Test
    void toCents_handlesZero() {
        assertThat(MoneyUtils.toCents(0)).isEqualTo(0L);
    }

    @Test
    void toCents_handlesNegativeAmount() {
        assertThat(MoneyUtils.toCents(-5.5)).isEqualTo(-550L);
    }

    @Test
    void toDollars_convertsCentsBackToDollars() {
        assertThat(MoneyUtils.toDollars(1000L)).isEqualTo(10.0);
    }

    @Test
    void toDollars_handlesZero() {
        assertThat(MoneyUtils.toDollars(0L)).isEqualTo(0.0);
    }

    @Test
    void toDollars_handlesNegativeCents() {
        assertThat(MoneyUtils.toDollars(-550L)).isEqualTo(-5.5);
    }

    @Test
    void savingRate_incomeZero_returnsZero() {
        assertThat(MoneyUtils.savingRate(0, 100)).isEqualTo(0.0);
    }

    @Test
    void savingRate_incomeNegative_returnsZero() {
        assertThat(MoneyUtils.savingRate(-100, 50)).isEqualTo(0.0);
    }

    @Test
    void savingRate_expensesBelowIncome_returnsPositiveRate() {
        assertThat(MoneyUtils.savingRate(1000, 600)).isEqualTo(40.0);
    }

    @Test
    void savingRate_expensesExceedIncome_returnsNegativeRate() {
        assertThat(MoneyUtils.savingRate(1000, 1200)).isEqualTo(-20.0);
    }

    @Test
    void round_nanValue_returnsZero() {
        assertThat(MoneyUtils.round(Double.NaN, 2)).isEqualTo(0.0);
    }

    @Test
    void round_infiniteValue_returnsZero() {
        assertThat(MoneyUtils.round(Double.POSITIVE_INFINITY, 2)).isEqualTo(0.0);
    }

    @Test
    void round_normalValue_roundsHalfUpToScale() {
        assertThat(MoneyUtils.round(12.345, 2)).isEqualTo(12.35);
    }
}
