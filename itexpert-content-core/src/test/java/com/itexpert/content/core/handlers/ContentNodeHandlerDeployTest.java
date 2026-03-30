package com.itexpert.content.core.handlers;

import com.itexpert.content.core.helpers.ContentHelper;
import com.itexpert.content.core.helpers.ContentNodeSlugHelper;
import com.itexpert.content.core.mappers.ContentNodeMapper;
import com.itexpert.content.core.mappers.NodeMapper;
import com.itexpert.content.core.repositories.ContentNodeRepository;
import com.itexpert.content.core.repositories.NodeRepository;
import com.itexpert.content.lib.entities.ContentNode;
import com.itexpert.content.lib.entities.Node;
import com.itexpert.content.lib.enums.StatusEnum;
import com.itexpert.content.lib.models.Notification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentNodeHandlerDeployTest {

        @Mock
        private ContentNodeRepository contentNodeRepository;
        @Mock
        private NodeRepository nodeRepository;
        @Mock
        private UserHandler userHandler;
        @Mock
        private NotificationHandler notificationHandler;
        @Mock
        private DataHandler dataHandler;
        @Mock
        private ContentHelper contentHelper;
        @Mock
        private ContentNodeSlugHelper contentNodeSlugHelper;
        @Mock
        private ContentNodeMapper contentNodeMapper;
        @Mock
        private NodeMapper nodeMapper;

        private ContentNodeHandler contentNodeHandler;

        private com.itexpert.content.lib.models.ContentNode snapshotModel;
        private ContentNode snapshotEntity;
        private Notification mockNotification;

        @BeforeEach
        void setUp() {
                contentNodeHandler = new ContentNodeHandler(
                                contentNodeRepository,
                                contentNodeMapper,
                                nodeRepository,
                                userHandler,
                                notificationHandler,
                                dataHandler,
                                contentHelper,
                                contentNodeSlugHelper);

                mockNotification = Notification.builder()
                                .id(UUID.randomUUID())
                                .user("Admin")
                                .type("CONTENT_NODE")
                                .build();

                // 🔧 FIX: lenient() pour les stubs communs non utilisés dans tous les tests
                lenient().when(notificationHandler.create(any(), any(), any(), any(), any(), any(), any()))
                                .thenReturn(Mono.just(mockNotification));

                lenient().when(contentNodeMapper.fromEntity(any(ContentNode.class)))
                                .thenAnswer(invocation -> {
                                        ContentNode entity = invocation.getArgument(0);
                                        return entity != null ? cloneToModel(entity) : null;
                                });
                lenient().when(contentNodeMapper.fromModel(any(com.itexpert.content.lib.models.ContentNode.class)))
                                .thenAnswer(invocation -> {
                                        com.itexpert.content.lib.models.ContentNode model = invocation.getArgument(0);
                                        return model != null ? cloneToEntity(model) : null;
                                });

                snapshotModel = createSnapshotModel("NODE-DEV", "PARENT-DEV", "1", "my-slug-dev");
                snapshotEntity = cloneToEntity(snapshotModel);
        }

        // ========================================================================
        // 🎯 TESTS PRINCIPAUX
        // ========================================================================

        @Nested
        @DisplayName("deployContent - Cas nominaux")
        class DeployContentNominalTests {

                @Test
                @DisplayName("Déploiement avec contenu PROD existant → incrémentation version + archivage")
                void deployContent_withExistingProdContent_shouldIncrementVersionAndArchive() {
                        ContentNode existingProd = cloneToEntity(
                                        createSnapshotModel("NODE-PROD", "PARENT-PROD", "1", "slug-prod"));

                        when(contentNodeRepository.findByCodeAndStatus(eq("NODE-DEV"), eq(StatusEnum.SNAPSHOT.name())))
                                        .thenReturn(Mono.just(snapshotEntity));
                        when(contentNodeRepository.findByCodeAndStatus(eq("NODE-PROD"), eq(StatusEnum.SNAPSHOT.name())))
                                        .thenReturn(Mono.just(existingProd));
                        when(nodeRepository.findByCodeAndStatus(eq("PARENT-PROD"), eq(StatusEnum.SNAPSHOT.name())))
                                        .thenReturn(Mono.just(new Node()));
                        when(contentNodeRepository.save(any(ContentNode.class)))
                                        .thenAnswer(inv -> Mono.just(inv.getArgument(0)));
                        when(contentNodeSlugHelper.update(any()))
                                        .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

                        StepVerifier.create(contentNodeHandler.deployContent("NODE-DEV", "PROD"))
                                        .assertNext(result -> {
                                                assertThat(result.getCode()).isEqualTo("NODE-PROD");
                                                assertThat(result.getVersion()).isEqualTo("2");
                                                assertThat(result.getStatus()).isEqualTo(StatusEnum.SNAPSHOT);
                                                assertThat(result.getSlug()).isEqualTo("slug-prod");
                                        })
                                        .verifyComplete();

                        verify(contentNodeRepository, times(2)).save(any(ContentNode.class));
                        verify(contentNodeSlugHelper).update(any());
                }

                @Test
                @DisplayName("Déploiement sans contenu PROD existant → création version 0")
                void deployContent_withoutExistingProdContent_shouldCreateVersionZero() {
                        when(contentNodeRepository.findByCodeAndStatus(eq("NODE-DEV"), eq(StatusEnum.SNAPSHOT.name())))
                                        .thenReturn(Mono.just(snapshotEntity));
                        when(contentNodeRepository.findByCodeAndStatus(eq("NODE-PROD"), eq(StatusEnum.SNAPSHOT.name())))
                                        .thenReturn(Mono.empty());
                        when(nodeRepository.findByCodeAndStatus(eq("PARENT-PROD"), eq(StatusEnum.SNAPSHOT.name())))
                                        .thenReturn(Mono.just(new Node()));
                        when(contentNodeRepository.save(any(ContentNode.class)))
                                        .thenAnswer(inv -> Mono.just(inv.getArgument(0)));
                        when(contentNodeSlugHelper.update(any()))
                                        .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

                        StepVerifier.create(contentNodeHandler.deployContent("NODE-DEV", "PROD"))
                                        .assertNext(result -> {
                                                assertThat(result.getCode()).isEqualTo("NODE-PROD");
                                                assertThat(result.getVersion()).isEqualTo("0");
                                                assertThat(result.getStatus()).isEqualTo(StatusEnum.SNAPSHOT);
                                                assertThat(result.getParentCode()).isEqualTo("PARENT-PROD");
                                        })
                                        .verifyComplete();

                        verify(contentNodeRepository, times(1)).save(any(ContentNode.class));
                }

                @Test
                @DisplayName("Déploiement avec parent non trouvé → fallback sur environmentCode")
                void deployContent_withParentNotFound_shouldFallbackToEnvironmentCode() {
                        // 🔧 FIX CRITIQUE: Le code appelle nodeRepository 2 fois avec des codes
                        // différents!
                        // 1ère fois: "PARENT-PROD" (n'existe pas)
                        // 2ème fois: "PROD" (après fallback, n'existe pas non plus)

                        when(contentNodeRepository.findByCodeAndStatus(eq("NODE-DEV"), eq(StatusEnum.SNAPSHOT.name())))
                                        .thenReturn(Mono.just(snapshotEntity));
                        when(contentNodeRepository.findByCodeAndStatus(eq("NODE-PROD"), eq(StatusEnum.SNAPSHOT.name())))
                                        .thenReturn(Mono.empty());

                        // 🔧 FIX #1: Mock pour TOUS les appels à findByCodeAndStatus sur nodeRepository
                        // Utiliser anyString() pour couvrir "PARENT-PROD" ET "PROD"
                        when(nodeRepository.findByCodeAndStatus(anyString(), eq(StatusEnum.SNAPSHOT.name())))
                                        .thenReturn(Mono.empty());

                        when(contentNodeRepository.save(any(ContentNode.class)))
                                        .thenAnswer(inv -> Mono.just(inv.getArgument(0)));
                        when(contentNodeSlugHelper.update(any()))
                                        .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

                        StepVerifier.create(contentNodeHandler.deployContent("NODE-DEV", "PROD"))
                                        .assertNext(result -> {
                                                assertThat(result.getCode()).isEqualTo("NODE-PROD");
                                                assertThat(result.getParentCode()).isEqualTo("PROD");
                                                assertThat(result.getParentCodeOrigin()).isEqualTo("PROD");
                                                assertThat(result.getVersion()).isEqualTo("0");
                                        })
                                        .verifyComplete();
                }
        }

        // ========================================================================
        // 🐛 TESTS - Gestion des Slugs
        // ========================================================================

        @Nested
        @DisplayName("deployContent - Gestion des slugs")
        class DeployContentSlugTests {

                @Test
                @DisplayName("Slug existant sur PROD → slug conservé")
                void deployContent_withExistingSlugOnProd_shouldKeepSlug() {
                        ContentNode existingProd = cloneToEntity(
                                        createSnapshotModel("NODE-PROD", "PARENT-PROD", "1", "existing-slug"));

                        when(contentNodeRepository.findByCodeAndStatus(eq("NODE-DEV"), eq(StatusEnum.SNAPSHOT.name())))
                                        .thenReturn(Mono.just(snapshotEntity));
                        when(contentNodeRepository.findByCodeAndStatus(eq("NODE-PROD"), eq(StatusEnum.SNAPSHOT.name())))
                                        .thenReturn(Mono.just(existingProd));
                        when(nodeRepository.findByCodeAndStatus(eq("PARENT-PROD"), eq(StatusEnum.SNAPSHOT.name())))
                                        .thenReturn(Mono.just(new Node()));
                        when(contentNodeRepository.save(any(ContentNode.class)))
                                        .thenAnswer(inv -> Mono.just(inv.getArgument(0)));
                        when(contentNodeSlugHelper.update(any()))
                                        .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

                        StepVerifier.create(contentNodeHandler.deployContent("NODE-DEV", "PROD"))
                                        .assertNext(result -> assertThat(result.getSlug()).isEqualTo("existing-slug"))
                                        .verifyComplete();
                }

                @Test
                @DisplayName("Slug null sur DEV et PROD → slug reste null après update")
                void deployContent_withNullSlugs_shouldHandleNullSlug() {
                        com.itexpert.content.lib.models.ContentNode modelNoSlug = createSnapshotModel("NODE-DEV",
                                        "PARENT-DEV", "1", null);
                        ContentNode entityNoSlug = cloneToEntity(modelNoSlug);
                        ContentNode existingProdNoSlug = cloneToEntity(
                                        createSnapshotModel("NODE-PROD", "PARENT-PROD", "1", null));

                        when(contentNodeRepository.findByCodeAndStatus(eq("NODE-DEV"), eq(StatusEnum.SNAPSHOT.name())))
                                        .thenReturn(Mono.just(entityNoSlug));
                        when(contentNodeRepository.findByCodeAndStatus(eq("NODE-PROD"), eq(StatusEnum.SNAPSHOT.name())))
                                        .thenReturn(Mono.just(existingProdNoSlug));
                        when(nodeRepository.findByCodeAndStatus(eq("PARENT-PROD"), eq(StatusEnum.SNAPSHOT.name())))
                                        .thenReturn(Mono.just(new Node()));
                        when(contentNodeRepository.save(any(ContentNode.class)))
                                        .thenAnswer(inv -> Mono.just(inv.getArgument(0)));
                        when(contentNodeSlugHelper.update(any()))
                                        .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

                        StepVerifier.create(contentNodeHandler.deployContent("NODE-DEV", "PROD"))
                                        .assertNext(result -> assertThat(result.getSlug()).isNull())
                                        .verifyComplete();
                }

                @Test
                @DisplayName("contentNodeSlugHelper.update génère un nouveau slug → slug mis à jour")
                void deployContent_whenSlugHelperGeneratesNewSlug_shouldUseNewSlug() {
                        when(contentNodeRepository.findByCodeAndStatus(eq("NODE-DEV"), eq(StatusEnum.SNAPSHOT.name())))
                                        .thenReturn(Mono.just(snapshotEntity));
                        when(contentNodeRepository.findByCodeAndStatus(eq("NODE-PROD"), eq(StatusEnum.SNAPSHOT.name())))
                                        .thenReturn(Mono.empty());
                        when(nodeRepository.findByCodeAndStatus(eq("PARENT-PROD"), eq(StatusEnum.SNAPSHOT.name())))
                                        .thenReturn(Mono.just(new Node()));
                        when(contentNodeRepository.save(any(ContentNode.class)))
                                        .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

                        when(contentNodeSlugHelper.update(any()))
                                        .thenAnswer(inv -> {
                                                com.itexpert.content.lib.models.ContentNode model = inv.getArgument(0);
                                                model.setSlug("auto-generated-slug-prod");
                                                return Mono.just(model);
                                        });

                        StepVerifier.create(contentNodeHandler.deployContent("NODE-DEV", "PROD"))
                                        .assertNext(result -> assertThat(result.getSlug())
                                                        .isEqualTo("auto-generated-slug-prod"))
                                        .verifyComplete();
                }
        }

        // ========================================================================
        // ⚠️ TESTS - Cas d'erreur
        // ========================================================================

        @Nested
        @DisplayName("deployContent - Cas d'erreur")
        class DeployContentErrorTests {

                @Test
                @DisplayName("ContentNode source non trouvé → erreur")
                void deployContent_withSourceNotFound_shouldThrowError() {
                        when(contentNodeRepository.findByCodeAndStatus(eq("NODE-DEV"), eq(StatusEnum.SNAPSHOT.name())))
                                        .thenReturn(Mono.empty());

                        StepVerifier.create(contentNodeHandler.deployContent("NODE-DEV", "PROD"))
                                        .expectErrorMatches(throwable -> throwable instanceof RuntimeException &&
                                                        throwable.getMessage().contains("Content node not found"))
                                        .verify();

                        verify(contentNodeRepository, never()).save(any());
                        verify(contentNodeSlugHelper, never()).update(any());
                }

                @Test
                @DisplayName("Échec de contentNodeSlugHelper.update → propagation de l'erreur")
                void deployContent_whenSlugHelperFails_shouldPropagateError() {
                        when(contentNodeRepository.findByCodeAndStatus(eq("NODE-DEV"), eq(StatusEnum.SNAPSHOT.name())))
                                        .thenReturn(Mono.just(snapshotEntity));
                        when(contentNodeRepository.findByCodeAndStatus(eq("NODE-PROD"), eq(StatusEnum.SNAPSHOT.name())))
                                        .thenReturn(Mono.empty());
                        when(nodeRepository.findByCodeAndStatus(anyString(), eq(StatusEnum.SNAPSHOT.name())))
                                        .thenReturn(Mono.just(new Node()));
                        when(contentNodeSlugHelper.update(any()))
                                        .thenReturn(Mono.error(new RuntimeException("Slug conflict")));

                        StepVerifier.create(contentNodeHandler.deployContent("NODE-DEV", "PROD"))
                                        .expectErrorMessage("Slug conflict")
                                        .verify();
                }

                @Test
                @DisplayName("Échec de repository.save → propagation de l'erreur")
                void deployContent_whenSaveFails_shouldPropagateError() {
                        when(contentNodeRepository.findByCodeAndStatus(eq("NODE-DEV"), eq(StatusEnum.SNAPSHOT.name())))
                                        .thenReturn(Mono.just(snapshotEntity));
                        when(contentNodeRepository.findByCodeAndStatus(eq("NODE-PROD"), eq(StatusEnum.SNAPSHOT.name())))
                                        .thenReturn(Mono.empty());
                        when(nodeRepository.findByCodeAndStatus(anyString(), eq(StatusEnum.SNAPSHOT.name())))
                                        .thenReturn(Mono.just(new Node()));
                        when(contentNodeSlugHelper.update(any()))
                                        .thenAnswer(inv -> Mono.just(inv.getArgument(0)));
                        when(contentNodeRepository.save(any(ContentNode.class)))
                                        .thenReturn(Mono.error(new IllegalStateException("DB error")));

                        StepVerifier.create(contentNodeHandler.deployContent("NODE-DEV", "PROD"))
                                        .expectErrorMessage("DB error")
                                        .verify();
                }
        }

        // ========================================================================
        // 🔄 TESTS - Transformation des codes
        // ========================================================================

        @Nested
        @DisplayName("deployContent - Transformation des codes")
        class DeployContentCodeTransformationTests {

                @Test
                @DisplayName("Transformation code DEV→PROD avec format standard")
                void deployContent_withStandardCodeFormat_shouldTransformCorrectly() {
                        com.itexpert.content.lib.models.ContentNode model = createSnapshotModel("CONTENT-DEV",
                                        "PARENT-DEV", "0", "slug");
                        ContentNode entity = cloneToEntity(model);

                        when(contentNodeRepository.findByCodeAndStatus(eq("CONTENT-DEV"),
                                        eq(StatusEnum.SNAPSHOT.name())))
                                        .thenReturn(Mono.just(entity));
                        when(contentNodeRepository.findByCodeAndStatus(eq("CONTENT-PROD"),
                                        eq(StatusEnum.SNAPSHOT.name())))
                                        .thenReturn(Mono.empty());
                        when(nodeRepository.findByCodeAndStatus(eq("PARENT-PROD"), eq(StatusEnum.SNAPSHOT.name())))
                                        .thenReturn(Mono.just(new Node()));
                        when(contentNodeRepository.save(any(ContentNode.class)))
                                        .thenAnswer(inv -> Mono.just(inv.getArgument(0)));
                        when(contentNodeSlugHelper.update(any()))
                                        .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

                        StepVerifier.create(contentNodeHandler.deployContent("CONTENT-DEV", "PROD"))
                                        .assertNext(result -> {
                                                assertThat(result.getCode()).isEqualTo("CONTENT-PROD");
                                                assertThat(result.getParentCode()).isEqualTo("PARENT-PROD");
                                        })
                                        .verifyComplete();
                }

                @Test
                @DisplayName("Déploiement vers environnement multi-partie (ex: PROD-EU)")
                void deployContent_withMultiPartEnvironment_shouldUseFirstPartForCode() {
                        when(contentNodeRepository.findByCodeAndStatus(eq("NODE-DEV"), eq(StatusEnum.SNAPSHOT.name())))
                                        .thenReturn(Mono.just(snapshotEntity));
                        when(contentNodeRepository.findByCodeAndStatus(eq("NODE-PROD"), eq(StatusEnum.SNAPSHOT.name())))
                                        .thenReturn(Mono.empty());
                        when(nodeRepository.findByCodeAndStatus(anyString(), eq(StatusEnum.SNAPSHOT.name())))
                                        .thenReturn(Mono.just(new Node()));
                        when(contentNodeRepository.save(any(ContentNode.class)))
                                        .thenAnswer(inv -> Mono.just(inv.getArgument(0)));
                        when(contentNodeSlugHelper.update(any()))
                                        .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

                        StepVerifier.create(contentNodeHandler.deployContent("NODE-DEV", "PROD-EU"))
                                        .assertNext(result -> {
                                                assertThat(result.getCode()).isEqualTo("NODE-PROD");
                                        })
                                        .verifyComplete();
                }

                @Test
                @DisplayName("Préservation du parentCode d'origine quand PROD existe déjà")
                void deployContent_withExistingProd_shouldPreserveOriginalParentCode() {
                        ContentNode existingProd = cloneToEntity(
                                        createSnapshotModel("NODE-PROD", "ORIGINAL-PARENT", "1", "slug"));

                        when(contentNodeRepository.findByCodeAndStatus(eq("NODE-DEV"), eq(StatusEnum.SNAPSHOT.name())))
                                        .thenReturn(Mono.just(snapshotEntity));
                        when(contentNodeRepository.findByCodeAndStatus(eq("NODE-PROD"), eq(StatusEnum.SNAPSHOT.name())))
                                        .thenReturn(Mono.just(existingProd));
                        when(nodeRepository.findByCodeAndStatus(eq("PARENT-PROD"), eq(StatusEnum.SNAPSHOT.name())))
                                        .thenReturn(Mono.just(new Node()));
                        when(contentNodeRepository.save(any(ContentNode.class)))
                                        .thenAnswer(inv -> Mono.just(inv.getArgument(0)));
                        when(contentNodeSlugHelper.update(any()))
                                        .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

                        StepVerifier.create(contentNodeHandler.deployContent("NODE-DEV", "PROD"))
                                        .assertNext(result -> assertThat(result.getParentCode())
                                                        .isEqualTo("ORIGINAL-PARENT"))
                                        .verifyComplete();
                }
        }

        // ========================================================================
        // 🔍 TESTS - Vérifications avancées
        // ========================================================================

        @Nested
        @DisplayName("deployContent - Vérifications avancées")
        class DeployContentAdvancedTests {

                @Test
                @DisplayName("Vérification que l'entité archivée a le statut ARCHIVE et la date de modification")
                void deployContent_archivedEntity_shouldHaveCorrectStatusAndModificationDate() {
                        ContentNode existingProd = cloneToEntity(
                                        createSnapshotModel("NODE-PROD", "PARENT-PROD", "1", "slug"));
                        Instant beforeDeploy = Instant.now();

                        when(contentNodeRepository.findByCodeAndStatus(eq("NODE-DEV"), eq(StatusEnum.SNAPSHOT.name())))
                                        .thenReturn(Mono.just(snapshotEntity));
                        when(contentNodeRepository.findByCodeAndStatus(eq("NODE-PROD"), eq(StatusEnum.SNAPSHOT.name())))
                                        .thenReturn(Mono.just(existingProd));
                        when(nodeRepository.findByCodeAndStatus(eq("PARENT-PROD"), eq(StatusEnum.SNAPSHOT.name())))
                                        .thenReturn(Mono.just(new Node()));
                        when(contentNodeRepository.save(any(ContentNode.class)))
                                        .thenAnswer(inv -> Mono.just(inv.getArgument(0)));
                        when(contentNodeSlugHelper.update(any()))
                                        .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

                        contentNodeHandler.deployContent("NODE-DEV", "PROD").block();

                        ArgumentCaptor<ContentNode> saveCaptor = ArgumentCaptor.forClass(ContentNode.class);
                        verify(contentNodeRepository, times(2)).save(saveCaptor.capture());

                        List<ContentNode> savedEntities = saveCaptor.getAllValues();
                        ContentNode archivedEntity = savedEntities.get(0);
                        ContentNode newEntity = savedEntities.get(1);

                        // 🔧 FIX: L'entité stocke le status comme String, donc comparer avec String
                        assertThat(archivedEntity.getStatus().name()).isEqualTo("ARCHIVE");
                        assertThat(archivedEntity.getModificationDate()).isNotNull();
                        assertThat(archivedEntity.getModificationDate())
                                        .isGreaterThanOrEqualTo(beforeDeploy.toEpochMilli());

                        assertThat(newEntity.getStatus().name()).isEqualTo("SNAPSHOT");
                        assertThat(newEntity.getVersion()).isEqualTo("2");
                }

                @Test
                @DisplayName("Vérification que la notification DEPLOYMENT est bien émise")
                void deployContent_shouldEmitDeploymentNotification() {
                        when(contentNodeRepository.findByCodeAndStatus(eq("NODE-DEV"), eq(StatusEnum.SNAPSHOT.name())))
                                        .thenReturn(Mono.just(snapshotEntity));
                        when(contentNodeRepository.findByCodeAndStatus(eq("NODE-PROD"), eq(StatusEnum.SNAPSHOT.name())))
                                        .thenReturn(Mono.empty());
                        when(nodeRepository.findByCodeAndStatus(anyString(), eq(StatusEnum.SNAPSHOT.name())))
                                        .thenReturn(Mono.just(new Node()));
                        when(contentNodeRepository.save(any(ContentNode.class)))
                                        .thenAnswer(inv -> Mono.just(inv.getArgument(0)));
                        when(contentNodeSlugHelper.update(any()))
                                        .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

                        contentNodeHandler.deployContent("NODE-DEV", "PROD").block();

                        verify(notificationHandler).create(
                                        eq(com.itexpert.content.lib.enums.NotificationEnum.DEPLOYMENT),
                                        eq("NODE-PROD"),
                                        any(),
                                        eq("CONTENT_NODE"),
                                        eq("NODE-PROD"),
                                        eq("0"),
                                        eq(Boolean.TRUE));
                }

                @Test
                @DisplayName("Vérification des dates de création/modification sur nouvelle entité")
                void deployContent_newEntity_shouldHaveCorrectDates() {
                        Instant beforeDeploy = Instant.now();

                        when(contentNodeRepository.findByCodeAndStatus(eq("NODE-DEV"), eq(StatusEnum.SNAPSHOT.name())))
                                        .thenReturn(Mono.just(snapshotEntity));
                        when(contentNodeRepository.findByCodeAndStatus(eq("NODE-PROD"), eq(StatusEnum.SNAPSHOT.name())))
                                        .thenReturn(Mono.empty());
                        when(nodeRepository.findByCodeAndStatus(anyString(), eq(StatusEnum.SNAPSHOT.name())))
                                        .thenReturn(Mono.just(new Node()));
                        when(contentNodeRepository.save(any(ContentNode.class)))
                                        .thenAnswer(inv -> Mono.just(inv.getArgument(0)));
                        when(contentNodeSlugHelper.update(any()))
                                        .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

                        com.itexpert.content.lib.models.ContentNode result = contentNodeHandler
                                        .deployContent("NODE-DEV", "PROD")
                                        .block();

                        assertThat(result.getCreationDate()).isNotNull();
                        assertThat(result.getModificationDate()).isNotNull();
                        assertThat(result.getCreationDate()).isEqualTo(result.getModificationDate());
                        assertThat(result.getCreationDate()).isGreaterThanOrEqualTo(beforeDeploy.toEpochMilli());
                }
        }

        // ========================================================================
        // 🧰 MÉTHODES UTILITAIRES
        // ========================================================================

        private com.itexpert.content.lib.models.ContentNode createSnapshotModel(
                        String code, String parentCode, String version, String slug) {

                com.itexpert.content.lib.models.ContentNode model = new com.itexpert.content.lib.models.ContentNode();
                model.setId(UUID.randomUUID());
                model.setCode(code);
                model.setParentCode(parentCode);
                model.setVersion(version);
                model.setStatus(StatusEnum.SNAPSHOT);
                model.setSlug(slug);
                model.setCreationDate(Instant.now().minusSeconds(3600).toEpochMilli());
                model.setModificationDate(Instant.now().minusSeconds(1800).toEpochMilli());
                model.setModifiedBy("test-user");
                return model;
        }

        private ContentNode cloneToEntity(com.itexpert.content.lib.models.ContentNode model) {
                if (model == null)
                        return null;
                ContentNode entity = new ContentNode();
                entity.setId(model.getId());
                entity.setCode(model.getCode());
                entity.setParentCode(model.getParentCode());
                entity.setParentCodeOrigin(model.getParentCodeOrigin());
                entity.setVersion(model.getVersion());
                // 🔧 L'entité utilise String pour le status
                entity.setStatus(model.getStatus());
                entity.setSlug(model.getSlug());
                entity.setRules(model.getRules());
                entity.setContent(model.getContent());
                entity.setCreationDate(model.getCreationDate());
                entity.setModificationDate(model.getModificationDate());
                entity.setPublicationDate(model.getPublicationDate());
                entity.setModifiedBy(model.getModifiedBy());
                entity.setMaxVersionsToKeep(model.getMaxVersionsToKeep());
                entity.setFavorite(model.isFavorite());
                return entity;
        }

        private com.itexpert.content.lib.models.ContentNode cloneToModel(ContentNode entity) {
                if (entity == null)
                        return null;
                com.itexpert.content.lib.models.ContentNode model = new com.itexpert.content.lib.models.ContentNode();
                model.setId(entity.getId());
                model.setCode(entity.getCode());
                model.setParentCode(entity.getParentCode());
                model.setParentCodeOrigin(entity.getParentCodeOrigin());
                model.setVersion(entity.getVersion());
                // 🔧 Conversion String → StatusEnum
                model.setStatus(entity.getStatus());
                model.setSlug(entity.getSlug());
                model.setRules(entity.getRules());
                model.setContent(entity.getContent());
                model.setCreationDate(entity.getCreationDate());
                model.setModificationDate(entity.getModificationDate());
                model.setPublicationDate(entity.getPublicationDate());
                model.setModifiedBy(entity.getModifiedBy());
                model.setMaxVersionsToKeep(entity.getMaxVersionsToKeep());
                model.setFavorite(entity.isFavorite());
                return model;
        }
}