package org.project.infrastructure.cache;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.project.domain.ride.entities.RideRequest;
import org.project.domain.ride.value_object.RideRequestID;
import org.project.domain.shared.value_objects.DriverID;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

public class RideRequests {

    private static final Duration TTL = Duration.ofSeconds(300);

    private final RedisTemplate<String, RideRequest> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    public RideRequests(
            RedisTemplate<String, RideRequest> redisTemplate,
            StringRedisTemplate stringRedisTemplate
    ) {
        this.redisTemplate = redisTemplate;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /* ================= ENABLE CHECK ================= */

    public boolean isEnabled() {
        return redisTemplate != null && stringRedisTemplate != null;
    }

    public boolean isEmpty(DriverID driverID) {
        if (!isEnabled()) return true;
        return pageOf(driverID).isEmpty();
    }

    /* ================= CRUD ================= */

    public void put(DriverID driverID, RideRequest rideRequest) {
        if (!isEnabled()) return;

        String redisKey = key(driverID, rideRequest.id());
        redisTemplate.opsForValue().set(redisKey, rideRequest, TTL);
    }

    public Optional<RideRequest> get(DriverID driverID, RideRequestID rideRequestID) {
        if (!isEnabled()) return Optional.empty();

        return Optional.ofNullable(
                redisTemplate.opsForValue().get(key(driverID, rideRequestID))
        );
    }

    public Optional<RideRequest> del(DriverID driverID, RideRequestID rideRequestID) {
        if (!isEnabled()) return Optional.empty();

        String redisKey = key(driverID, rideRequestID);
        RideRequest value = redisTemplate.opsForValue().get(redisKey);
        redisTemplate.delete(redisKey);
        return Optional.ofNullable(value);
    }

    public List<RideRequest> pageOf(DriverID driverID) {
        if (!isEnabled()) return List.of();

        return stringRedisTemplate
                .keys(driverRequestsKey(driverID))
                .stream()
                .map(redisTemplate.opsForValue()::get)
                .filter(Objects::nonNull)
                .toList();
    }

    /* ================= KEYS ================= */

    private static String driverRequestsKey(DriverID driverID) {
        return "ride_request:{" + driverID.value() + "}:*";
    }

    private static String key(DriverID driverID, RideRequestID rideRequestID) {
        return "ride_request:{" + driverID.value() + "}:" + rideRequestID.value();
    }
}