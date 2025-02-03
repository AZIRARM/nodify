package com.itexpert.content.core.handlers;

import com.itexpert.content.core.helpers.RenameCodesHelper;
import com.itexpert.content.core.mappers.NodeMapper;
import com.itexpert.content.core.repositories.NodeRepository;
import com.itexpert.content.lib.enums.NotificationEnum;
import com.itexpert.content.lib.enums.StatusEnum;
import com.itexpert.content.lib.enums.TypeEnum;
import com.itexpert.content.lib.models.Node;
import com.itexpert.content.lib.models.Rule;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.data.convert.EntityConverter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.time.Instant;
import java.util.*;

@Slf4j
@AllArgsConstructor
@Service
public class NodeHandler {
    private final NodeRepository nodeRepository;
    private final NodeMapper nodeMapper;

    private final ContentNodeHandler contentNodeHandler;

    private final NotificationHandler notificationHandler;

    private final EnvironmentHandler environmentHandler;

    private final RenameCodesHelper renameCodesHelper;

    private final UserHandler userHandler;

    private final EntityConverter entityConverter;

    public Flux<Node> findAll() {
        return nodeRepository.findAll().map(nodeMapper::fromEntity);
    }

    public Mono<Node> findById(UUID uuid) {
        return nodeRepository.findById(uuid).map(nodeMapper::fromEntity);
    }


    public Mono<Node> findByCodeAndStatus(String code, String status) {
        return nodeRepository.findByCodeAndStatus(code, status).map(nodeMapper::fromEntity);
    }

    public Flux<Node> findByCode(String code) {
        return nodeRepository.findByCode(code).map(nodeMapper::fromEntity);
    }

    public Mono<Node> save(Node node) {
        if (ObjectUtils.isNotEmpty(node) && ObjectUtils.isNotEmpty(node.getId())) {
            return this.saveFactory(node, false);
        }
        return Mono.just(node).filter(model -> ObjectUtils.isEmpty(model.getId()))
                .map(element -> this.saveFactory(element, true))
                .flatMap(Mono::from)
                .doOnNext(node1 -> {
                    log.info("{} saved ", node1.getCode());
                });

    }


    private Mono<Node> saveFactory(Node model, boolean isCreation) {
        return Mono.just(model).map(node -> {
                    if (isCreation) {
                        node.setId(UUID.randomUUID());
                        node.setVersion("0");
                        node.setStatus(StatusEnum.SNAPSHOT);
                        node.setCreationDate(Instant.now().toEpochMilli());
                        node.setModificationDate(node.getCreationDate());
                    } else {
                        node.setModificationDate(Instant.now().toEpochMilli());
                    }

                    if (ObjectUtils.isEmpty(node.getRules())) {
                        node.setRules(getDefaultRules());
                    }
                    return node;
                })
                .map(nodeMapper::fromModel)
                .flatMap(nodeRepository::save)
                .map(nodeMapper::fromEntity)
                .flatMap(node -> this.notify(node, isCreation ? NotificationEnum.CREATION : NotificationEnum.UPDATE));

    }

    private List<Rule> getDefaultRules() {
        Rule ruleMaintenance = new Rule();
        ruleMaintenance.setName("MAINTENANCE");
        ruleMaintenance.setCode("MAINTENANCE");
        ruleMaintenance.setType(TypeEnum.BOOL);
        ruleMaintenance.setValue("false");
        ruleMaintenance.setBehavior(Boolean.FALSE);
        ruleMaintenance.setEnable(Boolean.FALSE);
        ruleMaintenance.setEditable(false);
        ruleMaintenance.setErasable(false);


        Rule activationDate = new Rule();
        activationDate.setName("ACTIVATION_DATE");
        activationDate.setCode("ACTIVATION_CODE");
        activationDate.setType(TypeEnum.DATE);
        activationDate.setBehavior(Boolean.FALSE);
        activationDate.setEnable(Boolean.FALSE);
        activationDate.setEditable(false);
        activationDate.setErasable(false);

        Rule endDate = new Rule();
        endDate.setName("DEACTIVATION_DATE");
        endDate.setCode("DEACTIVATION_CODE");
        endDate.setType(TypeEnum.DATE);
        endDate.setBehavior(Boolean.FALSE);
        endDate.setEnable(Boolean.FALSE);
        endDate.setEditable(false);
        endDate.setErasable(false);

        return List.of(ruleMaintenance, activationDate, endDate);
    }

