package com.adsentry.settings;

import com.adsentry.settings.dto.AppSettingsDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AppSettingsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/settings returns agreed defaults for CPA target and min spend threshold")
    void getSettings_returnsAgreedDefaults() throws Exception {
        mockMvc.perform(get("/api/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.globalCpaTarget").value(500.00))
                .andExpect(jsonPath("$.minSpendThreshold").value(1000.00))
                .andExpect(jsonPath("$.priorityHighSpendCutoff").value(5000.00))
                .andExpect(jsonPath("$.priorityHighCpaMultiplier").value(2.0));
    }

    @Test
    @DisplayName("PUT /api/settings updates settings and rejects negative values")
    void updateSettings_validatesAndUpdates() throws Exception {
        // Valid update
        AppSettingsDto updateDto = new AppSettingsDto(
                new BigDecimal("650.00"),
                new BigDecimal("1200.00"),
                new BigDecimal("6000.00"),
                new BigDecimal("2.5"),
                null
        );

        mockMvc.perform(put("/api/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.globalCpaTarget").value(650.00))
                .andExpect(jsonPath("$.minSpendThreshold").value(1200.00));

        // Invalid update: negative CPA target rejected
        AppSettingsDto negativeDto = new AppSettingsDto(
                new BigDecimal("-50.00"),
                new BigDecimal("1000.00"),
                new BigDecimal("5000.00"),
                new BigDecimal("2.0"),
                null
        );

        mockMvc.perform(put("/api/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(negativeDto)))
                .andExpect(status().isBadRequest());
    }
}
