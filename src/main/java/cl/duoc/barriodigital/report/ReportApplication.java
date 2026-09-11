package cl.duoc.barriodigital.report;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "cl.duoc.barriodigital")
public class ReportApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReportApplication.class, args);
    }
}
