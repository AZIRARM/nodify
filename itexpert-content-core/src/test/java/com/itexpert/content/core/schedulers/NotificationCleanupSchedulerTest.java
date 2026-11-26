package com.itexpert.content.core.schedulers;

import com.itexpert.content.core.repositories.NotificationRepository;
import com.itexpert.content.lib.entities.Notification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationCleanupSchedulerTest {

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationCleanupScheduler notificationCleanupScheduler;

    private Notification buildNotification(int index) {
        Notification notification = new Notification();
        notification.setId(UUID.randomUUID());
        notification.setUserId("user-123");
        notification.setDate(Instant.now().minusSeconds(index).toEpochMilli());
        return notification;
    }

    private List<Notification> buildNotifications(int count) {
        List<Notification> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(buildNotification(i));
        }
        return list;
    }

    @Test
    void cleanupNotifications_ShouldDeleteExcessNotifications_WhenTotalExceedsLimit() {
        when(notificationRepository.count()).thenReturn(Mono.just(1005L));
        when(notificationRepository.findOldestNotifications(any(Pageable.class)))
                .thenReturn(Flux.fromIterable(buildNotifications(5)));
        when(notificationRepository.deleteAllById(anyIterable())).thenReturn(Mono.empty());

        notificationCleanupScheduler.cleanupNotifications();

        verify(notificationRepository).count();
        verify(notificationRepository).findOldestNotifications(any(Pageable.class));
        verify(notificationRepository).deleteAllById(argThat(ids -> {
            List<UUID> idList = new ArrayList<>();
            ids.forEach(idList::add);
            return idList.size() == 5;
        }));
    }

    @Test
    void cleanupNotifications_ShouldNotDeleteAnything_WhenTotalEqualsMax() {
        when(notificationRepository.count()).thenReturn(Mono.just(1000L));

        notificationCleanupScheduler.cleanupNotifications();

        verify(notificationRepository).count();
        verify(notificationRepository, never()).findOldestNotifications(any(Pageable.class));
        verify(notificationRepository, never()).deleteAllById(anyIterable());
    }

    @Test
    void cleanupNotifications_ShouldNotDeleteAnything_WhenTotalIsLessThanMax() {
        when(notificationRepository.count()).thenReturn(Mono.just(250L));

        notificationCleanupScheduler.cleanupNotifications();

        verify(notificationRepository).count();
        verify(notificationRepository, never()).findOldestNotifications(any(Pageable.class));
        verify(notificationRepository, never()).deleteAllById(anyIterable());
    }

    @Test
    void cleanupNotifications_ShouldNotDelete_WhenNoOldNotificationsFound() {
        when(notificationRepository.count()).thenReturn(Mono.just(1005L));
        when(notificationRepository.findOldestNotifications(any(Pageable.class)))
                .thenReturn(Flux.empty());

        notificationCleanupScheduler.cleanupNotifications();

        verify(notificationRepository).count();
        verify(notificationRepository).findOldestNotifications(any(Pageable.class));
        verify(notificationRepository, never()).deleteAllById(anyIterable());
    }

    @Test
    void cleanupNotifications_ShouldHandleEmptyCount() {
        when(notificationRepository.count()).thenReturn(Mono.empty());

        notificationCleanupScheduler.cleanupNotifications();

        verify(notificationRepository).count();
        verify(notificationRepository, never()).findOldestNotifications(any(Pageable.class));
        verify(notificationRepository, never()).deleteAllById(anyIterable());
    }

    @Test
    void cleanupNotifications_ShouldHandleError_WhenCountFails() {
        when(notificationRepository.count())
                .thenReturn(Mono.error(new RuntimeException("MongoDB connection failed")));

        notificationCleanupScheduler.cleanupNotifications();

        verify(notificationRepository).count();
        verify(notificationRepository, never()).findOldestNotifications(any(Pageable.class));
        verify(notificationRepository, never()).deleteAllById(anyIterable());
    }

    @Test
    void cleanupNotifications_ShouldHandleError_WhenFindOldestFails() {
        when(notificationRepository.count()).thenReturn(Mono.just(1005L));
        when(notificationRepository.findOldestNotifications(any(Pageable.class)))
                .thenReturn(Flux.error(new RuntimeException("Find failed")));

        notificationCleanupScheduler.cleanupNotifications();

        verify(notificationRepository).count();
        verify(notificationRepository).findOldestNotifications(any(Pageable.class));
        verify(notificationRepository, never()).deleteAllById(anyIterable());
    }

    @Test
    void cleanupNotifications_ShouldHandleError_WhenDeleteFails() {
        when(notificationRepository.count()).thenReturn(Mono.just(1002L));
        when(notificationRepository.findOldestNotifications(any(Pageable.class)))
                .thenReturn(Flux.fromIterable(buildNotifications(2)));
        when(notificationRepository.deleteAllById(anyIterable()))
                .thenReturn(Mono.error(new RuntimeException("Delete failed")));

        notificationCleanupScheduler.cleanupNotifications();

        verify(notificationRepository).count();
        verify(notificationRepository).findOldestNotifications(any(Pageable.class));
        verify(notificationRepository).deleteAllById(anyIterable());
    }

    @Test
    void cleanupNotifications_ShouldHaveCorrectScheduledAnnotation() throws NoSuchMethodException {
        var method = NotificationCleanupScheduler.class.getMethod("cleanupNotifications");
        Scheduled scheduledAnnotation = method.getAnnotation(Scheduled.class);

        assertThat(scheduledAnnotation).isNotNull();
        assertThat(scheduledAnnotation.fixedDelay()).isEqualTo(60 * 60 * 1000);
    }
}