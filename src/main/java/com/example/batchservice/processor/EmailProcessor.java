package com.example.batchservice.processor;

import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class EmailProcessor implements ItemProcessor<String, String> {

    @Override
    public String process(String email) {
        return "Hello " + email + ", this is your daily update!";
    }
}
