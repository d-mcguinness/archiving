package com.dmc.archiving.web;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pins how the framework serializes {@link LocalDateTime} over HTTP.
 *
 * <p>Two Jackson generations now sit on the classpath together: Spring Boot 4
 * converts HTTP bodies with Jackson 3 ({@code tools.jackson}), while parts of
 * this app hand-build Jackson 2 ({@code com.fasterxml}) mappers for export and
 * package generation. Nothing else in the suite asserts a date format, so a
 * change of default would sail past 200-odd green tests and only surface as a
 * broken payload for API clients.
 *
 * <p>The error {@code timestamp} is used as the canary because it needs no
 * fixtures or auth, and it travels through the very same message converter as
 * every entity {@code createdAt}/{@code updatedAt} the API returns.
 *
 * <p>Confirmed to fail on the regression it exists to catch: setting
 * {@code spring.jackson.datatype.datetime.write-dates-as-timestamps=true} turns
 * the value into {@code [2026,9,6,22,29,59,934290000]} and this test goes red.
 * Note the Jackson 2 spelling of that property,
 * {@code spring.jackson.serialization.write-dates-as-timestamps}, no longer
 * binds under Boot 4 — {@code WRITE_DATES_AS_TIMESTAMPS} moved off
 * {@code SerializationFeature} onto {@code DateTimeFeature}, so the old name now
 * fails context startup outright rather than being quietly ignored.
 */
@SpringBootTest
@AutoConfigureMockMvc
class JsonDateFormatTest {

    @Autowired private MockMvc mvc;

    @Test
    void localDateTimeIsSerializedAsAnIsoString() throws Exception {
        String body = mvc.perform(get("/api/no-such-endpoint-exists"))
                .andExpect(status().isNotFound())
                // A JSON string. With timestamp writing enabled this is an array
                // of date parts instead, and this expectation fails.
                .andExpect(jsonPath("$.timestamp").isString())
                .andReturn().getResponse().getContentAsString();

        String timestamp = JsonPath.read(body, "$.timestamp");

        // LocalDateTime.parse accepts only ISO-8601 local date-time, so this
        // pins the actual format rather than merely "some string".
        assertThatCode(() -> LocalDateTime.parse(timestamp))
                .as("error timestamp %s should be ISO-8601, e.g. 2026-09-06T21:59:50.902889", timestamp)
                .doesNotThrowAnyException();

        assertThat(timestamp).doesNotContain("[").doesNotContain("Z");
    }

    @Test
    void aNumericTimestampWouldFailThisSuite() {
        // Guards the guard: if LocalDateTime.parse ever started accepting epoch
        // numbers, the test above would pass against exactly the regression it
        // exists to catch.
        assertThatCode(() -> LocalDateTime.parse("1788712790902"))
                .isInstanceOf(DateTimeParseException.class);
    }
}
