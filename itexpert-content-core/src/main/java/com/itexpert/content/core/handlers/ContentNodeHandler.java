package com.itexpert.content.core.handlers;

import com.itexpert.content.core.helpers.ContentHelper;
import com.itexpert.content.core.helpers.ContentNodeSlugHelper;
import com.itexpert.content.core.mappers.ContentNodeMapper;
import com.itexpert.content.core.models.ContentNodePayload;
import com.itexpert.content.core.repositories.ContentNodeRepository;
import com.itexpert.content.core.repositories.NodeRepository;
import com.itexpert.content.core.utils.RulesUtils;
import com.itexpert.content.lib.enums.NotificationEnum;
import com.itexpert.content.lib.enums.StatusEnum;
import com.itexpert.content.lib.models.ContentNode;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@AllArgsConstructor
@Service
public class ContentNodeHandler {
    private final ContentNodeRepository contentNodeRepository;
    private final ContentNodeMapper contentNodeMapper;

    private final NodeRepository nodeRepository;

    private final UserHandler userHandler;
    private final NotificationHandler notificationHandler;
    private final DataHandler dataHandler;
    private final ContentHelper contentHelper;
    private final ContentNodeSlugHelper contentNodeSlugHelper;

    public Flux<ContentNode> findAll() {
        return contentNodeRepository.findAll().map(contentNode -> {
            return contentNodeMapper.fromEntity(contentNode);
        });
    }

    public Mono<ContentNode> findById(UUID uuid) {
        return contentNodeRepository.findById(uuid).map(contentNodeMapper::fromEntity);
    }

    public Mono<ContentNode> save(ContentNode contentNode) throws CloneNotSupportedException {
        return Mono.just(contentNode).filter(model -> ObjectUtils.isEmpty(model.getId()))
                .flatMap(model ->
                        contentNodeRepository.findByCodeAndStatus(contentNode.getCode(), StatusEnum.SNAPSHOT.name())
                                .map(entity -> Mono.empty())
                                .switchIfEmpty(saveFactory(contentNode, true))

                ).switchIfEmpty(this.saveFactory(contentNode, false));
    }

    private Mono saveFactory(ContentNode model, boolean isCreation) {
        return Mono.just(model).map(contentNode -> {
                    if (isCreation) {
                        contentNode.setId(UUID.randomUUID());
                        contentNode.setVersion("0");
                        contentNode.setStatus(StatusEnum.SNAPSHOT);
                        contentNode.setCreationDate(Instant.now().toEpochMilli());
                        contentNode.setModificationDate(contentNode.getCreationDate());
                    } else {
                        contentNode.setModificationDate(Instant.now().toEpochMilli());
                    }

                    if (ObjectUtils.isEmpty(contentNode.getRules())) {
                        contentNode.setRules(RulesUtils.getDefaultRules());
                    }

                    return contentNode;
                }).map(contentNodeMapper::fromModel)
                .flatMap(contentNodeRepository::save)
                .map(contentNodeMapper::fromEntity)
                .flatMap(node -> this.notify(node, isCreation ? NotificationEnum.DELETION : NotificationEnum.UPDATE));
    }

    public Mono<Boolean> delete(String code, UUID userId) {
        return contentNodeRepository.findByCodeAndStatus(code, StatusEnum.SNAPSHOT.name())
                .map(contentNode -> {
                    contentNode.setStatus(StatusEnum.DELETED);
                    contentNode.setModifiedBy(userId);
                    contentNode.setModificationDate(Instant.now().toEpochMilli());
                    return contentNode;
                }).flatMap(contentNodeRepository::save)
                .map(contentNode -> {
                    return contentNodeRepository.findByCodeAndStatus(code, StatusEnum.PUBLISHED.name()).map(entity -> {
                        entity.setStatus(StatusEnum.ARCHIVE);
                        return entity;
                    });
                }).flatMap(Mono::from)
                .flatMap(this.contentNodeRepository::save)
                .map(contentNodeMapper::fromEntity)
                .flatMap(model -> this.notify(model, NotificationEnum.DELETION))
                .map(unused -> Boolean.TRUE)
                .onErrorReturn(Boolean.FALSE);
    }

    public Flux<ContentNode> findByNodeCodeAndStatus(String nodeCode, String status) {
        return contentNodeRepository.findByNodeCodeAndStatus(nodeCode, status)
                .map(contentNodeMapper::fromEntity);
    }

