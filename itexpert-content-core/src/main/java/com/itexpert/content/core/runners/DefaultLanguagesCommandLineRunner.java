package com.itexpert.content.core.runners;

import com.itexpert.content.core.handlers.LanguageHandler;
import com.itexpert.content.lib.models.Language;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
public class DefaultLanguagesCommandLineRunner implements CommandLineRunner {

    @Value("${app.defaultLanguages}")
    private String defaultLanguages;

    private final LanguageHandler languageHandler;

    DefaultLanguagesCommandLineRunner(LanguageHandler languageHandler) {
        this.languageHandler = languageHandler;
    }

    public void run(String... args) {
        this.start();
    }

    private void start() {
        if (ObjectUtils.isNotEmpty(defaultLanguages)) {
            List<String> languageCodes = (List<String>) CollectionUtils.arrayToList(defaultLanguages.trim().split(";"));
            List<Language> languages = languageCodes.stream()
                    .map(code -> {
                        Language language = new Language();
                        language.setId(UUID.randomUUID());
                        language.setCode(code);
                        return language;
                    })
                    .collect(Collectors.toList());

            languageHandler.findAll()
                    .hasElements()
                    .flatMapMany(hasLanguages -> {
                        if (!hasLanguages) {
                            return languageHandler.saveAll(languages)
                                    .doOnNext(lang -> log.info("Language '{}' saved", lang.getCode()))
                                    .onErrorResume(e -> {
                                        log.warn("Error while saving default languages", e);
                                        return Mono.empty();
                                    });
                        } else {
                            log.info("Default languages already exist, skipping initialization.");
                            return Flux.empty();
                        }
                    })
                    .subscribe();
        }
    }


}