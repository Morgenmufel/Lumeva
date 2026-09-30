package lumeva.profileservice.infrastructure.adapter.out.cache;




import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lumeva.profileservice.domain.model.UserProfile;
import lumeva.profileservice.domain.port.out.ProfileCachePort;
import lumeva.profileservice.infrastructure.adapter.out.cache.dto.ProfileCacheDto;
import lombok.RequiredArgsConstructor;
import lumeva.profileservice.infrastructure.adapter.out.cache.mapper.ProfileCacheMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.*;

@Component
@RequiredArgsConstructor
public class RedisProfileAdapter implements ProfileCachePort {

    @Value("${lumeva.cache.profile-prefix}")
    private String keyPrefix;

    private final RedisTemplate<String, Object> redisTemplate;
    private final JsonMapper jsonMapper;
    private final ProfileCacheMapper mapper;

    private final Cache<String, ProfileCacheDto> l1Cache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(Duration.ofSeconds(15))
            .build();

    @Override
    public Optional<UserProfile> get(UUID id) {
        String key = buildKey(id);
        ProfileCacheDto dto = l1Cache.get(key, k -> fetchFromRedis(k));

        if (dto == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(mapper.toDomain(dto));
    }

    @Override
    public void put(UserProfile profile) {
        String key = buildKey(profile.getId());
        ProfileCacheDto dto = mapper.toCacheDto(profile);

        l1Cache.put(key, dto);
        redisTemplate.opsForValue().set(key, dto);
    }

    @Override
    public void evict(UUID id) {
        String key = buildKey(id);
        l1Cache.invalidate(key);
        redisTemplate.delete(key);
    }

    @Override
    public Map<UUID, UserProfile> getAll(Set<UUID> ids) {
        if (ids == null || ids.isEmpty()) return Map.of();

        Map<UUID, UserProfile> result = new HashMap<>();
        Set<UUID> missingInL1 = new HashSet<>();

        for (UUID id : ids) {
            ProfileCacheDto cachedL1 = l1Cache.getIfPresent(buildKey(id));
            if (cachedL1 != null) {
                result.put(id, mapper.toDomain(cachedL1));
            } else {
                missingInL1.add(id);
            }
        }

        if (!missingInL1.isEmpty()) {
            List<String> keysToFetch = missingInL1.stream().map(this::buildKey).toList();
            List<Object> rawList = redisTemplate.opsForValue().multiGet(keysToFetch);

            if (rawList != null) {
                List<UUID> missingList = new ArrayList<>(missingInL1);
                for (int i = 0; i < missingList.size(); i++) {
                    Object raw = rawList.get(i);
                    if (raw != null) {
                        ProfileCacheDto dto = jsonMapper.convertValue(raw, ProfileCacheDto.class);
                        UUID id = missingList.get(i);

                        l1Cache.put(buildKey(id), dto);
                        result.put(id, mapper.toDomain(dto));
                    }
                }
            }
        }

        return result;
    }

    @Override
    public void putAll(Map<UUID, UserProfile> profiles) {
        if (profiles == null || profiles.isEmpty()) return;

        Map<String, Object> batchMap = new HashMap<>();
        profiles.forEach((id, profile) -> {
            String key = buildKey(id);
            ProfileCacheDto dto = mapper.toCacheDto(profile);

            l1Cache.put(key, dto);
            batchMap.put(key, dto);
        });

        redisTemplate.opsForValue().multiSet(batchMap);
    }

    private ProfileCacheDto fetchFromRedis(String key) {
        Object rawData = redisTemplate.opsForValue().get(key);
        if (rawData == null) return null;
        return jsonMapper.convertValue(rawData, ProfileCacheDto.class);
    }

    private String buildKey(UUID id) {
        return keyPrefix + id.toString();
    }
}