    public Mono<Boolean> delete(String code, UUID userId) {
        return nodeRepository.findByCodeAndStatus(code, StatusEnum.SNAPSHOT.name())
                .map(node -> {
                    node.setStatus(StatusEnum.DELETED);
                    node.setModifiedBy(userId);
                    return node;
                }).flatMap(nodeRepository::save)
                .map(nodeMapper::fromEntity)
                .flatMap(model -> this.notify(model, NotificationEnum.DELETION))
                .map(unused -> Boolean.TRUE)
                .onErrorReturn(Boolean.FALSE);
    }

    public Mono<Boolean> deleteDefinitively(String code) {
        return this.findAllChildren(code)
                .flatMap(node -> this.contentNodeHandler.findAllByNodeCode(node.getCode()) // Récupère les contenus associés
                        .flatMap(contentNode -> this.contentNodeHandler.deleteDefinitively(contentNode.getCode()) // Supprime chaque contenu
                                .thenReturn(contentNode)) // Retourne le node supprimé pour garder la trace
                        .collectList() // Recueille tous les nodes supprimés dans une liste
                        .flatMap(deletedContentNodes -> this.notify(node, NotificationEnum.DELETION_DEFINITIVELY)) // Envoie la notification
                        .thenReturn(node) // Retourne le node parent
                )
                .map(Node::getCode)
                .collectList()
                .flatMap(this.nodeRepository::deleteAllByCode) // Supprime le node parent
                .hasElement(); // Vérifie si des éléments ont été supprimés
    }


    public Flux<Node> findParentsNodesByStatus(String status) {
        return nodeRepository.findParentsNodesByStatus(status)
                .map(nodeMapper::fromEntity);
    }


    public Flux<Node> findByCodeParent(String code) {
        return nodeRepository.findByCodeParent(code).map(nodeMapper::fromEntity);
    }

    public Flux<Node> findChildrenByCodeAndStatus(String code, String status) {
        return nodeRepository.findChildrenByCodeAndStatus(code, status).map(nodeMapper::fromEntity);
    }


    @Transactional
    public Mono<Node> publish(UUID nodeUuid, UUID userId) {
        return this.findById(nodeUuid)
                .map(entity -> this.findAllChildren(entity.getCode())
                        .collectList()
                        .doOnNext(nodes -> {
                            log.info(nodes.toString());
                        })
                        .flatMapIterable(nodes -> nodes)
                        .filter(node -> node.getStatus().equals(StatusEnum.SNAPSHOT))
                        .flatMap(node -> {

                            return this.nodeRepository.findByCodeAndStatus(node.getCode(), StatusEnum.PUBLISHED.name())
                                    .map(this.nodeMapper::fromEntity)
                                    .flatMap(toArchive -> this.archiveNode(toArchive, node, userId))
                                    .switchIfEmpty(this.publishNode(node, userId))
                                    .onErrorResume(error -> {
                                        // Log l'erreur si nécessaire
                                        System.err.println("Erreur détectée : " + error.getMessage());
                                        return this.publishNode(node, userId);
                                    });
                        })
                        .collectList()
                        .flatMapIterable(list -> list)
                        .map(this.nodeMapper::fromModel)
                        .flatMap(nodeRepository::save)
                        .map(nodeMapper::fromEntity)
                        .flatMap(model ->
                                this.contentNodeHandler.findAllByNodeCodeAndStatus(model.getCode(), StatusEnum.SNAPSHOT.name())
                                        .flatMap(contentNode -> this.contentNodeHandler.publish(contentNode.getId(), true, userId))
                                        .then(Mono.just(model)) // Assurez-vous que le modèle est retourné après la publication
                        )
                        .map(node -> node)
                        .collectList()
                        .flatMapIterable(list -> list)
                        .flatMap(model -> this.notify(model, NotificationEnum.DEPLOYMENT))
                ).flatMap(Mono::from);
    }

