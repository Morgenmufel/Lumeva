package lumeva.profileservice.infrastructure.adapter.out.cache;

import lumeva.profileservice.domain.model.UserProfile;
import lumeva.profileservice.infrastructure.adapter.out.cache.dto.ProfileCacheDto;
import lumeva.profileservice.infrastructure.adapter.out.cache.mapper.ProfileCacheMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.RedisTemplate;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RedisProfileAdapterTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private JsonMapper jsonMapper;

    @Mock
    private ProfileCacheMapper mapper;

    private RedisProfileAdapter cacheAdapter;

    @BeforeEach
    void setUp() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        cacheAdapter = new RedisProfileAdapter(redisTemplate, jsonMapper, mapper);

        Field prefixField = RedisProfileAdapter.class.getDeclaredField("keyPrefix");
        prefixField.setAccessible(true);
        prefixField.set(cacheAdapter, "profile:public:");
    }

    @Test
    @DisplayName("Повторный get() должен браться из L1 Caffeine без повторного вызова Redis")
    void shouldReturnFromL1CacheOnSecondCall() {
        UUID userId = UUID.randomUUID();
        String key = "profile:public:" + userId;
        Object rawRedisObj = new Object();
        ProfileCacheDto dto = new ProfileCacheDto();
        UserProfile domainProfile = UserProfile.createInitial(userId, "cache@lumeva.com");

        when(valueOperations.get(key)).thenReturn(rawRedisObj);
        when(jsonMapper.convertValue(rawRedisObj, ProfileCacheDto.class)).thenReturn(dto);
        when(mapper.toDomain(dto)).thenReturn(domainProfile);

        Optional<UserProfile> firstCall = cacheAdapter.get(userId);
        Optional<UserProfile> secondCall = cacheAdapter.get(userId);

        assertThat(firstCall).contains(domainProfile);
        assertThat(secondCall).contains(domainProfile);

        verify(valueOperations, times(1)).get(key);
    }
}