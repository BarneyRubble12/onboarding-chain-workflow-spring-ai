package com.hrpd.onboarding.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;


@Configuration
public class JdbcDataSourceConfig {

    @Bean
    @Primary
    public DataSource jdbcDataSource() {
        return DataSourceBuilder.create()
                .url("jdbc:postgresql://localhost:5432/onboarding")
                .username("onboarding")
                .password("onboarding")
                .driverClassName("org.postgresql.Driver")
                .build();
    }

    @Bean
    public JdbcTemplate jdbcTemplate(DataSource jdbcDataSource) {
        return new JdbcTemplate(jdbcDataSource);
    }
}
