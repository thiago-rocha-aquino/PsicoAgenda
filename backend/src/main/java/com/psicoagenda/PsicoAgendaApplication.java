package com.psicoagenda;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PsicoAgendaApplication {

    public static void main(String[] args) {
        SpringApplication.run(PsicoAgendaApplication.class, args);
    }
}
