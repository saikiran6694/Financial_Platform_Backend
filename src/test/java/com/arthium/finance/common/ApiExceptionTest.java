package com.arthium.finance.common;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;

class ApiExceptionTest {

    @Test
    void badRequest_hasBadRequestStatusAndDetailMessage() {
        ApiException ex = ApiException.badRequest("bad input");

        assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(ex.getMessage()).isEqualTo("bad input");
    }

    @Test
    void unauthorized_hasUnauthorizedStatus() {
        assertThat(ApiException.unauthorized("nope").getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void notFound_hasNotFoundStatus() {
        assertThat(ApiException.notFound("missing").getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void conflict_hasConflictStatus() {
        assertThat(ApiException.conflict("duplicate").getStatus()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void internal_hasInternalServerErrorStatus() {
        assertThat(ApiException.internal("boom").getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
