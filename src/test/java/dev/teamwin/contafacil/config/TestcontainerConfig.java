package dev.teamwin.contafacil.config;


import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistrar;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainerConfig {

    public static final MySQLContainer<?> MYSQL = new MySQLContainer<>(
            DockerImageName.parse("mysql:8.0"))
            .withDatabaseName("contafacil_test")
            .withUsername("test")
            .withPassword("test");

    static {
        MYSQL.start();
    }


    @Bean
    public DynamicPropertyRegistrar dynamicPropertiesRegistrar(){
        return registry -> {
            registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
            registry.add("spring.datasource.username", MYSQL::getUsername);
            registry.add("spring.datasource.password", MYSQL::getPassword);
            registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        };
    }

}
