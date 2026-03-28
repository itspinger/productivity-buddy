package org.productivitybuddy.config;

import java.time.LocalTime;
import java.util.List;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:config.properties")
@Getter
public class ApplicationConfig {

    @Value("${monitor.interval}")
    private int monitorInterval;

    @Value("${mapping.file}")
    private String mappingFile;

    @Value("${snapshot.interval}")
    private int snapshotInterval;

    @Value("${snapshot.fixed_times}")
    private List<LocalTime> snapshotFixedTimes;
}
