package de.unistuttgart.iste.meitrex.gamification_service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Arrays;

/**
 * This is the entry point of the application.
 */
@SpringBootApplication
@Slf4j
public class GamificationApplication {

    public static void main(String[] args) {
        Arrays.stream(args).map(arg -> "Received argument: " + arg).forEach(log::info);
        SpringApplication.run(GamificationApplication.class, args);
    }

}
