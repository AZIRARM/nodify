package com.itexpert.content.core.schedulers;

import com.itexpert.content.core.entities.ResourceLock;
import com.mongodb.client.result.DeleteResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LockCleanerSchedulerTest {

    @Mock
    private ReactiveMongoTemplate mongoTemplate;

    @InjectMocks
    private LockCleanerScheduler lockCleanerScheduler;

    @Test
    void cleanExpiredLocks_ShouldDeleteExpiredLocks() {
        DeleteResult deleted = DeleteResult.acknowledged(2L);
        when(mongoTemplate.remove(any(Query.class), eq(ResourceLock.class)))
                .thenReturn(Mono.just(deleted));

        lockCleanerScheduler.cleanExpiredLocks();

        verify(mongoTemplate).remove(any(Query.class), eq(ResourceLock.class));
    }

    @Test
    void cleanExpiredLocks_ShouldDoNothing_WhenNoExpiredLocksFound() {
        DeleteResult noneDeleted = DeleteResult.acknowledged(0L);
        when(mongoTemplate.remove(any(Query.class), eq(ResourceLock.class)))
                .thenReturn(Mono.just(noneDeleted));

        lockCleanerScheduler.cleanExpiredLocks();

        verify(mongoTemplate).remove(any(Query.class), eq(ResourceLock.class));
    }

    @Test
    void cleanExpiredLocks_ShouldHandleError_Gracefully() {
        when(mongoTemplate.remove(any(Query.class), eq(ResourceLock.class)))
                .thenReturn(Mono.error(new RuntimeException("MongoDB connection failed")));

        lockCleanerScheduler.cleanExpiredLocks();

        verify(mongoTemplate).remove(any(Query.class), eq(ResourceLock.class));
    }
}