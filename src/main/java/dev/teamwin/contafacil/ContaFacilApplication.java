package dev.teamwin.contafacil;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class ContaFacilApplication {


    public static void main(String[] args) {
        SpringApplication.run(ContaFacilApplication.class, args);
    }

}
