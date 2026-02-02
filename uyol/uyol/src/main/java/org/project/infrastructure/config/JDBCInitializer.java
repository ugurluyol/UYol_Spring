package org.project.infrastructure.config;

import com.hadzhy.jetquerious.jdbc.JetQuerious;
import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

    @Configuration
    public class JDBCInitializer {

        @Bean
        public JetQuerious jetQuerious(DataSource dataSource) {
            JetQuerious.init(dataSource);   // 🔥 ИНИЦИАЛИЗАЦИЯ
            return JetQuerious.instance();  // 🔥 ГОТОВЫЙ singleton
        }
    }

    //private final DataSource dataSource;

    /*public JDBCInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @PostConstruct
    public void init() {
        JetQuerious.init(dataSource);
    }*/