    private Mono<Node> publishNode(Node toPublish, UUID userId) {
        toPublish.setStatus(StatusEnum.PUBLISHED);
        toPublish.setModificationDate(Instant.now().toEpochMilli());
        toPublish.setPublicationDate(toPublish.getModificationDate());
        toPublish.setModifiedBy(userId);

        return this.nodeRepository.save(this.nodeMapper.fromModel(toPublish))
                .flatMap(this.nodeRepository::save)
                .map(this.nodeMapper::fromEntity)
                .map(toSnapshot -> {
                    toSnapshot.setId(UUID.randomUUID());
                    toSnapshot.setStatus(StatusEnum.SNAPSHOT);
                    toSnapshot.setVersion(Integer.toString(Integer.parseInt(toSnapshot.getVersion()) + 1));
                    toSnapshot.setModifiedBy(userId);
                    return toSnapshot;
                });
    }

    private Mono<Node> archiveNode(Node toArchive, Node origin, UUID userId) {

        toArchive.setStatus(StatusEnum.ARCHIVE);
        toArchive.setModificationDate(Instant.now().toEpochMilli());
        toArchive.setModifiedBy(userId);

        return this.nodeRepository.save(this.nodeMapper.fromModel(toArchive))
                .map(archived -> origin)
                .map(this.nodeMapper::fromModel)
                .map(toPublish -> {
                    toPublish.setStatus(StatusEnum.PUBLISHED);
                    toPublish.setModificationDate(Instant.now().toEpochMilli());
                    toPublish.setPublicationDate(toPublish.getModificationDate());
                    toPublish.setModifiedBy(userId);
                    return this.nodeRepository.save(toPublish)
                            .map(this.nodeMapper::fromEntity)
                            .map(toSnapshot -> {
                                toSnapshot.setId(UUID.randomUUID());
                                toSnapshot.setStatus(StatusEnum.SNAPSHOT);
                                toSnapshot.setVersion(Integer.toString(Integer.parseInt(toSnapshot.getVersion()) + 1));
                                toSnapshot.setModifiedBy(userId);
                                return toSnapshot;
                            });
                }).flatMap(Mono::from);

    }


    public Flux<Node> findAllByStatus(String status) {
        return nodeRepository.findAllByStatus(status).map(nodeMapper::fromEntity);
    }

    public Flux<Node> findAllByStatusAndUser(String status, String userEmail) {
        return Flux.from(
                userHandler.findByEmail(userEmail)
                        .map(userPost -> this.nodeRepository.findAllByStatus(status)
                                .filter(node -> userPost.getProjects().contains(node.getParentCodeOrigin()))
                                .map(nodeMapper::fromEntity))
        ).flatMap(Mono::from);
    }


    public Mono<Node> revert(String code, String version, UUID userId) {
        return this.nodeRepository.findByCodeAndStatus(code, StatusEnum.SNAPSHOT.name())
                .map(node -> {
                    node.setStatus(StatusEnum.ARCHIVE);
                    node.setModificationDate(Instant.now().toEpochMilli());
                    node.setModifiedBy(userId);
                    return node;
                }).flatMap(nodeRepository::save)
                .map(node -> node.getVersion())
                .flatMap(lastVersion -> nodeRepository.findByCodeAndVersion(code, version).map(node -> Tuples.of(lastVersion, node)))
                .map(tuple -> {
                    com.itexpert.content.lib.entities.Node node = tuple.getT2();
                    String lastVersion = tuple.getT1();
                    node.setVersion(Long.valueOf(Long.parseLong(lastVersion) + 1).toString());
                    node.setStatus(StatusEnum.SNAPSHOT);
                    node.setModifiedBy(userId);
                    node.setModificationDate(Instant.now().toEpochMilli());
                    return node;
                }).flatMap(nodeRepository::save)
                .map(nodeMapper::fromEntity)
                .flatMap(model -> this.notify(model, NotificationEnum.REVERT));

    }


