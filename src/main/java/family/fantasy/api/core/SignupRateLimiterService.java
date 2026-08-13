package family.fantasy.api.core;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Service;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SignupRateLimiterService {
    
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    public Bucket resolveBucket(String ip) {
        return cache.computeIfAbsent(ip, this::newBucket);
    }

    private Bucket newBucket(String ip) {
        // Allows 3 signups maximum per 1 hour from the same IP address
        Bandwidth limit = Bandwidth.builder()
            .capacity(3)
            .refillIntervally(3, Duration.ofHours(1))
            .build();
            
        return Bucket.builder().addLimit(limit).build();
    }
}