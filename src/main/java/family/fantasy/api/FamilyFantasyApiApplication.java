package family.fantasy.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableScheduling 
public class FamilyFantasyApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(FamilyFantasyApiApplication.class, args);
    }

    // 2. Creates a web client we can use anywhere in our app
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}