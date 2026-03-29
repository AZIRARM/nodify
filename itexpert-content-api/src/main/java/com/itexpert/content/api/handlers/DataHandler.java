package com.itexpert.content.api.handlers;

import com.itexpert.content.api.mappers.DataMapper;
import com.itexpert.content.api.repositories.ContentNodeRepository;
import com.itexpert.content.api.repositories.DataRepository;
import com.itexpert.content.lib.models.Data;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@AllArgsConstructor
@Service
public class DataHandler {
    private final DataRepository dataRepository;
    private final ContentNodeRepository contentNodeRepository;
    private final DataMapper dataMapper;

    public Flux<Data> findByContentCode(String code, Integer currentPage, Integer limit) {
        return dataRepository
                .findByContentNodeCode(code,
                        PageRequest.of(currentPage, limit, Sort.by(Sort.Order.by("creationDate")).descending()))
                .map(dataMapper::fromEntity);
    }

    public Mono<Data> findByContentNodeCodeAndKey(String contentCode, String key) {
        return dataRepository.findByContentNodeCodeAndKey(contentCode, key).map(dataMapper::fromEntity);
    }

    public Flux<Data> findByContentNodeCodeAndName(String contentCode, String Name) {
        return dataRepository.findByContentNodeCodeAndName(contentCode, Name).map(dataMapper::fromEntity);
    }

    public Mono<Data> save(Data data) {
        if (ObjectUtils.isEmpty(data.getId())) {
            data.setId(UUID.randomUUID());
            data.setCreationDate(Instant.now().toEpochMilli());
            data.setModificationDate(data.getCreationDate());
        } else {
            data.setModificationDate(Instant.now().toEpochMilli());
        }

        if (ObjectUtils.isEmpty(data.getKey())) {
            data.setKey(UUID.randomUUID().toString());
        }

        return dataRepository.save(dataMapper.fromModel(data))
                .map(dataMapper::fromEntity);
    }

    public Mono<Boolean> deleteAllByContentNodeCode(String contentNodeCode) {
        return dataRepository.deleteAllByContentNodeCode(contentNodeCode);
    }

    public Mono<Boolean> deleteByContentNodeCodeAndKey(String contentNodeCode, String key) {
        return dataRepository.deleteByContentNodeCodeAndKey(contentNodeCode, key);
    }

    public Mono<Boolean> delete(UUID uuid) {
        return dataRepository.deleteById(uuid).then(Mono.fromCallable(() -> true));
    }

    public Mono<Long> countAll() {
        return dataRepository.count();
    }

    public Mono<Long> countByContentCode(String code) {
        return dataRepository.countByContentNodeCode(code);
    }

    public Mono<Long> countByContentNodeCodeAndDataType(String code, String dataType) {
        return dataRepository.countByContentNodeCodeAndDataType(code, dataType);
    }

    public Mono<Long> countByContentNodeCodeAndUser(String code, String user) {
        return dataRepository.countByContentNodeCodeAndUser(code, user);
    }

    public Mono<Long> countByDataType(String dataType) {
        return dataRepository.countByDataType(dataType);
    }

    public Mono<Long> countByUser(String user) {
        return dataRepository.countByUser(user);
    }

    public Mono<Data> findById(UUID id) {
        return dataRepository.findById(id).map(dataMapper::fromEntity);
    }

    public Flux<Data> findAll(Integer currentPage, Integer limit) {
        return dataRepository
                .findAllBy(PageRequest.of(currentPage, limit, Sort.by(Sort.Order.by("creationDate")).descending()))
                .map(dataMapper::fromEntity);
    }

    public Flux<Data> findByDataType(String dataType, Integer currentPage, Integer limit) {
        return dataRepository
                .findByDataType(dataType,
                        PageRequest.of(currentPage, limit, Sort.by(Sort.Order.by("creationDate")).descending()))
                .map(dataMapper::fromEntity);
    }

    public Flux<Data> findByUser(String user, Integer currentPage, Integer limit) {
        return dataRepository
                .findByUser(user,
                        PageRequest.of(currentPage, limit, Sort.by(Sort.Order.by("creationDate")).descending()))
                .map(dataMapper::fromEntity);
    }

    public Flux<Data> searchByKeyword(String contentCode, String keyword, Integer currentPage, Integer limit) {
        return dataRepository
                .searchByContentNodeCodeAndKeyword(contentCode, keyword,
                        PageRequest.of(currentPage, limit, Sort.by(Sort.Order.by("creationDate")).descending()))
                .map(dataMapper::fromEntity);
    }

    public Mono<Data> update(UUID id, Data data) {
        data.setId(id);
        return this.save(data);
    }
}
