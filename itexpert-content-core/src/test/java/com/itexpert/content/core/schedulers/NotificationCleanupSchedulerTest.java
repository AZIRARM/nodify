package com.itexpert.content.core.schedulers;

import com.itexpert.content.core.repositories.NotificationRepository;
import com.itexpert.content.lib.entities.Notification;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
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

    private Notification buildNotification(String userId, int index) {
        Notification notification = new Notification();
        notification.setId(UUID.randomUUID());
        notification.setUserId(userId);
        notification.setDate(Instant.now().minusSeconds(index).toEpochMilli());
        return notification;
    }

    private List<Notification> buildNotifications(String userId, int count) {
        List<Notification> list = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            list.add(buildNotification(userId, i));
        }
        return list;
    }

    @Test
    void cleanupNotifications_ShouldDeleteExcessNotifications_WhenSizeExceedsLimit() {
        List<Notification> notifications = buildNotifications("user-123", 1005);

        when(notificationRepository.findAll(any(Sort.class))).thenReturn(Flux.fromIterable(notifications));
        when(notificationRepository.deleteAllById(anyIterable())).thenReturn(Mono.empty());

        notificationCleanupScheduler.cleanupNotifications();

        verify(notificationRepository).findAll(any(Sort.class));
        verify(notificationRepository).deleteAllById(argThat(ids -> {
            List<UUID> idList = new ArrayList<>();
            ids.forEach(idList::add);
            return idList.size() == 5;
        }));
    }

    @Test
    void cleanupNotifications_ShouldNotDeleteAnything_WhenSizeEqualsMax() {
        List<Notification> notifications = buildNotifications("user-123", 1000);

        when(notificationRepository.findAll(any(Sort.class))).thenReturn(Flux.fromIterable(notifications));

        notificationCleanupScheduler.cleanupNotifications();

        verify(notificationRepository).findAll(any(Sort.class));
        verify(notificationRepository, never()).deleteAllById(anyIterable());
    }

    @Test
    void cleanupNotifications_ShouldNotDeleteAnything_WhenSizeIsLessThanMax() {
        List<Notification> notifications = buildNotifications("user-123", 250);

        when(notificationRepository.findAll(any(Sort.class))).thenReturn(Flux.fromIterable(notifications));

        notificationCleanupScheduler.cleanupNotifications();

        verify(notificationRepository).findAll(any(Sort.class));
        verify(notificationRepository, never()).deleteAllById(anyIterable());
    }

    @Test
    void cleanupNotifications_ShouldHandleMultipleUsers_Correctly() {
        List<Notification> combined = new ArrayList<>();
        combined.addAll(buildNotifications("user-111", 1003)); // 3 à supprimer
        combined.addAll(buildNotifications("user-222", 500));  // 0 à supprimer
        combined.addAll(buildNotifications("user-333", 1002)); // 2 à supprimer

        when(notificationRepository.findAll(any(Sort.class))).thenReturn(Flux.fromIterable(combined));
        when(notificationRepository.deleteAllById(anyIterable())).thenReturn(Mono.empty());

        notificationCleanupScheduler.cleanupNotifications();

        verify(notificationRepository).findAll(any(Sort.class));
        verify(notificationRepository, times(2)).deleteAllById(anyIterable());
    }

    @Test
    void cleanupNotifications_ShouldDoNothing_WhenNoNotificationsFound() {
        when(notificationRepository.findAll(any(Sort.class))).thenReturn(Flux.empty());

        notificationCleanupScheduler.cleanupNotifications();

        verify(notificationRepository).findAll(any(Sort.class));
        verify(notificationRepository, never()).deleteAllById(anyIterable());
    }

    @Test
    void cleanupNotifications_ShouldHandleError_WhenFindAllFails() {
        when(notificationRepository.findAll(any(Sort.class)))
                .thenReturn(Flux.error(new RuntimeException("MongoDB connection failed")));

        notificationCleanupScheduler.cleanupNotifications();

        verify(notificationRepository).findAll(any(Sort.class));
        verify(notificationRepository, never()).deleteAllById(anyIterable());
    }

    @Test
    void cleanupNotifications_ShouldHandleError_WhenDeleteFails() {
        List<Notification> notifications = buildNotifications("user-123", 1002);

        when(notificationRepository.findAll(any(Sort.class))).thenReturn(Flux.fromIterable(notifications));
        when(notificationRepository.deleteAllById(anyIterable()))
                .thenReturn(Mono.error(new RuntimeException("Delete failed")));

        notificationCleanupScheduler.cleanupNotifications();

        verify(notificationRepository).findAll(any(Sort.class));
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