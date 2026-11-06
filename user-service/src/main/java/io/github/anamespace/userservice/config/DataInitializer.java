package io.github.anamespace.userservice.config;

import io.github.anamespace.userservice.service.AdminSeeder;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner seedAdminUser(AdminSeeder adminSeeder) {
        return args -> adminSeeder.seed();
    }
}