    public Mono<ContentNode> publish(UUID contentNodeUuid, Boolean publish, UUID userId) {
        return this.contentNodeRepository.findById(contentNodeUuid)
                .flatMap(contentNode ->
                        this.contentNodeRepository.findByCodeAndStatus(contentNode.getCode(), StatusEnum.PUBLISHED.name())
                                .flatMap(alreadyPublished -> {
                                    // Cas où un PUBLISHED existe
                                    alreadyPublished.setStatus(StatusEnum.ARCHIVE);
                                    alreadyPublished.setModifiedBy(userId);
                                    alreadyPublished.setModificationDate(Instant.now().toEpochMilli());

                                    return this.contentNodeRepository.save(alreadyPublished)
                                            .flatMap(archived -> this.contentNodeRepository.findByCodeAndStatus(archived.getCode(), StatusEnum.SNAPSHOT.name()))
                                            .flatMap(snapshotNode -> {
                                                snapshotNode.setStatus(StatusEnum.PUBLISHED);
                                                snapshotNode.setModifiedBy(userId);
                                                snapshotNode.setModificationDate(Instant.now().toEpochMilli());
                                                snapshotNode.setPublicationDate(snapshotNode.getModificationDate());

                                                return this.contentNodeRepository.save(snapshotNode)
                                                        .flatMap(publishedContent -> {
                                                            try {
                                                                // Création du SNAPSHOT en arrière-plan
                                                                long version = ObjectUtils.isNotEmpty(publishedContent.getVersion())
                                                                        ? Long.parseLong(publishedContent.getVersion()) + 1L
                                                                        : 1L;
                                                                com.itexpert.content.lib.entities.ContentNode newSnapshot = publishedContent.clone();
                                                                newSnapshot.setStatus(StatusEnum.SNAPSHOT);
                                                                newSnapshot.setModifiedBy(userId);
                                                                newSnapshot.setId(UUID.randomUUID());
                                                                newSnapshot.setVersion(Long.toString(version));
                                                                this.contentNodeRepository.save(newSnapshot).subscribe();

                                                                // Retourne le PUBLISHED final
                                                                return Mono.just(publishedContent);
                                                            } catch (CloneNotSupportedException e) {
                                                                return Mono.error(e);
                                                            }
                                                        });
                                            });
                                })
                                .switchIfEmpty(Mono.defer(() -> createFirstPublication(contentNodeUuid, publish)))
                )
                .map(contentNodeMapper::fromEntity)
                .flatMap(model -> this.notify(model, NotificationEnum.DEPLOYMENT));
    }


    private Mono<com.itexpert.content.lib.entities.ContentNode> createFirstPublication(UUID contentNodeUuid, Boolean publish) {
        if (publish) {
            return this.contentNodeRepository.findByIdAndStatus(contentNodeUuid, StatusEnum.SNAPSHOT)
                    .flatMap(contentNode -> {
                        contentNode.setStatus(StatusEnum.PUBLISHED);
                        contentNode.setModificationDate(Instant.now().toEpochMilli());
                        contentNode.setPublicationDate(contentNode.getModificationDate());

                        return this.contentNodeRepository.save(contentNode)
                                .flatMap(savedPublishedNode -> {
                                    // Crée le SNAPSHOT en flux réactif
                                    return Mono.fromCallable(() -> {
                                                com.itexpert.content.lib.entities.ContentNode newSnapshot = savedPublishedNode.clone();
                                                newSnapshot.setId(UUID.randomUUID());
                                                newSnapshot.setStatus(StatusEnum.SNAPSHOT);
                                                long version = ObjectUtils.isNotEmpty(savedPublishedNode.getVersion())
                                                        ? Long.parseLong(savedPublishedNode.getVersion()) + 1L
                                                        : 1L;
                                                newSnapshot.setVersion(Long.toString(version));
                                                return newSnapshot;
                                            })
                                            .flatMap(newSnapshot -> this.contentNodeRepository.save(newSnapshot))
                                            // On ignore le SNAPSHOT créé et on retourne le PUBLISHED
                                            .thenReturn(savedPublishedNode);
                                });
                    });
        }
        return Mono.empty();
    }


    public Flux<ContentNode> findAllByStatus(String status) {
        return contentNodeRepository.findAllByStatus(status).map(contentNodeMapper::fromEntity);
    }

    public Flux<ContentNode> findAllByNodeCode(String code) {
        return contentNodeRepository
                .findByNodeCode(code).map(contentNodeMapper::fromEntity);
    }

