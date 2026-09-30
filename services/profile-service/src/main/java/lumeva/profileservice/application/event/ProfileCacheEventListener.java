package lumeva.profileservice.application.event;

import lombok.RequiredArgsConstructor;
import lumeva.profileservice.domain.port.out.ProfileCachePort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class ProfileCacheEventListener {

    private final ProfileCachePort profileCache;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCacheSync(ProfileCacheSyncEvent event) {
        profileCache.evict(event.userId());
    }
}
