package dev.teamwin.contafacil;


import dev.teamwin.contafacil.config.TestcontainerConfig;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@ContextConfiguration(classes = {TestcontainerConfig.class})
public class BaseIntegrationTest {

}