    public Flux<ContentNode> findAllByCode(String code) {
        return contentNodeRepository.findAllByCode(code)
                .map(contentNodeMapper::fromEntity);
    }

    public Mono<ContentNode> setPublicationStatus(ContentNode contentNode) {
        return this.contentNodeRepository.findByCodeAndStatus(contentNode.getCode(), StatusEnum.PUBLISHED.name())
                .map(entity -> {
                    if (Objects.equals(entity.getModificationDate(), contentNode.getModificationDate())) {
                        contentNode.setPublicationStatus(StatusEnum.PUBLISHED.name());
                    } else {
                        contentNode.setPublicationStatus(StatusEnum.SNAPSHOT.name());
                    }
                    return contentNode;
                })
                .switchIfEmpty(Mono.just(contentNode))
                .map(model -> {
                    if (ObjectUtils.isEmpty(model.getPublicationStatus())) {
                        model.setPublicationStatus(StatusEnum.NEW.name());
                    }
                    return model;
                });
    }


    public Mono<ContentNode> revert(String code, String version, UUID userId) {
        return this.contentNodeRepository.findByCodeAndStatus(code, StatusEnum.SNAPSHOT.name())
                .map(contentNode -> {
                    contentNode.setStatus(StatusEnum.ARCHIVE);
                    contentNode.setModifiedBy(userId);
                    contentNode.setModificationDate(Instant.now().toEpochMilli());
                    return contentNode;
                }).flatMap(contentNodeRepository::save)
                .map(contentNode -> contentNode.getVersion())
                .flatMap(lastVersion -> contentNodeRepository.findByCodeAndVersion(code, version).map(node -> Tuples.of(lastVersion, node)))
                .map(tuple -> {
                    com.itexpert.content.lib.entities.ContentNode contentNode = tuple.getT2();
                    String lastVersion = tuple.getT1();
                    contentNode.setVersion(Long.valueOf(Long.parseLong(lastVersion) + 1).toString());
                    contentNode.setStatus(StatusEnum.SNAPSHOT);
                    contentNode.setModifiedBy(userId);
                    contentNode.setModificationDate(Instant.now().toEpochMilli());
                    return contentNode;
                }).flatMap(contentNodeRepository::save)
                .map(contentNodeMapper::fromEntity)
                .flatMap(model -> this.notify(model, NotificationEnum.REVERT));

    }

    public Mono<Boolean> activate(String code, UUID userId) {
        return contentNodeRepository.findByCodeAndStatus(code, StatusEnum.DELETED.name())
                .map(contentNode -> {
                    contentNode.setStatus(StatusEnum.SNAPSHOT);
                    contentNode.setModifiedBy(userId);
                    return contentNode;
                }).flatMap(contentNodeRepository::save)
                .map(contentNodeMapper::fromEntity)
                .flatMap(model -> this.notify(model, NotificationEnum.REACTIVATION))
                .map(unused -> Boolean.TRUE)
                .onErrorReturn(Boolean.FALSE);
    }

    public Mono<ContentNode> notify(ContentNode model, NotificationEnum type) {
        return Mono.just(model).flatMap(contentNode -> {
            return notificationHandler
                    .create(type,
                            contentNode.getCode(),
                            model.getModifiedBy(),
                            "CONTENT_NODE",
                            model.getCode(),
                            model.getVersion())
                    .map(notification -> contentNode);
        });
    }

    public Flux<ContentNode> findDeleted(String userEmail) {
        return userHandler.findByEmail(userEmail)
                .flatMapMany(userPost -> Flux.fromIterable(userPost.getProjects()))
                .flatMap(this::findDeletedFactory);
    }

    private Flux<ContentNode> findDeletedFactory(String code) {
        return this.contentNodeRepository.findAllByStatus(StatusEnum.DELETED.name())
                .flatMap(contentNode ->
                        this.contentIsPartOfNodeCode(this.contentNodeMapper.fromEntity(contentNode), code));
    }

    private Mono<ContentNode> contentIsPartOfNodeCode(ContentNode contentNode, String code) {
        return this.nodeRepository.findByCodeAndStatus(contentNode.getParentCode(), StatusEnum.SNAPSHOT.name())
                .doOnNext(node -> {
                    boolean a = ObjectUtils.isNotEmpty(node) && ObjectUtils.isNotEmpty(code)
                            && (code.equals(node.getCode()) || code.equals(node.getParentCode()) || code.equals(node.getParentCodeOrigin()));
                    log.info(node.getCode());
                })
                .filter(node -> ObjectUtils.isNotEmpty(code)
                        && (code.equals(node.getCode()) || code.equals(node.getParentCode()) || code.equals(node.getParentCodeOrigin())))
                .map(node -> contentNode);
    }