    public Mono<Boolean> activate(String code, UUID userId) {
        return nodeRepository.findByCodeAndStatus(code, StatusEnum.DELETED.name())
                .map(node -> {
                    node.setStatus(StatusEnum.SNAPSHOT);
                    node.setModifiedBy(userId);
                    node.setModificationDate(Instant.now().toEpochMilli());
                    return node;
                }).flatMap(nodeRepository::save)
                .map(nodeMapper::fromEntity)
                .flatMap(model -> this.notify(model, NotificationEnum.REACTIVATION))
                .map(unused -> Boolean.TRUE)
                .onErrorReturn(Boolean.FALSE);
    }

    public Mono<Node> notify(Node model, NotificationEnum type) {
        return Mono.just(model).flatMap(node -> {
            return notificationHandler
                    .create(type,
                            node.getCode(),
                            model.getModifiedBy(),
                            "NODE",
                            model.getCode(),
                            model.getVersion())
                    .map(notification -> node);
        });
    }


    public Flux<Node> findParentsNodesByStatus(String status, String userEmail) {
        return userHandler.findByEmail(userEmail)
                .flatMapMany(userPost -> Flux.fromIterable(userPost.getProjects()))
                .flatMap(code -> this.nodeRepository.findByCodeAndStatus(code, status)
                        .map(nodeMapper::fromEntity));
    }

    public Flux<Node> findDeleted(String userEmail) {
        return userHandler.findByEmail(userEmail)
                .flatMapMany(userPost -> Flux.fromIterable(userPost.getProjects()))
                .flatMap(this::findDeletedFactory);
    }

    private Flux<Node> findDeletedFactory(String code) {
        return this.nodeRepository.findByCodeParentOriginAndStatus(code, StatusEnum.DELETED.name())
                .map(nodeMapper::fromEntity);
    }


    public Mono<Node> export(String code) {
        return this.nodeRepository.findByCodeAndStatus(code, StatusEnum.PUBLISHED.name())
                .map(nodeMapper::fromEntity)
                .map(node -> Tuples.of(node, this.contentNodeHandler.findAllByNodeCodeAndStatus(node.getCode(), StatusEnum.PUBLISHED.name())))
                .flatMap(tuple -> {
                    if (ObjectUtils.isEmpty(tuple.getT1().getContents())) {
                        tuple.getT1().setContents(new LinkedList<>());
                    }
                    return tuple.getT2().collectList().map(contentNodes -> {
                        tuple.getT1().setContents(contentNodes);
                        return tuple.getT1();
                    });
                });
    }

    public Flux<Node> exportAll(String code) {
        return this.nodeRepository.findByCodeAndStatus(code, StatusEnum.SNAPSHOT.name())
                .map(nodeMapper::fromEntity)
                .map(node -> {
                    node.setId(UUID.randomUUID());
                    node.setParentCode(null);
                    node.setParentCodeOrigin(null);
                    return node;
                })
                .flatMap(this::setContentNodeToExport)
                .map(node -> Tuples.of(Mono.just(node), this.findAllDescendants(node)))
                .flatMapMany(tuple2 -> {
                    return Flux.merge(tuple2.getT1(), tuple2.getT2());
                })
                .flatMap(node -> this.notify(node, NotificationEnum.EXPORT));
    }

    private Flux<Node> findAllDescendants(Node node) {
        return this.nodeRepository.findChildrenByCodeAndStatus(node.getCode(), StatusEnum.SNAPSHOT.name())
                .map(nodeMapper::fromEntity)
                .map(model -> {
                    model.setId(UUID.randomUUID());
                    return model;
                })
                .flatMap(this::setContentNodeToExport)
                .flatMap(child -> findAllDescendants(child).concatWith(Mono.just(child))
                        .switchIfEmpty(Mono.just(child))
                );

    }

