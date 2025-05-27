package com.example.batchservice.writer;

import org.springframework.batch.item.ItemWriter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class EmailWriter implements ItemWriter<String> {

    private final JavaMailSender mailSender;
    private final JdbcTemplate jdbcTemplate;
    private final ExecutorService executorService = Executors.newFixedThreadPool(5); // ✅ 최대 5개의 병렬 실행 제한

    public EmailWriter(JavaMailSender mailSender, JdbcTemplate jdbcTemplate) {
        this.mailSender = mailSender;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void write(List<? extends String> messages) {
        for (String message : messages) {
            try {
                // 이메일 주소와 템플릿을 분리
                String[] parts = message.split("::", 2);
                String email = parts[0];
                String emailContent = parts[1];

                // 이메일 전송을 비동기로 실행 (예외 감지 가능)
                CompletableFuture.supplyAsync(() -> {
                    try {
                        sendEmail(email, emailContent);
                        return email; //
                    } catch (MessagingException e) {
                        throw new RuntimeException("[EmailWriter] 이메일 전송 실패: " + email, e);
                    }
                }, executorService).thenAccept(successEmail -> {
                    jdbcTemplate.update("UPDATE subscriber SET sent = TRUE WHERE email = ?", successEmail);
                    System.out.println("[EmailWriter] 이메일 발송 완료: " + successEmail);
                }).exceptionally(ex -> {
                    System.err.println("[EmailWriter] 이메일 전송 실패: " + ex.getMessage());
                    return null;
                });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void sendEmail(String email, String htmlContent) throws MessagingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

        helper.setTo(email);
        helper.setSubject("\uD83D\uDCF0 FLOWBIT 예측가격 뉴스 업데이트");
        helper.setText(htmlContent, true);

        mailSender.send(mimeMessage);
    }
}