    public Flux<ContentNode> findAllByNodeCodeAndStatus(String code, String status) {
        return this.contentNodeRepository.findByNodeCodeAndStatus(code, status)
                .map(contentNodeMapper::fromEntity);
    }

    public Flux<ContentNode> saveAll(List<ContentNode> contentNodes) {
        return Flux.fromIterable(contentNodes)
                .flatMap(contentNode ->
                        this.findByCodeAndStatus(contentNode.getCode(), StatusEnum.SNAPSHOT.name())
                                .flatMap(existingEntity -> {
                                    // Si une entité avec SNAPSHOT existe
                                    existingEntity.setStatus(StatusEnum.ARCHIVE);
                                    contentNode.setStatus(StatusEnum.SNAPSHOT);
                                    contentNode.setVersion(Integer.toString(Integer.parseInt(existingEntity.getVersion()) + 1));

                                    // Sauvegarde l'entité existante en ARCHIVE
                                    return contentNodeRepository.save(contentNodeMapper.fromModel(existingEntity))
                                            // Sauvegarde la nouvelle version
                                            .then(contentNodeRepository.save(contentNodeMapper.fromModel(contentNode)));
                                })
                                .switchIfEmpty(
                                        // Si aucune entité n'existe, sauvegarder directement
                                        Mono.defer(() -> {
                                            contentNode.setStatus(StatusEnum.SNAPSHOT);
                                            contentNode.setVersion("0");
                                            return contentNodeRepository.save(contentNodeMapper.fromModel(contentNode));
                                        })
                                )
                )
                .flatMap(entity -> {
                    // Mapper l'entité sauvegardée pour retourner le DTO ContentNode
                    ContentNode dto = contentNodeMapper.fromEntity(entity);
                    return Mono.just(dto);
                });
    }


    public Mono<ContentNode> findByCodeAndStatus(String code, String status) {
        return contentNodeRepository.findByCodeAndStatus(code, status).map(contentNodeMapper::fromEntity);
    }


