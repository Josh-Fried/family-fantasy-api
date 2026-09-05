package family.fantasy.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableScheduling 
@EnableCaching
public class FamilyFantasyApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(FamilyFantasyApiApplication.class, args);
    }

    // 2. Creates a web client we can use anywhere in our app
    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add((request, body, execution) -> {
            request.getHeaders().add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            request.getHeaders().add("Accept", "application/json, text/plain, */*");
            request.getHeaders().add("Accept-Language", "en-US,en;q=0.9");
            return execution.execute(request, body);
        });
        return restTemplate;
    }
}