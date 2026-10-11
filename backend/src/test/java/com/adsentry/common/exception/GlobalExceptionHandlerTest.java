package com.adsentry.common.exception;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(GlobalExceptionHandlerTest.TestErrorController.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @TestConfiguration
    @RestController
    static class TestErrorController {

        record DummyValidationRequest(
                @NotBlank(message = "Platform is required")
                String platform,

                @Positive(message = "Spend must be positive")
                BigDecimal spend
        ) {}

        @GetMapping("/api/test/api-exception")
        public void throwApiException() {
            throw new ApiException(ErrorCode.UNSUPPORTED_CURRENCY, "Currency 'USD' is not supported. Only INR is supported.");
        }

        @PostMapping("/api/test/validation")
        public void triggerValidation(@Valid @RequestBody DummyValidationRequest request) {
            // No-op
        }

        @GetMapping("/api/test/unexpected-error")
        public void throwUnexpected() {
            throw new RuntimeException("Simulated unexpected internal database failure");
        }
    }

    @Test
    void handleApiException_shouldReturnRfc7807AndStableCode() throws Exception {
        mockMvc.perform(get("/api/test/api-exception"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type", is("about:blank")))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.code", is("UNSUPPORTED_CURRENCY")))
                .andExpect(jsonPath("$.detail", containsString("Only INR is supported")))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    void handleValidationException_shouldReturnFieldErrorsList() throws Exception {
        String invalidJson = """
                {
                    "platform": "",
                    "spend": -50.00
                }
                """;

        mockMvc.perform(post("/api/test/validation")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")))
                .andExpect(jsonPath("$.errors", hasSize(2)))
                .andExpect(jsonPath("$.errors[*].field", containsInAnyOrder("platform", "spend")));
    }

    @Test
    void handleNotFound_shouldReturn404Rfc7807() throws Exception {
        mockMvc.perform(get("/api/non-existent-route-999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("RESOURCE_NOT_FOUND")))
                .andExpect(jsonPath("$.status", is(404)));
    }

    @Test
    void handleGenericException_shouldSanitizeDetailsAndNotLeakStacktrace() throws Exception {
        mockMvc.perform(get("/api/test/unexpected-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code", is("INTERNAL_SERVER_ERROR")))
                .andExpect(jsonPath("$.status", is(500)))
                .andExpect(jsonPath("$.detail", not(containsString("Simulated unexpected"))))
                .andExpect(jsonPath("$.stackTrace").doesNotExist());
    }
}
