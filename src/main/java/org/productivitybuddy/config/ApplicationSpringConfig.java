package org.productivitybuddy.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.time.LocalTime;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import oshi.SystemInfo;

@Configuration(proxyBeanMethods = false)
@ComponentScan(basePackages = "org.productivitybuddy")
public class ApplicationSpringConfig {

    @Bean
    public ConversionService conversionService() {
        final DefaultConversionService service = new DefaultConversionService();
        service.addConverter(String.class, LocalTime.class, LocalTime::parse);
        return service;
    }

    @Bean
    public Gson gsonProvider() {
        return new GsonBuilder().setPrettyPrinting().create();
    }

    @Bean
    public SystemInfo systemInfo() {
        return new SystemInfo();
    }
}
