package com.example.batchservice.reader;

import com.example.batchservice.dto.SubscriberWithKeywords;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Component
public class EmailReader {

    private final JdbcTemplate jdbcTemplate;

    public EmailReader(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<SubscriberWithKeywords> readAll() {
        return jdbcTemplate.query(
                "SELECT s.email, i.keyword FROM subscriber s JOIN interest i ON s.subscriber_id = i.subscriber_id where s.sent = FALSE",
                (rs) -> {
                    HashMap<String, SubscriberWithKeywords> map = new HashMap<>();

                    while (rs.next()) {
                        String email = rs.getString("email");
                        String keyword = rs.getString("keyword");

                        SubscriberWithKeywords subscriberWithKeywords = map.computeIfAbsent(email, k -> {
                            SubscriberWithKeywords newSubscriberWithKeywords = new SubscriberWithKeywords();
                            newSubscriberWithKeywords.setEmail(email);
                            newSubscriberWithKeywords.setKeywords(new ArrayList<>());
                            return newSubscriberWithKeywords;
                        });

                        subscriberWithKeywords.getKeywords().add(keyword);
                    }

                    return new ArrayList<>(map.values());
                }
        );
    }
}
