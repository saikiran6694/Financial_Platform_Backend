package com.arthium.finance.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ValuesTest {

    @Test
    void asLong_integerValue_returnsLongValue() {
        assertThat(Values.asLong(42)).isEqualTo(42L);
    }

    @Test
    void asLong_doubleValue_truncatesToLong() {
        assertThat(Values.asLong(42.9)).isEqualTo(42L);
    }

    @Test
    void asLong_nonNumericValue_returnsZero() {
        assertThat(Values.asLong("not a number")).isEqualTo(0L);
    }

    @Test
    void asLong_nullValue_returnsZero() {
        assertThat(Values.asLong(null)).isEqualTo(0L);
    }

    @Test
    void asDouble_numericValue_returnsDoubleValue() {
        assertThat(Values.asDouble(10)).isEqualTo(10.0);
    }

    @Test
    void asDouble_nonNumericValue_returnsZero() {
        assertThat(Values.asDouble("nope")).isEqualTo(0.0);
    }

    @Test
    void asString_nullValue_returnsNull() {
        assertThat(Values.asString(null)).isNull();
    }

    @Test
    void asString_numericValue_returnsStringRepresentation() {
        assertThat(Values.asString(5)).isEqualTo("5");
    }

    @Test
    void asString_stringValue_returnsSameString() {
        assertThat(Values.asString("groceries")).isEqualTo("groceries");
    }
}
