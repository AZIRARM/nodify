package com.itexpert.content.core.handlers;

import com.itexpert.content.core.helpers.ContentHelper;
import com.itexpert.content.core.helpers.ContentNodeSlugHelper;
import com.itexpert.content.core.mappers.ContentNodeMapper;
import com.itexpert.content.core.mappers.NodeMapper;
import com.itexpert.content.core.repositories.ContentNodeRepository;
import com.itexpert.content.core.repositories.NodeRepository;
import com.itexpert.content.lib.entities.ContentNode;
import com.itexpert.content.lib.enums.StatusEnum;
import com.itexpert.content.lib.models.Notification;
import com.itexpert.content.lib.models.Rule;
import com.itexpert.content.lib.enums.TypeEnum;
import com.itexpert.content.lib.enums.OperatorEnum;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentNodeHandlerImportTest {

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
    // 🎯 TESTS PRINCIPAUX - importContentNode
    // ========================================================================

    @Nested
    @DisplayName("importContentNode - Cas nominaux")
    class ImportContentNodeNominalTests {

        @Test
        @DisplayName("Import avec contenu existant → incrémentation version + archivage")
        void importContentNode_withExistingContent_shouldIncrementVersionAndArchive() {
            com.itexpert.content.lib.models.ContentNode inputNode = createSnapshotModel("CODE-123", "PARENT-1", "1",
                    "slug-existing");
            ContentNode existingEntity = cloneToEntity(
                    createSnapshotModel("CODE-123", "PARENT-1", "1", "slug-existing"));
            ContentNode savedArchived = cloneToEntity(
                    createSnapshotModel("CODE-123", "PARENT-1", "1", "slug-existing"));
            savedArchived.setStatus(StatusEnum.ARCHIVE);
            ContentNode savedNew = cloneToEntity(createSnapshotModel("CODE-123", "PARENT-1", "2", "slug-existing"));

            when(contentNodeRepository.findByCodeAndStatus(eq("CODE-123"), eq(StatusEnum.SNAPSHOT.name())))
                    .thenReturn(Mono.just(existingEntity));
            when(contentNodeRepository.save(any(ContentNode.class)))
                    .thenReturn(Mono.just(savedArchived))
                    .thenReturn(Mono.just(savedNew));
            when(contentNodeSlugHelper.update(any()))
                    .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

            StepVerifier.create(contentNodeHandler.importContentNode(inputNode))
                    .assertNext(result -> {
                        assertThat(result.getCode()).isEqualTo("CODE-123");
                        assertThat(result.getVersion()).isEqualTo("2");
                        assertThat(result.getStatus()).isEqualTo(StatusEnum.SNAPSHOT);
                        assertThat(result.getSlug()).isEqualTo("slug-existing");
                        assertThat(result.getId()).isNotNull();
                    })
                    .verifyComplete();

            verify(contentNodeRepository, times(2)).save(any(ContentNode.class));
            verify(contentNodeSlugHelper).update(any());
            verify(notificationHandler).create(
                    eq(com.itexpert.content.lib.enums.NotificationEnum.IMPORT),
                    any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Import sans contenu existant → création version 0")
        void importContentNode_withoutExistingContent_shouldCreateVersionZero() {
            // 🔧 FIX: Pour une nouvelle entité, ne pas définir de dates dans le modèle
            // d'entrée
            // Les dates seront générées automatiquement par le handler
            com.itexpert.content.lib.models.ContentNode inputNode = new com.itexpert.content.lib.models.ContentNode();
            inputNode.setCode("NEW-CODE");
            inputNode.setParentCode("PARENT-NEW");
            inputNode.setSlug("new-slug");
            inputNode.setStatus(StatusEnum.SNAPSHOT);
            inputNode.setRules(getDefaultBoolDateRules());
            // ❌ Pas de setCreationDate/setModificationDate - seront générés

            ContentNode savedEntity = cloneToEntity(inputNode);
            savedEntity.setId(UUID.randomUUID());
            savedEntity.setVersion("0");
            // Les dates seront set par le handler lors du save

            when(contentNodeRepository.findByCodeAndStatus(eq("NEW-CODE"), eq(StatusEnum.SNAPSHOT.name())))
                    .thenReturn(Mono.empty());
            when(contentNodeRepository.save(any(ContentNode.class)))
                    .thenAnswer(inv -> {
                        ContentNode entity = inv.getArgument(0);
                        // Simulation du comportement du handler qui set les dates
                        if (entity.getCreationDate() == null) {
                            entity.setCreationDate(Instant.now().toEpochMilli());
                            entity.setModificationDate(entity.getCreationDate());
                        }
                        return Mono.just(entity);
                    });
            when(contentNodeSlugHelper.update(any()))
                    .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

            StepVerifier.create(contentNodeHandler.importContentNode(inputNode))
                    .assertNext(result -> {
                        assertThat(result.getCode()).isEqualTo("NEW-CODE");
                        assertThat(result.getVersion()).isEqualTo("0");
                        assertThat(result.getStatus()).isEqualTo(StatusEnum.SNAPSHOT);
                        assertThat(result.getId()).isNotNull();
                        // 🔧 FIX: Vérifier que les dates sont présentes, pas une valeur exacte
                        assertThat(result.getCreationDate()).isNotNull();
                        assertThat(result.getModificationDate()).isNotNull();
                    })
                    .verifyComplete();

            verify(contentNodeRepository, times(1)).save(any(ContentNode.class));
            verify(contentNodeSlugHelper).update(any());
        }

        @Test
        @DisplayName("Import avec slug null → slug reste null après update")
        void importContentNode_withNullSlug_shouldHandleNullSlug() {
            com.itexpert.content.lib.models.ContentNode inputNode = createSnapshotModel("CODE-NULL-SLUG", "PARENT", "1",
                    null);
            ContentNode existingEntity = cloneToEntity(createSnapshotModel("CODE-NULL-SLUG", "PARENT", "1", null));
            ContentNode savedNew = cloneToEntity(createSnapshotModel("CODE-NULL-SLUG", "PARENT", "2", null));

            when(contentNodeRepository.findByCodeAndStatus(eq("CODE-NULL-SLUG"), eq(StatusEnum.SNAPSHOT.name())))
                    .thenReturn(Mono.just(existingEntity));
            when(contentNodeRepository.save(any(ContentNode.class)))
                    .thenReturn(Mono.just(existingEntity))
                    .thenReturn(Mono.just(savedNew));
            when(contentNodeSlugHelper.update(any()))
                    .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

            StepVerifier.create(contentNodeHandler.importContentNode(inputNode))
                    .assertNext(result -> assertThat(result.getSlug()).isNull())
                    .verifyComplete();
        }
    }

    // ========================================================================
    // 🔄 TESTS - Versions et dates
    // ========================================================================

    @Nested
    @DisplayName("importContentNode - Versions et dates")
    class ImportContentNodeVersionTests {

        @Test
        @DisplayName("Incrémentation correcte de la version (1 → 2)")
        void importContentNode_shouldIncrementVersionCorrectly() {
            com.itexpert.content.lib.models.ContentNode inputNode = createSnapshotModel("VER-TEST", "PARENT", "1",
                    "slug");
            ContentNode existingEntity = cloneToEntity(createSnapshotModel("VER-TEST", "PARENT", "1", "slug"));
            ContentNode savedNew = cloneToEntity(createSnapshotModel("VER-TEST", "PARENT", "2", "slug"));

            when(contentNodeRepository.findByCodeAndStatus(eq("VER-TEST"), eq(StatusEnum.SNAPSHOT.name())))
                    .thenReturn(Mono.just(existingEntity));
            when(contentNodeRepository.save(any(ContentNode.class)))
                    .thenReturn(Mono.just(existingEntity))
                    .thenReturn(Mono.just(savedNew));
            when(contentNodeSlugHelper.update(any()))
                    .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

            StepVerifier.create(contentNodeHandler.importContentNode(inputNode))
                    .assertNext(result -> assertThat(result.getVersion()).isEqualTo("2"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("Incrémentation version avec numéro élevé (99 → 100)")
        void importContentNode_shouldHandleHighVersionNumbers() {
            com.itexpert.content.lib.models.ContentNode inputNode = createSnapshotModel("HIGH-VER", "PARENT", "99",
                    "slug");
            ContentNode existingEntity = cloneToEntity(createSnapshotModel("HIGH-VER", "PARENT", "99", "slug"));
            ContentNode savedNew = cloneToEntity(createSnapshotModel("HIGH-VER", "PARENT", "100", "slug"));

            when(contentNodeRepository.findByCodeAndStatus(eq("HIGH-VER"), eq(StatusEnum.SNAPSHOT.name())))
                    .thenReturn(Mono.just(existingEntity));
            when(contentNodeRepository.save(any(ContentNode.class)))
                    .thenReturn(Mono.just(existingEntity))
                    .thenReturn(Mono.just(savedNew));
            when(contentNodeSlugHelper.update(any()))
                    .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

            StepVerifier.create(contentNodeHandler.importContentNode(inputNode))
                    .assertNext(result -> assertThat(result.getVersion()).isEqualTo("100"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("Préservation de la creationDate de l'existant")
        void importContentNode_shouldPreserveOriginalCreationDate() {
            // 🔧 FIX: Utiliser une date fixe et connue pour l'existant
            Instant originalCreationDate = Instant.parse("2024-01-15T10:30:00Z");

            com.itexpert.content.lib.models.ContentNode inputNode = createSnapshotModel("DATE-TEST", "PARENT", "1",
                    "slug");
            ContentNode existingEntity = cloneToEntity(createSnapshotModel("DATE-TEST", "PARENT", "1", "slug"));
            existingEntity.setCreationDate(originalCreationDate.toEpochMilli());
            existingEntity.setModificationDate(Instant.now().toEpochMilli());

            ContentNode savedNew = cloneToEntity(createSnapshotModel("DATE-TEST", "PARENT", "2", "slug"));
            savedNew.setCreationDate(originalCreationDate.toEpochMilli());

            when(contentNodeRepository.findByCodeAndStatus(eq("DATE-TEST"), eq(StatusEnum.SNAPSHOT.name())))
                    .thenReturn(Mono.just(existingEntity));
            when(contentNodeRepository.save(any(ContentNode.class)))
                    .thenReturn(Mono.just(existingEntity))
                    .thenReturn(Mono.just(savedNew));
            when(contentNodeSlugHelper.update(any()))
                    .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

            StepVerifier.create(contentNodeHandler.importContentNode(inputNode))
                    .assertNext(result -> {
                        // 🔧 FIX: Comparer avec la date fixe, pas Instant.now()
                        assertThat(result.getCreationDate()).isEqualTo(originalCreationDate.toEpochMilli());
                        // modificationDate doit être plus récente que creationDate
                        assertThat(result.getModificationDate()).isGreaterThan(result.getCreationDate());
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Nouvelle entité → dates de création et modification identiques")
        void importContentNode_newEntity_shouldHaveSameCreationAndModificationDates() {
            // 🔧 FIX: Pour une nouvelle entité, ne pas pré-définir les dates dans l'input
            com.itexpert.content.lib.models.ContentNode inputNode = new com.itexpert.content.lib.models.ContentNode();
            inputNode.setCode("NEW-DATE");
            inputNode.setParentCode("PARENT");
            inputNode.setSlug("slug");
            inputNode.setStatus(StatusEnum.SNAPSHOT);
            inputNode.setRules(getDefaultBoolDateRules());

            ContentNode savedEntity = cloneToEntity(inputNode);
            savedEntity.setId(UUID.randomUUID());
            savedEntity.setVersion("0");

            when(contentNodeRepository.findByCodeAndStatus(eq("NEW-DATE"), eq(StatusEnum.SNAPSHOT.name())))
                    .thenReturn(Mono.empty());
            when(contentNodeRepository.save(any(ContentNode.class)))
                    .thenAnswer(inv -> {
                        ContentNode entity = inv.getArgument(0);
                        // Simulation: le handler set les dates si null
                        if (entity.getCreationDate() == null) {
                            long now = Instant.now().toEpochMilli();
                            entity.setCreationDate(now);
                            entity.setModificationDate(now);
                        }
                        return Mono.just(entity);
                    });
            when(contentNodeSlugHelper.update(any()))
                    .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

            // When
            com.itexpert.content.lib.models.ContentNode result = contentNodeHandler
                    .importContentNode(inputNode)
                    .block();

            // Then
            // 🔧 FIX: Vérifier que les dates sont égales entre elles, pas une valeur
            // absolue
            assertThat(result.getCreationDate()).isNotNull();
            assertThat(result.getModificationDate()).isNotNull();
            assertThat(result.getCreationDate()).isEqualTo(result.getModificationDate());
            // 🔧 FIX: Vérifier que la date est récente (dans les dernières 5 secondes)
            assertThat(result.getCreationDate()).isGreaterThan(Instant.now().minusSeconds(5).toEpochMilli());
            assertThat(result.getCreationDate()).isLessThanOrEqualTo(Instant.now().toEpochMilli());
        }
    }

    // ========================================================================
    // 🧩 TESTS - Champs spécifiques : rules (BOOL/DATE), favorite, content
    // ========================================================================

    @Nested
    @DisplayName("importContentNode - Champs et métadonnées")
    class ImportContentNodeFieldsTests {

        @Test
        @DisplayName("Préservation du champ favorite de l'existant")
        void importContentNode_shouldPreserveFavoriteFlag() {
            com.itexpert.content.lib.models.ContentNode inputNode = createSnapshotModel("FAV-TEST", "PARENT", "1",
                    "slug");
            inputNode.setFavorite(true);

            ContentNode existingEntity = cloneToEntity(createSnapshotModel("FAV-TEST", "PARENT", "1", "slug"));
            existingEntity.setFavorite(true);
            ContentNode savedNew = cloneToEntity(createSnapshotModel("FAV-TEST", "PARENT", "2", "slug"));
            savedNew.setFavorite(true);

            when(contentNodeRepository.findByCodeAndStatus(eq("FAV-TEST"), eq(StatusEnum.SNAPSHOT.name())))
                    .thenReturn(Mono.just(existingEntity));
            when(contentNodeRepository.save(any(ContentNode.class)))
                    .thenReturn(Mono.just(existingEntity))
                    .thenReturn(Mono.just(savedNew));
            when(contentNodeSlugHelper.update(any()))
                    .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

            StepVerifier.create(contentNodeHandler.importContentNode(inputNode))
                    .assertNext(result -> assertThat(result.isFavorite()).isTrue())
                    .verifyComplete();
        }

        @Test
        @DisplayName("Préservation des rules BOOL/DATE avec clonage profond")
        void importContentNode_shouldPreserveBoolAndDateRulesWithDeepClone() {
            List<Rule> customRules = getDefaultBoolDateRules();
            customRules.get(0).setValue("true");

            com.itexpert.content.lib.models.ContentNode inputNode = createSnapshotModel("RULES-TEST", "PARENT", "1",
                    "slug");
            ContentNode existingEntity = cloneToEntity(createSnapshotModel("RULES-TEST", "PARENT", "1", "slug"));
            existingEntity.setRules(cloneRulesList(customRules));

            ContentNode savedNew = cloneToEntity(createSnapshotModel("RULES-TEST", "PARENT", "2", "slug"));
            savedNew.setRules(cloneRulesList(customRules));

            when(contentNodeRepository.findByCodeAndStatus(eq("RULES-TEST"), eq(StatusEnum.SNAPSHOT.name())))
                    .thenReturn(Mono.just(existingEntity));
            when(contentNodeRepository.save(any(ContentNode.class)))
                    .thenReturn(Mono.just(existingEntity))
                    .thenReturn(Mono.just(savedNew));
            when(contentNodeSlugHelper.update(any()))
                    .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

            StepVerifier.create(contentNodeHandler.importContentNode(inputNode))
                    .assertNext(result -> {
                        assertThat(result.getRules()).isNotNull();
                        assertThat(result.getRules()).hasSize(2);

                        Rule boolRule = result.getRules().get(0);
                        assertThat(boolRule.getType()).isEqualTo(TypeEnum.BOOL);
                        assertThat(boolRule.getName()).isEqualTo("isActive");
                        assertThat(boolRule.getValue()).isEqualTo("true");
                        assertThat(boolRule.getOperator()).isEqualTo(OperatorEnum.EQ);

                        Rule dateRule = result.getRules().get(1);
                        assertThat(dateRule.getType()).isEqualTo(TypeEnum.DATE);
                        assertThat(dateRule.getName()).isEqualTo("expiryDate");
                        assertThat(dateRule.getOperator()).isEqualTo(OperatorEnum.SUP_EQ);

                        assertThat(result.getRules().get(0)).isNotSameAs(customRules.get(0));
                        assertThat(result.getRules().get(1)).isNotSameAs(customRules.get(1));
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("Import avec rules null → gestion sécurisée sans NPE")
        void importContentNode_withNullRules_shouldHandleSafely() {
            com.itexpert.content.lib.models.ContentNode inputNode = createSnapshotModel("NULL-RULES", "PARENT", null,
                    "slug");
            inputNode.setRules(null);

            ContentNode existingEntity = cloneToEntity(createSnapshotModel("NULL-RULES", "PARENT", "1", "slug"));
            existingEntity.setRules(null);
            ContentNode savedNew = cloneToEntity(createSnapshotModel("NULL-RULES", "PARENT", "2", "slug"));
            savedNew.setRules(null);

            when(contentNodeRepository.findByCodeAndStatus(eq("NULL-RULES"), eq(StatusEnum.SNAPSHOT.name())))
                    .thenReturn(Mono.just(existingEntity));
            when(contentNodeRepository.save(any(ContentNode.class)))
                    .thenReturn(Mono.just(existingEntity))
                    .thenReturn(Mono.just(savedNew));
            when(contentNodeSlugHelper.update(any()))
                    .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

            StepVerifier.create(contentNodeHandler.importContentNode(inputNode))
                    .assertNext(result -> assertThat(result.getRules()).isNull())
                    .verifyComplete();
        }

        @Test
        @DisplayName("Préservation du content de l'existant")
        void importContentNode_shouldPreserveContent() {
            String contentData = "{\"blocks\": [{\"type\": \"text\", \"value\": \"Hello\"}]}";
            com.itexpert.content.lib.models.ContentNode inputNode = createSnapshotModel("CONTENT-TEST", "PARENT", "1",
                    "slug");
            ContentNode existingEntity = cloneToEntity(createSnapshotModel("CONTENT-TEST", "PARENT", "1", "slug"));
            existingEntity.setContent(contentData);
            ContentNode savedNew = cloneToEntity(createSnapshotModel("CONTENT-TEST", "PARENT", "2", "slug"));
            savedNew.setContent(contentData);

            when(contentNodeRepository.findByCodeAndStatus(eq("CONTENT-TEST"), eq(StatusEnum.SNAPSHOT.name())))
                    .thenReturn(Mono.just(existingEntity));
            when(contentNodeRepository.save(any(ContentNode.class)))
                    .thenReturn(Mono.just(existingEntity))
                    .thenReturn(Mono.just(savedNew));
            when(contentNodeSlugHelper.update(any()))
                    .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

            StepVerifier.create(contentNodeHandler.importContentNode(inputNode))
                    .assertNext(result -> assertThat(result.getContent()).isEqualTo(contentData))
                    .verifyComplete();
        }
    }

    // ========================================================================
    // 🧩 TESTS - Gestion des Slugs
    // ========================================================================

    @Nested
    @DisplayName("importContentNode - Gestion des slugs")
    class ImportContentNodeSlugTests {

        @Test
        @DisplayName("contentNodeSlugHelper.update génère un nouveau slug → slug mis à jour")
        void importContentNode_whenSlugHelperGeneratesNewSlug_shouldUseNewSlug() {
            com.itexpert.content.lib.models.ContentNode inputNode = createSnapshotModel("SLUG-GEN", "PARENT", null,
                    null);
            ContentNode savedEntity = cloneToEntity(
                    createSnapshotModel("SLUG-GEN", "PARENT", "0", "auto-generated-slug"));
            savedEntity.setId(UUID.randomUUID());

            when(contentNodeRepository.findByCodeAndStatus(eq("SLUG-GEN"), eq(StatusEnum.SNAPSHOT.name())))
                    .thenReturn(Mono.empty());
            when(contentNodeRepository.save(any(ContentNode.class)))
                    .thenReturn(Mono.just(savedEntity));

            when(contentNodeSlugHelper.update(any()))
                    .thenAnswer(inv -> {
                        com.itexpert.content.lib.models.ContentNode model = inv.getArgument(0);
                        model.setSlug("auto-generated-slug");
                        return Mono.just(model);
                    });

            StepVerifier.create(contentNodeHandler.importContentNode(inputNode))
                    .assertNext(result -> assertThat(result.getSlug()).isEqualTo("auto-generated-slug"))
                    .verifyComplete();
        }

        @Test
        @DisplayName("Slug existant conservé même si input a un slug différent")
        void importContentNode_withExistingSlug_shouldKeepExistingSlug() {
            com.itexpert.content.lib.models.ContentNode inputNode = createSnapshotModel("SLUG-KEEP", "PARENT", "1",
                    "input-slug");
            ContentNode existingEntity = cloneToEntity(
                    createSnapshotModel("SLUG-KEEP", "PARENT", "1", "existing-slug"));
            ContentNode savedNew = cloneToEntity(createSnapshotModel("SLUG-KEEP", "PARENT", "2", "existing-slug"));

            when(contentNodeRepository.findByCodeAndStatus(eq("SLUG-KEEP"), eq(StatusEnum.SNAPSHOT.name())))
                    .thenReturn(Mono.just(existingEntity));
            when(contentNodeRepository.save(any(ContentNode.class)))
                    .thenReturn(Mono.just(existingEntity))
                    .thenReturn(Mono.just(savedNew));
            when(contentNodeSlugHelper.update(any()))
                    .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

            StepVerifier.create(contentNodeHandler.importContentNode(inputNode))
                    .assertNext(result -> assertThat(result.getSlug()).isEqualTo("existing-slug"))
                    .verifyComplete();
        }
    }

    // ========================================================================
    // ⚠️ TESTS - Cas d'erreur
    // ========================================================================

    @Nested
    @DisplayName("importContentNode - Cas d'erreur")
    class ImportContentNodeErrorTests {

        @Test
        @DisplayName("Échec de contentNodeSlugHelper.update → propagation de l'erreur")
        void importContentNode_whenSlugHelperFails_shouldPropagateError() {
            com.itexpert.content.lib.models.ContentNode inputNode = createSnapshotModel("ERROR-SLUG", "PARENT", null,
                    "slug");

            when(contentNodeRepository.findByCodeAndStatus(eq("ERROR-SLUG"), eq(StatusEnum.SNAPSHOT.name())))
                    .thenReturn(Mono.empty());
            when(contentNodeSlugHelper.update(any()))
                    .thenReturn(Mono.error(new RuntimeException("Slug already exists")));

            StepVerifier.create(contentNodeHandler.importContentNode(inputNode))
                    .expectErrorMessage("Slug already exists")
                    .verify();

            verify(contentNodeRepository, never()).save(any());
        }

        @Test
        @DisplayName("Échec de repository.save sur archivage → propagation de l'erreur")
        void importContentNode_whenArchiveSaveFails_shouldPropagateError() {
            com.itexpert.content.lib.models.ContentNode inputNode = createSnapshotModel("ERROR-ARCHIVE", "PARENT", "1",
                    "slug");
            ContentNode existingEntity = cloneToEntity(createSnapshotModel("ERROR-ARCHIVE", "PARENT", "1", "slug"));

            when(contentNodeRepository.findByCodeAndStatus(eq("ERROR-ARCHIVE"), eq(StatusEnum.SNAPSHOT.name())))
                    .thenReturn(Mono.just(existingEntity));
            // 🔧 FIX: Le save échoue dès le premier appel (archivage), donc
            // slugHelper.update() n'est jamais atteint
            when(contentNodeRepository.save(any(ContentNode.class)))
                    .thenReturn(Mono.error(new IllegalStateException("Database unavailable")));
            // ❌ SUPPRIMÉ: when(contentNodeSlugHelper.update(any())) - jamais appelé dans ce
            // cas d'erreur

            StepVerifier.create(contentNodeHandler.importContentNode(inputNode))
                    .expectErrorMessage("Database unavailable")
                    .verify();

            // 🔧 Optionnel: Vérifier que slugHelper.update() n'a jamais été appelé
            verify(contentNodeSlugHelper, never()).update(any());
        }

        @Test
        @DisplayName("Échec de repository.save sur nouvelle version → propagation de l'erreur")
        void importContentNode_whenNewVersionSaveFails_shouldPropagateError() {
            com.itexpert.content.lib.models.ContentNode inputNode = createSnapshotModel("ERROR-NEW", "PARENT", "1",
                    "slug");
            ContentNode existingEntity = cloneToEntity(createSnapshotModel("ERROR-NEW", "PARENT", "1", "slug"));
            ContentNode archivedEntity = cloneToEntity(createSnapshotModel("ERROR-NEW", "PARENT", "1", "slug"));
            archivedEntity.setStatus(StatusEnum.ARCHIVE);

            when(contentNodeRepository.findByCodeAndStatus(eq("ERROR-NEW"), eq(StatusEnum.SNAPSHOT.name())))
                    .thenReturn(Mono.just(existingEntity));
            when(contentNodeRepository.save(any(ContentNode.class)))
                    .thenReturn(Mono.just(archivedEntity))
                    .thenReturn(Mono.error(new IllegalStateException("Save failed")));
            when(contentNodeSlugHelper.update(any()))
                    .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

            StepVerifier.create(contentNodeHandler.importContentNode(inputNode))
                    .expectErrorMessage("Save failed")
                    .verify();
        }
    }

    // ========================================================================
    // 🔍 TESTS - Vérifications avancées
    // ========================================================================

    @Nested
    @DisplayName("importContentNode - Vérifications avancées")
    class ImportContentNodeAdvancedTests {

        @Test
        @DisplayName("Vérification que l'entité archivée a le statut ARCHIVE")
        void importContentNode_archivedEntity_shouldHaveArchiveStatus() {
            com.itexpert.content.lib.models.ContentNode inputNode = createSnapshotModel("ARCHIVE-CHECK", "PARENT", "1",
                    "slug");
            ContentNode existingEntity = cloneToEntity(createSnapshotModel("ARCHIVE-CHECK", "PARENT", "1", "slug"));
            ContentNode savedNew = cloneToEntity(createSnapshotModel("ARCHIVE-CHECK", "PARENT", "2", "slug"));

            when(contentNodeRepository.findByCodeAndStatus(eq("ARCHIVE-CHECK"), eq(StatusEnum.SNAPSHOT.name())))
                    .thenReturn(Mono.just(existingEntity));
            when(contentNodeRepository.save(any(ContentNode.class)))
                    .thenAnswer(inv -> Mono.just(inv.getArgument(0)));
            when(contentNodeSlugHelper.update(any()))
                    .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

            contentNodeHandler.importContentNode(inputNode).block();

            ArgumentCaptor<ContentNode> saveCaptor = ArgumentCaptor.forClass(ContentNode.class);
            verify(contentNodeRepository, times(2)).save(saveCaptor.capture());

            ContentNode archivedEntity = saveCaptor.getAllValues().get(0);
            ContentNode newEntity = saveCaptor.getAllValues().get(1);

            assertThat(archivedEntity.getStatus().name()).isEqualTo("ARCHIVE");
            assertThat(newEntity.getStatus().name()).isEqualTo("SNAPSHOT");
            assertThat(newEntity.getVersion()).isEqualTo("2");
        }

        @Test
        @DisplayName("Vérification que la notification IMPORT est bien émise")
        void importContentNode_shouldEmitImportNotification() {
            com.itexpert.content.lib.models.ContentNode inputNode = createSnapshotModel("NOTIF-CHECK", "PARENT", null,
                    "slug");
            ContentNode savedEntity = cloneToEntity(createSnapshotModel("NOTIF-CHECK", "PARENT", "0", "slug"));
            savedEntity.setId(UUID.randomUUID());

            when(contentNodeRepository.findByCodeAndStatus(eq("NOTIF-CHECK"), eq(StatusEnum.SNAPSHOT.name())))
                    .thenReturn(Mono.empty());
            when(contentNodeRepository.save(any(ContentNode.class)))
                    .thenReturn(Mono.just(savedEntity));
            when(contentNodeSlugHelper.update(any()))
                    .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

            contentNodeHandler.importContentNode(inputNode).block();

            verify(notificationHandler).create(
                    eq(com.itexpert.content.lib.enums.NotificationEnum.IMPORT),
                    eq("NOTIF-CHECK"),
                    any(),
                    eq("CONTENT_NODE"),
                    eq("NOTIF-CHECK"),
                    eq("0"),
                    eq(Boolean.TRUE));
        }

        @Test
        @DisplayName("Vérification que le mapper est appelé correctement")
        void importContentNode_shouldUseMappersCorrectly() {
            com.itexpert.content.lib.models.ContentNode inputNode = createSnapshotModel("MAPPER-CHECK", "PARENT", null,
                    "slug");
            ContentNode savedEntity = cloneToEntity(createSnapshotModel("MAPPER-CHECK", "PARENT", "0", "slug"));
            savedEntity.setId(UUID.randomUUID());

            when(contentNodeRepository.findByCodeAndStatus(eq("MAPPER-CHECK"), eq(StatusEnum.SNAPSHOT.name())))
                    .thenReturn(Mono.empty());
            when(contentNodeRepository.save(any(ContentNode.class)))
                    .thenReturn(Mono.just(savedEntity));
            when(contentNodeSlugHelper.update(any()))
                    .thenAnswer(inv -> Mono.just(inv.getArgument(0)));

            contentNodeHandler.importContentNode(inputNode).block();

            verify(contentNodeMapper).fromModel(any());
            verify(contentNodeMapper).fromEntity(any());
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
        // 🔧 FIX: Date fixe pour les tests avec existant (reproductible)
        model.setCreationDate(Instant.parse("2024-01-15T10:30:00Z").toEpochMilli());
        model.setModificationDate(Instant.parse("2024-01-15T11:00:00Z").toEpochMilli());
        model.setModifiedBy("test-user");
        model.setRules(getDefaultBoolDateRules());
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
        entity.setStatus(model.getStatus());
        entity.setSlug(model.getSlug());
        entity.setRules(cloneRulesList(model.getRules()));
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
        model.setStatus(entity.getStatus());
        model.setSlug(entity.getSlug());
        model.setRules(cloneRulesList(entity.getRules()));
        model.setContent(entity.getContent());
        model.setCreationDate(entity.getCreationDate());
        model.setModificationDate(entity.getModificationDate());
        model.setPublicationDate(entity.getPublicationDate());
        model.setModifiedBy(entity.getModifiedBy());
        model.setMaxVersionsToKeep(entity.getMaxVersionsToKeep());
        model.setFavorite(entity.isFavorite());
        return model;
    }

    private List<Rule> cloneRulesList(List<Rule> originalRules) {
        if (originalRules == null)
            return null;
        List<Rule> cloned = new ArrayList<>();
        for (Rule rule : originalRules) {
            try {
                cloned.add(rule.clone());
            } catch (CloneNotSupportedException e) {
                Rule copy = new Rule();
                copy.setType(rule.getType());
                copy.setName(rule.getName());
                copy.setValue(rule.getValue());
                copy.setEditable(rule.isEditable());
                copy.setErasable(rule.isErasable());
                copy.setOperator(rule.getOperator());
                copy.setBehavior(rule.getBehavior());
                copy.setEnable(rule.getEnable());
                copy.setDescription(rule.getDescription());
                cloned.add(copy);
            }
        }
        return cloned;
    }

    private List<Rule> getDefaultBoolDateRules() {
        List<Rule> rules = new ArrayList<>();

        Rule boolRule = new Rule();
        boolRule.setType(TypeEnum.BOOL);
        boolRule.setName("isActive");
        boolRule.setValue("false");
        boolRule.setEditable(true);
        boolRule.setErasable(false);
        boolRule.setOperator(OperatorEnum.EQ);
        boolRule.setBehavior(true);
        boolRule.setEnable(true);
        boolRule.setDescription("Règle booléenne pour statut actif");
        rules.add(boolRule);

        Rule dateRule = new Rule();
        dateRule.setType(TypeEnum.DATE);
        dateRule.setName("expiryDate");
        dateRule.setValue("2024-12-31T23:59:59Z");
        dateRule.setEditable(true);
        dateRule.setErasable(true);
        dateRule.setOperator(OperatorEnum.SUP_EQ);
        dateRule.setBehavior(false);
        dateRule.setEnable(true);
        dateRule.setDescription("Date d'expiration minimale");
        rules.add(dateRule);

        return rules;
    }
}