package com.adsentry.importing;

import com.adsentry.settings.domain.AppSetting;
import com.adsentry.settings.repository.AppSettingRepository;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class FlywayMigrationIntegrationTest {

    @Autowired
    private Flyway flyway;

    @Autowired
    private AppSettingRepository appSettingRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flywayMigration_shouldApplySuccessfully() {
        var info = flyway.info().current();
        assertThat(info).isNotNull();
        assertThat(info.getVersion().getVersion()).isEqualTo("1");
        assertThat(info.getState().isApplied()).isTrue();
    }

    @Test
    void appSettings_shouldContainInitialExampleDefaults() {
        Optional<AppSetting> cpaTarget = appSettingRepository.findBySettingKey("global_cpa_target");
        assertThat(cpaTarget).isPresent();
        assertThat(cpaTarget.get().getSettingValue()).isEqualTo("500.00");

        Optional<AppSetting> minSpend = appSettingRepository.findBySettingKey("min_spend_threshold");
        assertThat(minSpend).isPresent();
        assertThat(minSpend.get().getSettingValue()).isEqualTo("1000.00");
    }

    @Test
    void effectiveRecordsView_shouldBeQueryable() {
        Integer count = jdbcTemplate.queryForObject("SELECT count(*) FROM effective_records_view", Integer.class);
        assertThat(count).isNotNull();
        assertThat(count).isEqualTo(0);
    }
}
