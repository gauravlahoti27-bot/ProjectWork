package com.trekmate.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;

@Configuration
public class DataSourceConfig {

    private static final Logger logger = LoggerFactory.getLogger(DataSourceConfig.class);

    @Value("${spring.datasource.url:jdbc:mysql://localhost:3306/trekmate_db}")
    private String mysqlUrl;

    @Value("${spring.datasource.username:root}")
    private String mysqlUsername;

    @Value("${spring.datasource.password:root}")
    private String mysqlPassword;

    @Bean
    @Primary
    public DataSource dataSource() {
        // Test if MySQL server is reachable
        try {
            logger.info("Attempting connection to MySQL server at: {}", mysqlUrl);
            DriverManager.setLoginTimeout(3);
            Connection conn = DriverManager.getConnection(mysqlUrl, mysqlUsername, mysqlPassword);
            conn.close();
            logger.info(">>> SUCCESS: Connected to MySQL database!");
            return DataSourceBuilder.create()
                    .url(mysqlUrl)
                    .username(mysqlUsername)
                    .password(mysqlPassword)
                    .driverClassName("com.mysql.cj.jdbc.Driver")
                    .build();
        } catch (Exception ex) {
            logger.warn(">>> NOTICE: Could not connect to local MySQL server ({}).", ex.getMessage());
            logger.warn(">>> AUTOMATIC FALLBACK: Starting embedded H2 database so TrekMate can run immediately without errors.");
            return DataSourceBuilder.create()
                    .url("jdbc:h2:mem:trekmate_db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE;MODE=MySQL")
                    .username("sa")
                    .password("")
                    .driverClassName("org.h2.Driver")
                    .build();
        }
    }
}

