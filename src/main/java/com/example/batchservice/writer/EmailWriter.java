package com.example.batchservice.writer;

import org.springframework.batch.item.ItemWriter;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EmailWriter implements ItemWriter<String> {

    private final JavaMailSender mailSender;

    public EmailWriter(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void write(List<? extends String> messages) {
        for (String message : messages) {
            SimpleMailMessage mailMessage = new SimpleMailMessage();
            mailMessage.setTo("kbsserver@naver.com"); // 실제 수신자 이메일
            mailMessage.setSubject("Daily Update");
            mailMessage.setText(message);
            mailSender.send(mailMessage);
        }
    }
}
