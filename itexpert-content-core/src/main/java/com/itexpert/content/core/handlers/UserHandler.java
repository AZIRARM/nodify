package com.itexpert.content.core.handlers;

import com.itexpert.content.core.mappers.UserMapper;
import com.itexpert.content.core.repositories.NodeRepository;
import com.itexpert.content.core.repositories.UserRepository;
import com.itexpert.content.core.utils.auth.PBKDF2Encoder;
import com.itexpert.content.lib.enums.NotificationEnum;
import com.itexpert.content.lib.enums.StatusEnum;
import com.itexpert.content.lib.models.UserPassword;
import com.itexpert.content.lib.models.UserPost;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
@Service
public class UserHandler {
    private final UserRepository userRepository;
    private final NodeRepository nodeRepository;
    private final UserMapper userMapper;
    private final PBKDF2Encoder passwordEncoder;
    private final NotificationHandler notificationHandler;

    public Flux<UserPost> findAll() {
        return userRepository.findAll().map(userMapper::fromEntity);
    }

    public Mono<UserPost> findById(UUID uuid) {
        return userRepository.findById(uuid).switchIfEmpty(Mono.empty()).map(userMapper::fromEntity);
    }

    public Mono<UserPost> save(UserPost user) {
        // Étape 1 : Nettoyer la liste des projets
        return filterRedundantProjects(user.getProjects())
                .doOnNext(user::setProjects) // On met à jour l'objet avec la liste propre
                .flatMap(cleanedCodes -> {

                    // Cas : CRÉATION (Pas d'ID)
                    if (ObjectUtils.isEmpty(user.getId())) {
                        user.setId(UUID.randomUUID());
                        user.setPassword(passwordEncoder.encode(user.getPassword()));

                        return userRepository.findByEmail(user.getEmail())
                                .switchIfEmpty(
                                        userRepository.save(userMapper.fromModel(user)))
                                .map(userMapper::fromEntity)
                                .flatMap(
                                        model -> this.notify(model, NotificationEnum.CREATION_OR_UPDATE, Boolean.TRUE));
                    }

                    // Cas : UPDATE (ID présent)
                    else {
                        return userRepository.findById(user.getId())
                                .flatMap(userDb -> {
                                    // Optionnel : vérifier si l'email n'a pas changé si c'est interdit
                                    return userRepository.save(userMapper.fromModel(user));
                                })
                                .map(userMapper::fromEntity)
                                .flatMap(
                                        model -> this.notify(model, NotificationEnum.CREATION_OR_UPDATE, Boolean.TRUE));
                    }
                });
    }

    private Mono<List<String>> filterRedundantProjects(List<String> codes) {
        if (ObjectUtils.isEmpty(codes) || codes.size() <= 1) {
            return Mono.just(codes != null ? codes : Collections.emptyList());
        }

        // On traite chaque code en parallèle
        return Flux.fromIterable(codes)
                .flatMap(code -> isAncestorPresentInList(code, codes)
                        .map(hasAncestor -> new ProjectFilterResult(code, hasAncestor)))
                .filter(result -> !result.isHasAncestor()) // On ne garde que ceux qui n'ont pas de parent dans la liste
                .map(ProjectFilterResult::getCode)
                .collectList();
    }

    /**
     * Remonte récursivement l'arbre pour voir si un parent du 'code'
     * se trouve dans la liste 'selectedCodes'
     */
    private Mono<Boolean> isAncestorPresentInList(String code, List<String> selectedCodes) {
        return nodeRepository.findByCodeAndStatus(code, StatusEnum.SNAPSHOT.name())
                .flatMap(node -> {
                    String parentCode = node.getParentCode();

                    // Racine atteinte : pas d'ancêtre dans la liste
                    if (parentCode == null || parentCode.isEmpty()) {
                        return Mono.just(false);
                    }

                    // Parent direct trouvé dans la liste : redondance !
                    if (selectedCodes.contains(parentCode)) {
                        return Mono.just(true);
                    }

                    // Sinon, on continue de remonter
                    return isAncestorPresentInList(parentCode, selectedCodes);
                })
                // SI le noeud n'existe pas en base, on considère qu'il n'a pas d'ancêtre
                .defaultIfEmpty(false);
    }

    public Mono<Boolean> delete(UUID uuid) {
        return this.userRepository.findById(uuid)
                .flatMap(entity -> {
                    if (entity.getRoles() != null && entity.getRoles().contains("ADMIN")) {
                        return Mono.just(Boolean.FALSE);
                    }
                    return this.notify(this.userMapper.fromEntity(entity), NotificationEnum.DELETION, Boolean.TRUE)
                            .flatMap(notification -> this.userRepository.deleteById(uuid)
                                    .thenReturn(Boolean.TRUE))
                            .onErrorReturn(Boolean.FALSE);
                });
    }

    public Mono<UserPost> findByEmail(String username) {
        return userRepository.findByEmail(username).map(userMapper::fromEntity);
    }

    public Mono<Boolean> changePassword(UserPassword userPassword) {
        return userRepository.findById(UUID.fromString(userPassword.getUserId()))
                .map(user -> {
                    if (passwordEncoder.encode(user.getPassword()).equals(
                            passwordEncoder.encode(user.getPassword()))) {
                        user.setPassword(passwordEncoder.encode(userPassword.getNewPassword()));
                        return userRepository.save(user)
                                .map(this.userMapper::fromEntity)
                                .flatMap(userPost -> this.notify(userPost, NotificationEnum.PASSWORD_CHANGE,
                                        Boolean.FALSE))
                                .map(userBDD -> Boolean.TRUE);
                    }
                    return userRepository.save(user)
                            .map(this.userMapper::fromEntity)
                            .flatMap(userPost -> this.notify(userPost, NotificationEnum.PASSWORD_CHANGE, Boolean.FALSE))
                            .map(userPost -> Boolean.FALSE);
                })
                .flatMap(Mono::from)
                .switchIfEmpty(Mono.just(Boolean.FALSE));
    }

    public Mono<UserPost> notify(UserPost model, NotificationEnum type, Boolean notifyAll) {
        return Mono.just(model).flatMap(user -> {
            return notificationHandler
                    .create(type,
                            "",
                            user.getEmail(),
                            "USER",
                            "",
                            "",
                            notifyAll)
                    .map(notification -> model);
        });
    }

    private static class ProjectFilterResult {
        private final String code;
        private final boolean hasAncestor;

        public ProjectFilterResult(String code, boolean hasAncestor) {
            this.code = code;
            this.hasAncestor = hasAncestor;
        }

        public String getCode() {
            return code;
        }

        public boolean isHasAncestor() {
            return hasAncestor;
        }
    }
}
