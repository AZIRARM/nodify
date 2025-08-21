package com.itexpert.content.core.handlers;

import com.itexpert.content.core.helpers.NodeSlugHelper;
import com.itexpert.content.core.helpers.RenameNodeCodesHelper;
import com.itexpert.content.core.mappers.NodeMapper;
import com.itexpert.content.core.models.TreeNode;
import com.itexpert.content.core.repositories.NodeRepository;
import com.itexpert.content.core.utils.RulesUtils;
import com.itexpert.content.lib.enums.NotificationEnum;
import com.itexpert.content.lib.enums.StatusEnum;
import com.itexpert.content.lib.models.ContentNode;
import com.itexpert.content.lib.models.Node;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuples;

import java.nio.charset.StandardCharsets;
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

    private final RenameNodeCodesHelper renameNodeCodesHelper;

    private final UserHandler userHandler;

    private final NodeSlugHelper nodeSlugHelper;

    public Flux<Node> findAll() {
        return nodeRepository.findAll().map(nodeMapper::fromEntity);
    }

    public Mono<Node> findById(UUID uuid) {
        return nodeRepository.findById(uuid).map(nodeMapper::fromEntity);
    }


    public Mono<Node> findByCodeAndStatus(String code, String status) {
        return nodeRepository.findByCodeAndStatus(code, status).map(nodeMapper::fromEntity);
    }

    public Mono<Boolean> hasNodes() {
        return nodeRepository.count()
                .map(count -> count > 0);
    }

    public Flux<Node> findByCode(String code) {
        return nodeRepository.findByCode(code).map(nodeMapper::fromEntity);
    }

    public Mono<Node> save(Node node) {
        if (ObjectUtils.isNotEmpty(node) && ObjectUtils.isNotEmpty(node.getId())) {
            return this.saveFactory(node, false).doOnNext(node1 -> {
                log.info("Node {} with status {} saved", node1.getCode(), node1.getStatus());
            });
        }
        return Mono.just(node).filter(model -> ObjectUtils.isEmpty(model.getId()))
                .map(element -> this.saveFactory(element, true))
                .flatMap(Mono::from)
                .doOnNext(node1 -> {
                    log.info("Node {}, with status {} updated", node1.getCode(), node1.getStatus());
                });

    }

    public Flux<Node> saveAll(List<Node> nodes) {
        if (ObjectUtils.isEmpty(nodes)) {
            return Flux.empty();
        }
        return Flux.fromIterable(nodes).flatMap(this::save);
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
                        node.setRules(RulesUtils.getDefaultRules());
                    }

                    return node;
                })
                .map(nodeMapper::fromModel)
                .flatMap(nodeRepository::save)
                .map(nodeMapper::fromEntity)
                .flatMap(node -> this.notify(node, isCreation ? NotificationEnum.CREATION : NotificationEnum.UPDATE));

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
                .concatWith(this.findByCode(code))
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

    public Mono<Node> setPublicationStatus(Node node) {
        return this.nodeRepository.findByCodeAndStatus(node.getCode(), StatusEnum.PUBLISHED.name())
                .map(entity -> {
                    if (Objects.equals(entity.getModificationDate(), node.getModificationDate())) {
                        node.setPublicationStatus(StatusEnum.PUBLISHED.name());
                    } else {
                        node.setPublicationStatus(StatusEnum.SNAPSHOT.name());
                    }
                    return node;
                })
                .switchIfEmpty(Mono.just(node))
                .map(model -> {
                    if (ObjectUtils.isEmpty(model.getPublicationStatus())) {
                        model.setPublicationStatus(StatusEnum.NEW.name());
                    }
                    return model;
                });
    }


    public Flux<Node> findByCodeParent(String code) {
        return nodeRepository.findByCodeParent(code).map(nodeMapper::fromEntity);
    }

    public Flux<Node> findChildrenByCodeAndStatus(String code, String status) {
        return nodeRepository.findChildrenByCodeAndStatus(code, status)
                .map(nodeMapper::fromEntity);
    }


    @Transactional
    /**
     * Point d'entrée public pour la publication d'un noeud et de tous ses enfants.
     * @param nodeUuid L'UUID du noeud parent à publier.
     * @param userId L'ID de l'utilisateur qui effectue l'opération.
     * @return Un Mono contenant le noeud parent publié.
     */
    public Mono<Node> publish(UUID nodeUuid, UUID userId) {
        return this.findById(nodeUuid)
                .doOnNext(node -> {
                    log.info("Node: {}, status: {} ", node.getName(), node.getStatus().name());
                })
                .filter(parentNode -> parentNode.getStatus().equals(StatusEnum.SNAPSHOT))
                .switchIfEmpty(Mono.error(new IllegalStateException("Impossible de publier un noeud dont le statut n'est pas SNAPSHOT")))
                .flatMap(parentNode -> this.publishRecursive(parentNode, userId));
    }

    /**
     * Logique de publication récursive pour un noeud donné.
     * Cette méthode applique la logique complète (archivage si nécessaire, publication,
     * création d'un snapshot, publication des enfants) pour un seul noeud.
     *
     * @param nodeToProcess Le noeud à traiter.
     * @param userId        L'ID de l'utilisateur.
     * @return Un Mono contenant le noeud traité après sa publication.
     */
    private Mono<Node> publishRecursive(Node nodeToProcess, UUID userId) {
        log.info("Publish Node Parent {}, version {}", nodeToProcess.getCode(), nodeToProcess.getVersion());
        // Étape 1 : Publier le nœud parent
        return this.publishParentNode(nodeToProcess, userId)
                .flatMap(publishedParentNode ->
                        // Étape 2 : Publier tous les enfants (déjà trouvés)
                        this.findAllChildren(publishedParentNode.getCode())
                                .doOnNext(childreen -> {
                                    log.info("Publish Node Child {}, version {}", childreen.getCode(), childreen.getVersion());
                                })
                                .flatMap(childNode -> this.publishParentNode(childNode, userId))
                                .then(Mono.just(publishedParentNode))
                );
    }

    private Mono<Node> publishParentNode(Node nodeToProcess, UUID userId) {
        // Cette méthode gère la logique de publication d'un seul nœud parent et de son contenu
        return this.nodeRepository.findByCodeAndStatus(nodeToProcess.getCode(), StatusEnum.PUBLISHED.name())
                .flatMap(publishedParentNode ->
                        // Si une version publiée existe, l'archiver d'abord
                        this.archiveNode(this.nodeMapper.fromEntity(publishedParentNode), userId)
                )
                .then(
                        // Que l'archivage ait eu lieu ou non, publier le nœud actuel
                        this.publishNode(nodeToProcess, userId)
                )
                .flatMap(publishedParentNode ->
                        // Publier le contenu associé au nœud
                        this.contentNodeHandler.findAllByNodeCodeAndStatus(publishedParentNode.getCode(), StatusEnum.SNAPSHOT.name())
                                .flatMap(contentNode -> this.contentNodeHandler.publish(contentNode.getId(), true, userId))
                                .then(Mono.just(publishedParentNode))
                )
                .flatMap(finalNode ->
                        // Créer un nouveau snapshot du nœud
                        this.createSnapshot(finalNode, userId)
                )
                .flatMap(finalNode ->
                        // Envoyer la notification de déploiement
                        this.notify(finalNode, NotificationEnum.DEPLOYMENT)
                );
    }


    Mono<Node> createSnapshot(Node node, UUID userId) {
        try {
            Node snapshot = (Node) node.clone();
            snapshot.setId(UUID.randomUUID());
            snapshot.setStatus(StatusEnum.SNAPSHOT);
            snapshot.setVersion(Integer.toString(Integer.parseInt(node.getVersion()) + 1));
            node.setModifiedBy(userId);
            return this.nodeRepository.save(this.nodeMapper.fromModel(snapshot))
                    .map(saved -> node);
        } catch (CloneNotSupportedException cloneNotSupportedException) {
            return Mono.error(cloneNotSupportedException);
        }

    }


    Mono<Node> publishNode(Node toPublish, UUID userId) {
        toPublish.setStatus(StatusEnum.PUBLISHED);
        toPublish.setModificationDate(Instant.now().toEpochMilli());
        toPublish.setPublicationDate(toPublish.getModificationDate());
        toPublish.setModifiedBy(userId);

        return this.nodeRepository.save(this.nodeMapper.fromModel(toPublish))
                .map(this.nodeMapper::fromEntity);
    }

    public Mono<Node> archiveNode(Node toArchive, UUID userId) {

        toArchive.setStatus(StatusEnum.ARCHIVE);
        toArchive.setModificationDate(Instant.now().toEpochMilli());
        toArchive.setModifiedBy(userId);

        return this.nodeRepository.save(this.nodeMapper.fromModel(toArchive))
                .map(this.nodeMapper::fromEntity);

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

    public Mono<byte[]> exportAll(String code, String parentCodeOrigin) {
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
                .flatMap(node -> this.notify(node, NotificationEnum.EXPORT))
                .collectList()
                .flatMap(nodes -> renameNodeCodesHelper.changeCodesAndReturnJson(nodes, parentCodeOrigin, false))
                .map(jsons -> jsons.getBytes(StandardCharsets.UTF_8));
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
                    // Archiver l'ancien SNAPSHOT
                    existingNode.setStatus(StatusEnum.ARCHIVE);

                    // Préparer le nouveau SNAPSHOT
                    model.setVersion(Integer.toString(Integer.parseInt(existingNode.getVersion()) + 1));
                    model.setStatus(StatusEnum.SNAPSHOT);

                    // Mise à jour du slug pour le nouveau node
                    return nodeSlugHelper.update(model) // ou autre champ qui représente l'environnement
                            .flatMap(updatedModel ->
                                    // Sauvegarder l'ancien et le nouveau
                                    this.nodeRepository.save(nodeMapper.fromModel(existingNode))
                                            .then(this.nodeRepository.save(nodeMapper.fromModel(updatedModel)))
                            );
                })
                .switchIfEmpty(
                        Mono.defer(() -> {
                            model.setVersion("0");
                            model.setStatus(StatusEnum.SNAPSHOT);

                            // 🔹 Mise à jour du slug aussi pour la création initiale
                            return nodeSlugHelper.update(model)
                                    .flatMap(updatedModel ->
                                            this.nodeRepository.save(nodeMapper.fromModel(updatedModel))
                                    );
                        })
                )
                .flatMap(savedNode ->
                        // Sauvegarder les contenus associés
                        contentNodeHandler.saveAll(model.getContents())
                                .then(Mono.just(savedNode))
                )
                .map(this.nodeMapper::fromEntity)
                .flatMap(node -> this.notify(node, NotificationEnum.IMPORT));
    }

    @Transactional
    public Flux<Node> importNodes(List<Node> nodes, String nodeParentCode, Boolean fromFile) {
        return this.nodeRepository.findByCodeAndStatus(nodeParentCode, StatusEnum.SNAPSHOT.name())
                .flatMapMany(nodeParent -> {

                    String parentCodeOrigin = ObjectUtils.isEmpty(nodeParent.getParentCodeOrigin())
                            ? nodeParent.getCode()
                            : nodeParent.getParentCodeOrigin();

                    return this.renameNodeCodesHelper.changeNodesCodesAndReturnFlux(nodes, parentCodeOrigin, fromFile)
                            .map(node -> {
                                if (ObjectUtils.isEmpty(node.getParentCode())) {
                                    node.setParentCode(nodeParent.getCode());
                                    node.setParentCodeOrigin(nodeParent.getCode());
                                } else {
                                    node.setParentCodeOrigin(nodeParent.getCode());
                                }
                                return node;
                            })
                            .flatMap(node -> this.nodeRepository.findByCodeAndStatus(node.getCode(), StatusEnum.SNAPSHOT.name())
                                    .flatMap(existingNode -> {
                                        existingNode.setStatus(StatusEnum.ARCHIVE);
                                        existingNode.setModificationDate(Instant.now().toEpochMilli());

                                        node.setParentCode(existingNode.getParentCode());
                                        node.setParentCodeOrigin(existingNode.getParentCodeOrigin());
                                        node.setVersion(Integer.toString(Integer.parseInt(existingNode.getVersion()) + 1));
                                        node.setStatus(StatusEnum.SNAPSHOT);
                                        node.setModificationDate(Instant.now().toEpochMilli());
                                        if(ObjectUtils.isNotEmpty(existingNode.getSlug())) {
                                            node.setSlug(existingNode.getSlug());
                                        }
                                        node.setFavorite(existingNode.isFavorite());
                                        return this.nodeRepository.save(existingNode)
                                                .then(Mono.just(node));
                                    })
                                    .switchIfEmpty(Mono.just(node).map(model -> {
                                        model.setModificationDate(Instant.now().toEpochMilli());
                                        model.setCreationDate(model.getModificationDate());
                                        model.setVersion("0");
                                        return model;
                                    }))
                            );
                })
                .switchIfEmpty(
                        this.renameNodeCodesHelper.changeNodesCodesAndReturnFlux(nodes, "", fromFile)
                                .flatMap(node ->
                                        nodeRepository.findByCode(node.getCode())
                                                .flatMap(entity -> Mono.empty())
                                                .switchIfEmpty(Mono.just(node))
                                )
                                .map(o -> (Node) o)
                                .flatMap(node -> {
                                    node.setStatus(StatusEnum.SNAPSHOT);
                                    node.setVersion("0");
                                    node.setModificationDate(Instant.now().toEpochMilli());
                                    node.setCreationDate(node.getModificationDate());
                                    return this.nodeRepository.save(nodeMapper.fromModel(node))
                                            .map(entity -> node);
                                })
                )
                // Mise à jour du slug pour chaque node
                .flatMap(this.nodeSlugHelper::update)
                .collectList()
                .flatMapMany(nodesList -> Flux.fromIterable(nodesList)
                        .flatMap(this::importContent)
                        .map(nodeMapper::fromModel)
                        .flatMap(nodeRepository::save)
                )
                .map(nodeMapper::fromEntity)
                .flatMap(node -> this.notify(node, NotificationEnum.IMPORT));
    }


    private Mono<Node> importContent(Node node) {
        return Flux.fromIterable(Optional.ofNullable(node.getContents()).orElse(List.of()))
                .flatMap(this.contentNodeHandler::importContentNode)
                .collectList()
                .hasElement()
                .map(haveElements -> node)
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
        Set<String> visitedCodes = Collections.synchronizedSet(new HashSet<>());

        return findAllChildrenRecursive(code, visitedCodes)
                .filter(node -> !node.getCode().equals(code)) // <-- on exclut le parent
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
        return nodeRepository.findAllParentOrigin()
                .map(nodeMapper::fromEntity);
    }

    public Mono<Boolean> slugAlreadyExists(String code, String slug) {
        return this.nodeRepository.findBySlugAndStatusAndCodeNotIn(slug, StatusEnum.SNAPSHOT.name(), List.of(code))
                .doOnNext(node -> {
                    log.info(node.getCode());
                })
                .hasElements();
    }

    public Mono<TreeNode> generateTreeView(String code, List<String> userProjects) {
        return findByCodeAndStatus(code, StatusEnum.SNAPSHOT.name())
                .flatMap(node -> this.setContentsNodeWithStatus(node, StatusEnum.SNAPSHOT.name()))
                .flatMap(node -> this.buildTreeFromNode(node, userProjects));
    }

    private Mono<TreeNode> buildTreeFromNode(Node node, List<String> userProjects) {
        TreeNode treeNode = new TreeNode();
        treeNode.setName(node.getName());
        treeNode.setCode(node.getCode());
        if (ObjectUtils.isEmpty(node.getParentCode())) {
            treeNode.setType("NODIFY");
        }

        List<TreeNode> childreens = new ArrayList<>();

        if (node.getContents() != null) {
            for (ContentNode content : node.getContents()) {
                TreeNode leaf = new TreeNode();
                leaf.setName(ObjectUtils.isEmpty(content.getDescription()) ? content.getCode() : content.getDescription());
                leaf.setCode(content.getCode());
                leaf.setChildren(Collections.emptyList());
                leaf.setType(content.getType().name());
                leaf.setLeaf(Boolean.TRUE);
                childreens.add(leaf);
            }
        }

        return this.findAllByParentCodeAndStatus(node.getCode(), StatusEnum.SNAPSHOT.name())
                .filter(children -> userProjects.isEmpty() || userProjects.contains(children.getCode()) || userProjects.contains(children.getParentCode()) || userProjects.contains(children.getParentCodeOrigin()))
                .flatMap(parent -> setContentsNodeWithStatus(parent, StatusEnum.SNAPSHOT.name()))
                .flatMap(parent -> this.buildTreeFromNode(parent, userProjects))
                .collectList()
                .map(subTrees -> {
                    childreens.addAll(subTrees);
                    treeNode.setChildren(childreens);
                    return treeNode;
                });
    }

    private Mono<Node> setContentsNodeWithStatus(Node node, String status) {
        return contentNodeHandler.findAllByNodeCodeAndStatus(node.getCode(), status)
                .collectList()
                .map(contents -> {
                    node.setContents(contents);
                    return node;
                });
    }

    public Flux<Node> findAllByParentCodeAndStatus(String code, String name) {
        return this.nodeRepository.findAllByParentCodeAndStatus(code, StatusEnum.SNAPSHOT.name())
                .map(this.nodeMapper::fromEntity);
    }

    public Mono<Boolean> deleteById(UUID id) {
        return this.nodeRepository.findById(id)
                .map(this.nodeMapper::fromEntity)
                .flatMap(node ->
                        this.nodeRepository.deleteById(id).map(unused -> this.notify(node, NotificationEnum.DELETION_DEFINITIVELY)).then(Mono.just(node))
                ).hasElement();
    }
}

