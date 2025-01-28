package com.example.batchservice.writer;

import org.springframework.batch.item.ItemWriter;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
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
            try {
                // 메시지에서 이메일과 HTML 분리
                String[] parts = message.split("::", 2);
                String email = parts[0];  // 수신자 이메일 주소
                String htmlContent = parts[1]; // HTML 메시지

                // MIME 메시지 생성
                MimeMessage mimeMessage = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
                helper.setTo(email);
                helper.setSubject("FLOWBIT 예측가격 뉴스 업데이트");
                helper.setText(htmlContent, true);

                // 이메일 전송
                mailSender.send(mimeMessage);
            } catch (MessagingException e) {
                e.printStackTrace();
            }
        }
    }
}
