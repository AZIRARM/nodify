package com.itexpert.content.core.endpoints;

import com.itexpert.content.core.handlers.AccessRoleHandler;
import com.itexpert.content.core.handlers.NotificationHandler;
import com.itexpert.content.core.mappers.AccessRoleMapper;
import com.itexpert.content.core.models.AccessRole;
import com.itexpert.content.core.repositories.AccessRoleRepository;
import com.itexpert.content.lib.enums.NotificationEnum;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

@Slf4j
public class AccessRoleHandlerTest {

    private final AccessRoleRepository mockAccessRoleRepository = mock(AccessRoleRepository.class);
    private final AccessRoleMapper mockAccessRoleMapper = mock(AccessRoleMapper.class);
    private final NotificationHandler mockNotificationHandler = mock(NotificationHandler.class);

    private AccessRoleHandler accessRoleHandler;

    @BeforeEach
    void setUp() {
        this.accessRoleHandler = new AccessRoleHandler(
                mockAccessRoleRepository,
                mockAccessRoleMapper,
                mockNotificationHandler);
    }

    // Tests for findAll()
    @Test
    public void testFindAll() {
        List<com.itexpert.content.lib.entities.AccessRole> accessRoles = List.of(
                new com.itexpert.content.lib.entities.AccessRole(), new com.itexpert.content.lib.entities.AccessRole());
        when(mockAccessRoleRepository.findAll()).thenReturn(Flux.fromIterable(accessRoles));

        Flux<AccessRole> result = accessRoleHandler.findAll();

        result.subscribe(role -> log.info("Found role: {}", role));
        verify(mockAccessRoleRepository, times(1)).findAll();
        verify(mockAccessRoleMapper, times(1)).fromEntity(any());
    }

    // Tests for findById()
    @Test
    public void testFindById() {
        UUID uuid = UUID.randomUUID();
        com.itexpert.content.lib.entities.AccessRole accessRole = new com.itexpert.content.lib.entities.AccessRole();

        when(mockAccessRoleRepository.findById(uuid)).thenReturn(Mono.just(accessRole));

        Mono<AccessRole> result = accessRoleHandler.findById(uuid);

        result.subscribe(role -> log.info("Found role: {}", role));
        verify(mockAccessRoleRepository, times(1)).findById(uuid);
        verify(mockAccessRoleMapper, times(1)).fromEntity(any());
    }

    // Tests for save()
    @Test
    public void testSave() {
        AccessRole accessRole = new AccessRole();
        UUID uuid = UUID.randomUUID();
        accessRole.setId(uuid);

        when(mockAccessRoleRepository.findByCode(any()))
                .thenReturn(Mono.just(new com.itexpert.content.lib.entities.AccessRole()));
        when(mockAccessRoleRepository.save(any()))
                .thenReturn(Mono.just(new com.itexpert.content.lib.entities.AccessRole()));
        when(mockAccessRoleMapper.fromModel(any(AccessRole.class)))
                .thenReturn(new com.itexpert.content.lib.entities.AccessRole());

        Mono<AccessRole> result = accessRoleHandler.save(accessRole);

        result.subscribe(role -> log.info("Saved role: {}", role));
        verify(mockAccessRoleRepository, times(1)).findByCode(any());
        verify(mockAccessRoleRepository, times(1)).save(any(com.itexpert.content.lib.entities.AccessRole.class));
        verify(mockAccessRoleMapper, times(1)).fromEntity(any());
        verify(mockNotificationHandler, times(0)).create(any(), any(), any(), any(),
                any(),
                any(), any());
    }

    // Tests for delete()
    @Test
    public void testDelete() {
        UUID uuid = UUID.randomUUID();
        com.itexpert.content.lib.entities.AccessRole accessRole = new com.itexpert.content.lib.entities.AccessRole();
        AccessRole accessRoleModel = new AccessRole();
        accessRoleModel.setId(uuid);

        when(mockAccessRoleRepository.findById(uuid)).thenReturn(Mono.just(accessRole));
        when(mockAccessRoleMapper.fromEntity(any()))
                .thenReturn(accessRoleModel);

        Mono<Boolean> result = accessRoleHandler.delete(uuid);

        result.subscribe(success -> log.info("Deleted: {}", success));
        verify(mockAccessRoleRepository, times(1)).findById(uuid);
        verify(mockNotificationHandler, times(1)).create(any(), any(), any(),
                any(), any(), any(),
                any());
    }

    // Tests for findByCode()
    @Test
    public void testFindByCode() {
        String code = "test-code";
        com.itexpert.content.lib.entities.AccessRole accessRole = new com.itexpert.content.lib.entities.AccessRole();

        when(mockAccessRoleRepository.findByCode(code)).thenReturn(Mono.just(accessRole));

        Mono<AccessRole> result = accessRoleHandler.findByCode(code);

        result.subscribe(role -> log.info("Found role by code: {}", role));
        verify(mockAccessRoleRepository, times(1)).findByCode(code);
        verify(mockAccessRoleMapper, times(1)).fromEntity(any());
    }

    // Tests for saveAll()
    @Test
    public void testSaveAll() {
        List<AccessRole> accessRoles = List.of(
                new AccessRole(), new AccessRole());

        when(mockAccessRoleRepository.save(any(com.itexpert.content.lib.entities.AccessRole.class)))
                .thenReturn(Mono.just(new com.itexpert.content.lib.entities.AccessRole()));
        when(mockAccessRoleRepository.findByCode(any()))
                .thenReturn(Mono.just(new com.itexpert.content.lib.entities.AccessRole()));
        when(mockAccessRoleMapper.fromModel(any()))
                .thenReturn(new com.itexpert.content.lib.entities.AccessRole());
        when(mockAccessRoleMapper.fromEntity(any()))
                .thenReturn(new AccessRole());

        Flux<AccessRole> result = accessRoleHandler.saveAll(accessRoles);

        result.subscribe(role -> log.info("Saved role: {}", role));
        verify(mockNotificationHandler, times(1)).create(any(),
                any(), any(),
                any(), any(), any(),
                any());
    }

    // Tests for notify()
    @Test
    public void testNotify() {
        AccessRole accessRole = new AccessRole();
        NotificationEnum type = NotificationEnum.CREATION_OR_UPDATE;

        Mono<AccessRole> result = accessRoleHandler.notify(accessRole, type);

        result.subscribe(role -> log.info("Notified role: {}", role));
        verify(mockNotificationHandler, times(1)).create(any(NotificationEnum.class), any(), any(),
                any(), any(), any(),
                anyBoolean());
    }
}