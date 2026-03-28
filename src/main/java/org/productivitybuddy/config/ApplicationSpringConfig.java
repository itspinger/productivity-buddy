package org.productivitybuddy.config;

import java.time.LocalTime;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;

@Configuration
@ComponentScan(basePackages = "org.productivitybuddy")
public class ApplicationSpringConfig {

    @Bean
    public ConversionService conversionService() {
        final DefaultConversionService service = new DefaultConversionService();
        service.addConverter(String.class, LocalTime.class, LocalTime::parse);
        return service;
    }

}