    private Mono<Node> setContentNodeToExport(Node node) {
        return this.contentNodeHandler.findAllByNodeCodeAndStatus(node.getCode(), StatusEnum.SNAPSHOT.name())
                .map(contentNode -> {
                    contentNode.setId(UUID.randomUUID());
                    return contentNode;
                })
                .collectList()
                .flatMap(contentList -> {
                            node.setContents(contentList);
                            return Mono.just(node);
                        }
                );

    }


    public Mono<Node> importNode(Node model) {
        return this.findByCodeAndStatus(model.getCode(), StatusEnum.SNAPSHOT.name())
                .flatMap(existingNode -> {
                    // Si un nœud SNAPSHOT existe, archiver l'ancien et créer une nouvelle version
                    existingNode.setStatus(StatusEnum.ARCHIVE);
                    model.setVersion(Integer.toString(Integer.parseInt(existingNode.getVersion()) + 1));
                    model.setStatus(StatusEnum.SNAPSHOT);

                    // Sauvegarder l'ancien en ARCHIVE et le nouveau SNAPSHOT
                    return this.nodeRepository.save(nodeMapper.fromModel(existingNode))
                            .then(this.nodeRepository.save(this.nodeMapper.fromModel(model)));
                })
                .switchIfEmpty(
                        // Si aucun nœud SNAPSHOT n'existe, sauvegarder directement le modèle comme SNAPSHOT
                        Mono.defer(() -> {
                            model.setVersion("0");
                            model.setStatus(StatusEnum.SNAPSHOT);
                            return this.nodeRepository.save(this.nodeMapper.fromModel(model));
                        })
                )
                .flatMap(savedNode -> {
                    // Sauvegarder les contenus associés au nœud
                    return contentNodeHandler.saveAll(model.getContents())
                            .then(Mono.just(savedNode)); // Remplace thenReturn par then + Mono.just
                })
                .map(this.nodeMapper::fromEntity)
                .flatMap(node -> this.notify(node, NotificationEnum.IMPORT));
    }

