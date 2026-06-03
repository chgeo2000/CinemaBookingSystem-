package org.example.cinemabookingsystem.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

    @Bean
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @ConfigurationProperties("spring.datasource.hikari")
    public DataSource hikariDataSource() {
        return dataSourceProperties().initializeDataSourceBuilder().build();
    }

    @Bean
    public JdbcClient jdbcClient(@Qualifier("hikariDataSource") DataSource dataSource) {
        return JdbcClient.create(dataSource);
    }
}
