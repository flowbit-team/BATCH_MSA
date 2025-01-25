package com.example.batchservice.reader;


import org.springframework.batch.item.ItemReader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EmailReader implements ItemReader<String> {

    private final JdbcTemplate jdbcTemplate;
    private int index = 0;
    private List<String> emails;

    public EmailReader(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public String read() {
        if (emails == null) {
            emails = jdbcTemplate.queryForList("SELECT user_id FROM member", String.class);
        }

        if (index < emails.size()) {
            return emails.get(index++);
        } else {
            index = 0;
            return null;
        }
    }
}
