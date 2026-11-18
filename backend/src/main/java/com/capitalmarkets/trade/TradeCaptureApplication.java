package com.capitalmarkets.trade;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
@ConfigurationPropertiesScan
public class TradeCaptureApplication {
    public static void main(String[] args) {
        SpringApplication.run(TradeCaptureApplication.class, args);
    }
}
