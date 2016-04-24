package ru.georgeee.bachelor.yarn.dict.wikt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class WiktionaryDbConfig {
    private static final Logger log = LoggerFactory.getLogger(WiktionaryDbConfig.class);

    @Bean(name = "enwikt.datasource")
    @ConfigurationProperties(prefix = "dict.enwikt")
    public DataSource enwiktDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "enwikt.jdbc")
    @Autowired
    public JdbcTemplate enwiktJdbcTemplate(@Qualifier("enwikt.datasource") DataSource dataSource) {
        log.info("enwiktJdbc: data source {}", dataSource);
        return new JdbcTemplate(dataSource);
    }

    //@TODO remove @Primary if other DB would be configured (currently this is set for compatibility with Spring boot)
    @Primary
    @Bean(name = "ruwikt.datasource")
    @ConfigurationProperties(prefix = "dict.ruwikt")
    public DataSource ruwiktDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "ruwikt.jdbc")
    @Autowired
    public JdbcTemplate ruwiktJdbcTemplate(@Qualifier("ruwikt.datasource") DataSource dataSource) {
        log.info("ruwiktJdbc: data source {}", dataSource);
        return new JdbcTemplate(dataSource);
    }
}

