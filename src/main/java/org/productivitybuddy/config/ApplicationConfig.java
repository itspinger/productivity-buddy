package org.productivitybuddy.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:config.properties")
@Getter
public class ApplicationConfig {
    private static final String APP_DIRECTORY_NAME = "ProductivityBuddy";

    @Value("${monitor.interval}")
    private int monitorInterval;

    @Value("${mapping.file}")
    private String mappingFile;

    @Value("${snapshot.interval}")
    private int snapshotInterval;

    @Value("${snapshot.fixed_times}")
    private List<LocalTime> snapshotFixedTimes;

    public Path getResolvedMappingFilePath() {
        final Path configuredPath = Paths.get(this.mappingFile);
        if (configuredPath.isAbsolute()) {
            return configuredPath.normalize();
        }

        return this.getApplicationDataDirectory().resolve(configuredPath).normalize();
    }

    public Path getApplicationDataDirectory() {
        return this.resolveUserDataDirectory().normalize();
    }

    private Path resolveUserDataDirectory() {
        final String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        final String userHome = System.getProperty("user.home");

        if (osName.contains("mac")) {
            return Paths.get(userHome, "Library", "Application Support", APP_DIRECTORY_NAME);
        }

        if (osName.contains("win")) {
            final String appData = System.getenv("APPDATA");
            if (appData != null && !appData.isBlank()) {
                return Paths.get(appData, APP_DIRECTORY_NAME);
            }

            return Paths.get(userHome, "AppData", "Roaming", APP_DIRECTORY_NAME);
        }

        return Paths.get(userHome, ".local", "share", APP_DIRECTORY_NAME);
    }
}
