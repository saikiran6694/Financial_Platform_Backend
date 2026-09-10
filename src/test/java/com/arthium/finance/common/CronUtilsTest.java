package com.arthium.finance.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CronUtilsTest {

    @Test
    void toSpringCron_nullExpression_throwsBadRequest() {
        assertThatThrownBy(() -> CronUtils.toSpringCron(null))
                .isInstanceOf(ApiException.class)
                .extracting(e -> ((ApiException) e).getStatus())
                .isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST);
    }

    @Test
    void toSpringCron_blankExpression_throwsBadRequest() {
        assertThatThrownBy(() -> CronUtils.toSpringCron("   "))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void toSpringCron_fiveFieldExpression_prependsSecondsField() {
        String result = CronUtils.toSpringCron("0 9 * * *");

        assertThat(result).isEqualTo("0 0 9 * * *");
    }

    @Test
    void toSpringCron_sixFieldExpression_isReturnedUnchanged() {
        String result = CronUtils.toSpringCron("0 0 9 * * *");

        assertThat(result).isEqualTo("0 0 9 * * *");
    }

    @Test
    void toSpringCron_collapsesRepeatedWhitespace() {
        String result = CronUtils.toSpringCron("0   9   *  *   *");

        assertThat(result).isEqualTo("0 0 9 * * *");
    }

    @Test
    void toSpringCron_invalidFieldCount_throwsBadRequest() {
        assertThatThrownBy(() -> CronUtils.toSpringCron("0 9 *"))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Invalid cron expression");
    }
}
