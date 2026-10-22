package com.example.orderservice.client;

import com.example.orderservice.exception.ServiceUnavailableException;
import com.example.orderservice.exception.UserNotFoundException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

/**
 * Talks to USER-SERVICE directly (service-to-service, NOT through the gateway).
 * The host name "USER-SERVICE" is resolved through Eureka - no hardcoded host/port.
 */
@Slf4j
@Component
//@RequiredArgsConstructor
public class UserClient {

    private final RestClient restClient;

    // Spring injects the @LoadBalanced builder we created in RestClientConfig
    public UserClient(RestClient.Builder loadBalancedBuilder) {
        this.restClient = loadBalancedBuilder.baseUrl("http://USER-SERVICE").build();
    }

    /**
     * Safety nets around this method (outer to inner):
     *
     * 1. @Retry          - tries again if the call fails (up to 3 attempts).
     *                      fallbackMethod = "userServiceFallback" means: when Retry finally
     *                      gives up, or the circuit breaker blocks the call, Spring calls the
     *                      fallback method below instead of letting the raw exception escape.
     * 2. @CircuitBreaker - counts failures; opens when too many calls fail and blocks further calls.
     *
     * Which fallback runs? Resilience4j looks at the exception that came out and picks the
     * fallback method whose last parameter matches that exception type:
     *   - CallNotPermittedException  -> circuit is OPEN, call was blocked
     *   - ServiceUnavailableException -> all retry attempts failed
     *   - UserNotFoundException      -> NO matching fallback, so it passes through untouched
     *                                   and still becomes a normal 404.
     */
    @Retry(name = "userService", fallbackMethod = "userServiceFallback")
    @CircuitBreaker(name = "userService")
    public void validateUserExists(Long userId) {
        log.info("Calling USER-SERVICE via RestClient to validate userId={}", userId);
        try {
            restClient.get()
                    .uri("/api/users/{id}", userId)
                    .retrieve()
                    .onStatus(status -> status.value() == 404, (req, res) -> {
                        // 404 = the user really does not exist. Not a service failure,
                        // so no retry, no circuit-breaker failure, no fallback (see application.yml).
                        log.warn("User not found in USER-SERVICE, userId={}", userId);
                        throw new UserNotFoundException("User not found with id " + userId);
                    })
                    .onStatus(status -> status.is5xxServerError(), (req, res) -> {
                        // 5xx = USER-SERVICE has a problem -> counts as a failure
                        log.error("USER-SERVICE returned status {}", res.getStatusCode());
                        throw new ServiceUnavailableException("User service returned an error");
                    })
                    .toBodilessEntity();
        } catch (ResourceAccessException | IllegalStateException ex) {
            // connection refused, timeout, or no instance in Eureka -> counts as a failure
            log.error("USER-SERVICE is unreachable: {}", ex.getMessage());
            throw new ServiceUnavailableException("User service is currently unavailable. Please try again later.");
        }
    }

    // ---------------------------------------------------------------------------------
    // FALLBACK METHODS
    // Rules: same class, same name as in fallbackMethod, same parameters as the original
    // method (Long userId) plus ONE extra parameter at the end: the exception it handles.
    // The return type must match the original method (void here).
    //
    // For "does this user exist?" there is no sensible fake answer (pretending the user
    // exists could create orders for people who do not exist). So our plan B is to fail
    // cleanly and quickly with a clear 503 message.
    // ---------------------------------------------------------------------------------

    /** Runs when the circuit is OPEN: the call was blocked and USER-SERVICE was never contacted. */
    public void userServiceFallback(Long userId, CallNotPermittedException ex) {
        log.warn("Fallback: circuit is OPEN, call to USER-SERVICE blocked for userId={}", userId);
        throw new ServiceUnavailableException(
                "User service is temporarily unavailable (too many recent failures). Please try again in a minute.");
    }

    /** Runs when all retry attempts failed (the circuit was still closed). */
    public void userServiceFallback(Long userId, ServiceUnavailableException ex) {
        log.error("Fallback: all retry attempts failed for userId={}: {}", userId, ex.getMessage());
        throw new ServiceUnavailableException("User service is currently unavailable. Please try again later.");
    }

//    private final UserFeignClient userFeignClient;
//
//    /** Returns normally if the user exists; throws otherwise. */
//    public void validateUserExists(Long userId) {
//
//        log.info("Calling USER-SERVICE via Feign to validate userId={}", userId);
//        try {
//            userFeignClient.getUserById(userId);
//        } catch (FeignException.NotFound ex) {
//
//            // USER-SERVICE answered 404 -> the user does not exist
//            log.warn("User not found in USER-SERVICE, userId={}", userId);
//            throw new UserNotFoundException("User not found with id " + userId);
//        } catch (FeignException ex) {
//
//            // Everything else: connection refused, timeout, no instance in Eureka (503), 500 ...
//            log.error("USER-SERVICE call failed, status={}, message={}", ex.status(), ex.getMessage());
//            throw new ServiceUnavailableException("User service is currently unavailable. Please try again later.");
//        }
//    }
}
