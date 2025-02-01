package com.example.batchservice.batch;

import com.example.batchservice.processor.EmailProcessor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class JobScheduler {

    private final JobLauncher jobLauncher;
    private final Job userNotificationJob;
    private final JdbcTemplate jdbcTemplate;
    private final EmailProcessor emailProcessor; // EmailProcessor를 주입받음
    private static boolean isRunning = false; // 실행 상태 변수 추가

    @Autowired
    public JobScheduler(JobLauncher jobLauncher, Job userNotificationJob, JdbcTemplate jdbcTemplate, EmailProcessor emailProcessor) {
        this.jobLauncher = jobLauncher;
        this.userNotificationJob = userNotificationJob;
        this.jdbcTemplate = jdbcTemplate;
        this.emailProcessor = emailProcessor; // 생성자에서 주입
    }

    /**
     *  매일 자정 실행 + 데이터가 남아 있으면 추가 실행 (중복 실행 방지)
     */
    @Scheduled(cron = "0 0/5 * * * ?") // 5분마다 실행
    public synchronized void scheduleJob() { //  동기화하여 중복 실행 방지
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
        } catch (Exception e) {
            System.err.println("이메일 상태 초기화 또는 템플릿 초기화 중 오류 발생: " + e.getMessage());
        }
    }
}
