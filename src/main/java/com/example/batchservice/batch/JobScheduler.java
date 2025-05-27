package com.example.batchservice.batch;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.example.batchservice.dto.DiscordPayload;
import com.example.batchservice.processor.EmailProcessor;
import com.example.batchservice.service.TemplateCache;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
@EnableScheduling
public class JobScheduler {
    private static final Logger logger = LoggerFactory.getLogger(JobScheduler.class);
    private final JobLauncher jobLauncher;
    private final Job userNotificationJob;
    private final JdbcTemplate jdbcTemplate;
    private final EmailProcessor emailProcessor;
    private final TemplateCache templateCache;
    private static boolean isRunning = false;
    private static LocalDate lastWebhookSentDate = null;
    private static final AtomicBoolean isWebhookSentToday = new AtomicBoolean(false);

    @Value("${discord.webhook.url}")
    private String discordWebhookUrl;

    public JobScheduler(JobLauncher jobLauncher, 
                       Job userNotificationJob, 
                       JdbcTemplate jdbcTemplate, 
                       EmailProcessor emailProcessor,
                       TemplateCache templateCache) {
        this.jobLauncher = jobLauncher;
        this.userNotificationJob = userNotificationJob;
        this.jdbcTemplate = jdbcTemplate;
        this.emailProcessor = emailProcessor;
        this.templateCache = templateCache;
    }

    @Scheduled(cron = "0 0/1 * * * ?")
    public synchronized void scheduleJob() {
        if (isRunning) {
            logger.info("이미 실행 중이므로 새로운 Job 실행 안 함.");
            return;
        }

        int count = getPendingEmailCount();
        logger.info("현재 남은 이메일 개수: {}", count);

        if (count > 0) {
            try {
                isRunning = true;
                logger.info("배치 실행 시작.");

                if (isWebhookSentToday.compareAndSet(false, true)) {
                    sendToDiscord(emailProcessor.generateDiscordMessage());
                }

                JobExecution jobExecution = jobLauncher.run(userNotificationJob, new JobParametersBuilder()
                        .addLong("timestamp", System.currentTimeMillis())
                        .toJobParameters());

                logger.info("배치 실행 완료. 상태: {}", jobExecution.getStatus());
            } catch (Exception e) {
                logger.error("이메일 배치 실행 중 오류 발생", e);
            } finally {
                isRunning = false;
            }
        } else {
            logger.info("모든 이메일이 처리되어 실행 안 함.");
        }
    }

    private int getPendingEmailCount() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM subscriber WHERE sent = FALSE", Integer.class);
    }

    private void sendToDiscord(String message) {
        if (lastWebhookSentDate == null || LocalDate.now().isAfter(lastWebhookSentDate)) {
            RestTemplate restTemplate = new RestTemplate();
            try {
                ObjectMapper objectMapper = new ObjectMapper();
                String payload = objectMapper.writeValueAsString(new DiscordPayload(message));

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<String> entity = new HttpEntity<>(payload, headers);

                restTemplate.postForEntity(discordWebhookUrl, entity, String.class);

                lastWebhookSentDate = LocalDate.now();
                logger.info("Discord Webhook으로 메시지 전송 완료.");
            } catch (Exception e) {
                logger.error("Discord Webhook 전송 중 오류 발생", e);
            }
        }
    }

    @Scheduled(cron = "0 0 10 * * ?")
    public void resetEmailStatusAndTemplate() {
        try {
            logger.info("한국 시간 아침 7시 - 이메일 상태 초기화 시작.");
            jdbcTemplate.update("UPDATE subscriber SET sent = FALSE WHERE sent = TRUE");
            logger.info("이메일 상태 초기화 완료.");

            // TemplateCache를 통해 캐시 초기화
            templateCache.resetCache();
            
            isWebhookSentToday.set(false);
            logger.info("모든 초기화 작업 완료.");
        } catch (Exception e) {
            logger.error("이메일 상태 초기화 또는 템플릿 초기화 중 오류 발생", e);
        }
    }
}