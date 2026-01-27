package org.project.infrastructure.config;

import com.hadzhy.jetquerious.jdbc.JetQuerious;
import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;

import org.springframework.context.annotation.Configuration;

@Configuration
public class JDBCInitializer {

    private final DataSource dataSource;

    public JDBCInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void init() {
        JetQuerious.init(dataSource);
    }
}
