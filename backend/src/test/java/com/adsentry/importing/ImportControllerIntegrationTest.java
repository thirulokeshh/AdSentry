package com.adsentry.importing;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ImportControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("POST /api/imports/preview previews CSV without saving")
    void previewEndpoint_returnsPreviewSummary() throws Exception {
        String csvContent = "date,channel,campaign,spend\n2026-03-01,Google Ads,Brand-1,500.00\n2026-03-01,Organic,Brand-2,300.00";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.csv",
                "text/csv",
                csvContent.getBytes()
        );

        mockMvc.perform(multipart("/api/imports/preview")
                        .file(file)
                        .param("confirmedCurrency", "INR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalRows").value(2))
                .andExpect(jsonPath("$.estimatedAcceptedRows").value(1))
                .andExpect(jsonPath("$.estimatedRejectedRows").value(1))
                .andExpect(jsonPath("$.detectedCurrency").value("INR"));
    }

    @Test
    @DisplayName("POST /api/imports executes import and returns created batch")
    void importEndpoint_createsBatch() throws Exception {
        String csvContent = "date,channel,campaign,spend\n2026-03-01,Google Ads,Brand-1,500.00";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "real_import.csv",
                "text/csv",
                csvContent.getBytes()
        );

        String responseBody = mockMvc.perform(multipart("/api/imports")
                        .file(file)
                        .param("confirmedCurrency", "INR"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.acceptedRows").value(1))
                .andReturn().getResponse().getContentAsString();

        // Extract ID and test GET /api/imports
        mockMvc.perform(get("/api/imports"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("Unsupported currency in preview returns 400 with UNSUPPORTED_CURRENCY")
    void previewEndpoint_unsupportedCurrencyRejected() throws Exception {
        String csvContent = "date,channel,campaign,Cost (USD)\n2026-03-01,Google Ads,Brand-1,500.00";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "usd_export.csv",
                "text/csv",
                csvContent.getBytes()
        );

        mockMvc.perform(multipart("/api/imports/preview").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_CURRENCY"));
    }
}