    @Transactional
    public Mono<ContentNode> importContentNode(ContentNode contentNode) {
        return this.findByCodeAndStatus(contentNode.getCode(), StatusEnum.SNAPSHOT.name())
                .flatMap(existingContentNode -> {
                    // Préparer le nouveau ContentNode
                    contentNode.setId(UUID.randomUUID());
                    contentNode.setVersion(String.valueOf(Integer.parseInt(existingContentNode.getVersion()) + 1));
                    contentNode.setStatus(StatusEnum.SNAPSHOT);
                    contentNode.setCreationDate(existingContentNode.getCreationDate());
                    contentNode.setModificationDate(Instant.now().toEpochMilli());
                    if(ObjectUtils.isNotEmpty(existingContentNode.getSlug())){
                        contentNode.setSlug(existingContentNode.getSlug());
                    }
                    contentNode.setFavorite(existingContentNode.isFavorite());

                    if (ObjectUtils.isNotEmpty(existingContentNode.getSlug())) {
                        contentNode.setSlug(existingContentNode.getSlug());
                    }

                    // Archiver l'ancien ContentNode
                    existingContentNode.setStatus(StatusEnum.ARCHIVE);
                    existingContentNode.setModificationDate(Instant.now().toEpochMilli());

                    // Sauvegarder l'ancien contenu archivé et retourner le nouveau SNAPSHOT
                    return this.contentNodeRepository.save(contentNodeMapper.fromModel(existingContentNode))
                            .thenReturn(contentNode);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    // Si aucun ContentNode existant trouvé, créer un nouveau SNAPSHOT
                    contentNode.setId(UUID.randomUUID());
                    contentNode.setVersion("0");
                    contentNode.setStatus(StatusEnum.SNAPSHOT);
                    contentNode.setCreationDate(Instant.now().toEpochMilli());
                    contentNode.setModificationDate(contentNode.getCreationDate());
                    return Mono.just(contentNode);
                }))
                // 🔹 Mise à jour du slug avec ContentNodeSlugHelper
                .flatMap(this.contentNodeSlugHelper::update)
                .flatMap(model -> this.contentNodeRepository.save(this.contentNodeMapper.fromModel(model)))
                .map(this.contentNodeMapper::fromEntity)
                .flatMap(model -> this.notify(model, NotificationEnum.IMPORT));
    }

    public Mono<Boolean> deleteDefinitively(String code) {
        return contentNodeRepository.findAllByCode(code)
                .flatMap(node -> this.contentNodeRepository.delete(node)
                        .map(unused -> this.dataHandler.deleteAllByContentNodeCode(code))
                        .map(response -> node)
                )
                .flatMap(model -> this.notify(this.contentNodeMapper.fromEntity(model), NotificationEnum.DELETION_DEFINITIVELY))
                .collectList()
                .map(unused -> Boolean.TRUE)
                .onErrorContinue((throwable, o) -> log.error(throwable.getMessage(), throwable));
    }

    public Mono<Boolean> nodeHaveContents(String code) {
        return this.contentNodeRepository.countDistinctByParentCode(code).map(count -> count > 0);
    }

    public Mono<ContentNode> deployContent(String contentNodeCode, String environmentCode) {
        return this.findByCodeAndStatus(contentNodeCode, StatusEnum.SNAPSHOT.name())
                .map(model -> {
                    model.setCode(model.getCode().replace(model.getParentCode().split("-")[1], environmentCode.split("-")[0]));
                    model.setParentCode(model.getParentCode().replace(model.getParentCode().split("-")[1], environmentCode.split("-")[0]));
                    model.setParentCodeOrigin(null);
                    return model;
                })
                .flatMap(contentNode ->
                        this.findByCodeAndStatus(contentNode.getCode(), StatusEnum.SNAPSHOT.name())
                                .flatMap(existingContentNode -> {

                                    contentNode.setId(UUID.randomUUID());
                                    contentNode.setParentCode(existingContentNode.getParentCode());
                                    contentNode.setVersion(String.valueOf(Integer.parseInt(existingContentNode.getVersion()) + 1));
                                    contentNode.setStatus(StatusEnum.SNAPSHOT);
                                    contentNode.setCreationDate(existingContentNode.getCreationDate());
                                    contentNode.setModificationDate(Instant.now().toEpochMilli());

                                    if (ObjectUtils.isNotEmpty(existingContentNode.getSlug())) {
                                        contentNode.setSlug(existingContentNode.getSlug());
                                    }

                                    existingContentNode.setStatus(StatusEnum.ARCHIVE);
                                    existingContentNode.setModificationDate(Instant.now().toEpochMilli());

                                    // Sauvegarder l'ancien contenu en ARCHIVE et retourner le nouveau SNAPSHOT
                                    return this.contentNodeRepository.save(contentNodeMapper.fromModel(existingContentNode))
                                            .thenReturn(contentNode);

                                })
                                .switchIfEmpty(Mono.just(contentNode).map(newContentNode -> {
                                    newContentNode.setId(UUID.randomUUID());
                                    newContentNode.setVersion("0");
                                    newContentNode.setStatus(StatusEnum.SNAPSHOT);
                                    newContentNode.setCreationDate(Instant.now().toEpochMilli());
                                    newContentNode.setModificationDate(newContentNode.getCreationDate());
                                    return newContentNode;
                                }))
                )
                // 🔹 Mise à jour du slug avec ContentNodeSlugHelper
                .flatMap(contentNodeSlugHelper::update)
                .flatMap(updatedContentNode -> this.contentNodeRepository.save(this.contentNodeMapper.fromModel(updatedContentNode)))
                .map(this.contentNodeMapper::fromEntity)
                .flatMap(model -> this.notify(model, NotificationEnum.DEPLOYMENT));
    }

    public Mono<ContentNode> fillContent(
            String code, StatusEnum status,
            ContentNodePayload contentNode) {
        return this.contentNodeRepository.findByCodeAndStatus(code, status.name())
                .map(entity -> {
                    entity.setContent(contentNode.getContent());
                    return entity;
                })
                .flatMap(entity -> this.contentHelper.fillContents(entity, status))
                .map(entity -> entity)
                .map(this.contentNodeMapper::fromEntity);
    }

    public Mono<Boolean> slugAlreadyExists(String code, String slug) {
        return this.contentNodeRepository.findBySlugAndCode(slug, code)
                .hasElements();
    }

    public Mono<Boolean> deleteById(UUID id) {
        return this.contentNodeRepository.findById(id)
                .map(this.contentNodeMapper::fromEntity)
                .flatMap(content ->
                        this.contentNodeRepository.deleteById(id).map(unused -> this.notify(content, NotificationEnum.DELETION_DEFINITIVELY)).then(Mono.just(content))
                ).hasElement();
    }
}

