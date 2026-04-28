package com.example.aitourism;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

@SpringBootApplication
public class AiTourismApplication {
    public static void main(String[] args) {
        loadEnv();
        SpringApplication.run(AiTourismApplication.class, args);
    }

    private static void loadEnv() {
        try {
            Path envPath = Paths.get(".env");
            if (!Files.exists(envPath)) {
                return;
            }
            Properties props = new Properties();
            props.load(Files.newBufferedReader(envPath));
            props.forEach((key, value) -> {
                String k = (String) key;
                if (System.getProperty(k) == null) {
                    System.setProperty(k, (String) value);
                }
            });
        } catch (IOException ignored) {
        }
    }
}
