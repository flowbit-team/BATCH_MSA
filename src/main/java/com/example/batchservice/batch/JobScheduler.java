package com.example.batchservice.batch;

import com.example.batchservice.dto.DiscordPayload;
import com.example.batchservice.processor.EmailProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@EnableScheduling
public class JobScheduler {

    private final JobLauncher jobLauncher;
    private final Job userNotificationJob;
    private final JdbcTemplate jdbcTemplate;
    private final EmailProcessor emailProcessor;
    private static boolean isRunning = false; // 실행 상태 변수
    private static LocalDate lastWebhookSentDate = null; // 마지막으로 Webhook 전송된 날짜
    private static final AtomicBoolean isWebhookSentToday = new AtomicBoolean(false); // 하루에 한번만 Webhook 보내기 위한 상태

    @Value("${discord.webhook.url}")
    private String DISCORD_WEBHOOK_URL;
    @Autowired
    public JobScheduler(JobLauncher jobLauncher, Job userNotificationJob, JdbcTemplate jdbcTemplate, EmailProcessor emailProcessor) {
        this.jobLauncher = jobLauncher;
        this.userNotificationJob = userNotificationJob;
        this.jdbcTemplate = jdbcTemplate;
        this.emailProcessor = emailProcessor;
    }

    /**
     * 5분마다 실행하며, 남은 이메일이 있으면 전송 및 Discord Webhook 전송
     */
    @Scheduled(cron = "0 0/1 * * * ?") // 5분마다 실행
    public synchronized void scheduleJob() {
        if (isRunning) {
            System.out.println("[Scheduler] 이미 실행 중이므로 새로운 Job 실행 안 함.");
            return;
        }

        int count = getPendingEmailCount();
        System.out.println("[Scheduler] 현재 남은 이메일 개수: " + count);

        if (count > 0) {
            try {
                isRunning = true; // 실행 상태 변경
                System.out.println("[Scheduler] 배치 실행 시작.");

                // 이메일 템플릿을 Discord로 전송
                if (isWebhookSentToday.compareAndSet(false, true)) {
                    sendToDiscord(emailProcessor.generateDiscordMessage());
                }

                JobExecution jobExecution = jobLauncher.run(userNotificationJob, new JobParametersBuilder()
                        .addLong("timestamp", System.currentTimeMillis())
                        .toJobParameters());

                System.out.println("[Scheduler] 배치 실행 완료. 상태: " + jobExecution.getStatus());



            } catch (Exception e) {
                System.err.println("이메일 배치 실행 중 오류 발생: " + e.getMessage());
            } finally {
                isRunning = false; // 실행 완료 후 상태 변경
            }
        } else {
            System.out.println("[Scheduler] 모든 이메일이 처리되어 실행 안 함.");
        }
    }

    /**
     * 아직 보내지 않은 이메일 개수 조회
     */
    private int getPendingEmailCount() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM subscriber WHERE sent = FALSE", Integer.class);
    }


    private void sendToDiscord(String message) {
        if (lastWebhookSentDate == null || LocalDate.now().isAfter(lastWebhookSentDate)) {
            // 하루가 지난 경우에만 Discord로 전송
            RestTemplate restTemplate = new RestTemplate();
            try {
                // JSON 데이터를 안전하게 처리하기 위해 ObjectMapper 사용
                ObjectMapper objectMapper = new ObjectMapper();
                String payload = objectMapper.writeValueAsString(new DiscordPayload(message));

                // 요청 헤더에 Content-Type을 application/json으로 설정
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                // HttpEntity에 payload와 headers를 함께 전달
                HttpEntity<String> entity = new HttpEntity<>(payload, headers);

                // Webhook URL로 POST 요청 전송
                restTemplate.postForEntity(DISCORD_WEBHOOK_URL, entity, String.class);

                lastWebhookSentDate = LocalDate.now();
                System.out.println("[Scheduler] Discord Webhook으로 메시지 전송 완료.");
            } catch (Exception e) {
                System.err.println("Discord Webhook 전송 중 오류 발생: " + e.getMessage());
            }
        }
    }




    /**
     * 한국 시간 아침 7시에 모든 이메일의 'sent' 상태를 False로 초기화하고 템플릿 캐시도 초기화
     */
    @Scheduled(cron = "0 0 10 * * ?") // 한국 시간 아침 7시에 실행 (UTC 10시)
    public void resetEmailStatusAndTemplate() {
        try {
            // 이메일 상태 초기화
            System.out.println("[Scheduler] 한국 시간 아침 7시 - 이메일 상태 초기화 시작.");
            jdbcTemplate.update("UPDATE subscriber SET sent = FALSE WHERE sent = TRUE"); // 상태를 False로 초기화
            System.out.println("[Scheduler] 이메일 상태 초기화 완료.");

            // 템플릿 캐시 초기화
            emailProcessor.resetTemplateCache(); // 템플릿 캐시 초기화

            // Discord Webhook 전송 상태 초기화
            isWebhookSentToday.set(false); // 하루에 한 번만 전송되도록 상태 초기화
        } catch (Exception e) {
            System.err.println("이메일 상태 초기화 또는 템플릿 초기화 중 오류 발생: " + e.getMessage());
        }
    }
}