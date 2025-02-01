package com.example.batchservice.reader;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EmailReader {

    private final JdbcTemplate jdbcTemplate;

    public EmailReader(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<String> readAll() {
        return jdbcTemplate.queryForList(
                "SELECT email FROM subscriber WHERE sent = FALSE",
                String.class
        );
    }
}