    @Transactional
    public Flux<Node> importNodes(List<Node> nodes, String nodeParentCode) {
        // 1. Trouver efficacement le nœud parent (gestion des nœuds manquants de manière gracieuse)
        return this.nodeRepository.findByCodeAndStatus(nodeParentCode, StatusEnum.SNAPSHOT.name())
                .flatMapMany(nodeParent -> {
                    // Si le parent est trouvé, on met à jour les nœuds enfants
                    return
                            this.nodeRepository.findByCodeAndStatus(nodeParentCode, StatusEnum.SNAPSHOT.name())
                                    .doOnNext(existingNode -> {
                                        log.info(existingNode.getCode());
                                    })
                                    .map(node -> ObjectUtils.isEmpty(node.getParentCodeOrigin()) ? node.getCode() : nodeParent.getParentCodeOrigin())
                                    .map(parentCodeOrigin ->
                                            this.renameCodesHelper.changeNodesCodesAndReturnFlux(nodes, parentCodeOrigin)
                                                    .map(node -> {
                                                        // Si le parentCode et parentCodeOrigin sont vides, on les met à jour
                                                        if (ObjectUtils.isEmpty(node.getParentCode())) {
                                                            node.setParentCode(nodeParent.getCode());
                                                            node.setParentCodeOrigin(nodeParent.getCode());
                                                        } else {
                                                            node.setParentCodeOrigin(nodeParent.getCode());
                                                        }
                                                        return node;
                                                    }).flatMap(node -> this.nodeRepository.findByCodeAndStatus(node.getCode(), StatusEnum.SNAPSHOT.name())
                                                            .flatMap(existingNode -> {
                                                                existingNode.setStatus(StatusEnum.ARCHIVE);
                                                                existingNode.setModificationDate(Instant.now().toEpochMilli());

                                                                node.setVersion(Integer.toString(Integer.parseInt(existingNode.getVersion()) + 1));
                                                                node.setStatus(StatusEnum.SNAPSHOT);
                                                                node.setModificationDate(Instant.now().toEpochMilli());

                                                                return this.nodeRepository.save(existingNode)
                                                                        .then(Mono.just(node));
                                                            })
                                                            .switchIfEmpty(Mono.just(node).map(model -> {
                                                                model.setModificationDate(Instant.now().toEpochMilli());
                                                                model.setCreationDate(model.getModificationDate());
                                                                model.setVersion("0");
                                                                return model;
                                                            })))
                                    );


                }).flatMap(Flux::from)
                .switchIfEmpty(
                        // Si le nœud parent n'est pas trouvé, on cherche chaque nœud dans l'entrepôt et on l'archive si nécessaire
                        this.renameCodesHelper.changeNodesCodesAndReturnFlux(nodes, "")
                                .flatMap(node ->
                                        nodeRepository.findByCode(node.getCode())
                                                .flatMap(entity -> Mono.empty()) // Si un entity est trouvé, retourner Mono.empty
                                                .switchIfEmpty(Mono.just(node)) // Si aucun entity n'est trouvé, retourner le node
                                )
                                .map(o -> (Node) o)
                                .flatMap(node -> {
                                            // Si le nœud existe, on l'archive et on met à jour son état
                                            node.setStatus(StatusEnum.SNAPSHOT);
                                            node.setVersion("0");
                                            node.setModificationDate(Instant.now().toEpochMilli());
                                            node.setCreationDate(node.getModificationDate());
                                            return this.nodeRepository.save(nodeMapper.fromModel(node)).map(entity -> node);//Retourne le nœud mis à jour
                                        }
                                )
                ).collectList()  // Collecte tous les nœuds dans une liste
                .flatMapMany(nodesList -> {
                    // Une fois tous les nœuds collectés, on les sauvegarde en une seule opération
                    return Flux.fromIterable(nodesList)
                            .flatMap(this::importContent)  // Appliquer la logique de contenu
                            .map(nodeMapper::fromModel)    // Mapper le modèle à une entité si nécessaire
                            .flatMap(nodeRepository::save);  // Sauvegarde chaque nœud dans le dépôt
                }).map(nodeMapper::fromEntity)
                .flatMap(node -> this.notify(node, NotificationEnum.IMPORT));
    }


    private Mono<Node> importContent(Node node) {
        return Flux.fromIterable(node.getContents())
                .flatMap(this.contentNodeHandler::importContentNode)
                .collectList().hasElement().map(haveElements -> node)
                .map(this.nodeMapper::fromModel)
                .flatMap(this.nodeRepository::save)
                .map(this.nodeMapper::fromEntity);
    }

    public Mono<Boolean> haveContents(String code) {
        return this.contentNodeHandler.nodeHaveContents(code);
    }

    public Mono<Boolean> haveChilds(String code) {
        return this.nodeRepository.countDistinctByParentCode(code).map(count -> count > 0);
    }

    public Flux<Node> findAllChildren(String code) {
        // Utilisation d'un Set partagé pour éviter les doublons
        Set<String> visitedCodes = Collections.synchronizedSet(new HashSet<>());

        return findAllChildrenRecursive(code, visitedCodes)
                .groupBy(Node::getCode)
                .flatMap(g -> g.reduce((a, b) -> a.getCode().compareTo(b.getCode()) > 0 ? a : b));
    }

    private Flux<Node> findAllChildrenRecursive(String code, Set<String> visitedCodes) {
        // Évitez les cycles ou doublons en vérifiant si le code a déjà été visité
        if (!visitedCodes.add(code)) {
            return Flux.empty(); // Si déjà visité, retournez un flux vide
        }

        // Récupérez les enfants et appliquez la récursion
        return this.nodeRepository.findByCodeOrCodeParent(code)
                .flatMap(node -> findAllChildrenRecursive(node.getCode(), visitedCodes) // Recherche récursive
                        .startWith(this.nodeMapper.fromEntity(node))); // Inclure le nœud actuel
    }

    public Flux<Node> findParentOrigin() {
        return nodeRepository.findAllParentOrigin().map(nodeMapper::fromEntity);
    }
}

