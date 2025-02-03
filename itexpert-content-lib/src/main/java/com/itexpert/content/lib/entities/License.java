package com.itexpert.content.lib.entities;

import com.itexpert.content.lib.enums.LicenseTypeEnum;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.UUID;

@Document(collection = "licenses")
@Data
public class License {
    @Id
    private UUID id;

    private String userName;
    private String licence;

    private LicenseTypeEnum type;

    private String product;
    private String version;
    private String customer;

    private Long creationDate;
    private Long modificationDate;

    private Long startDate;
    private Long endDate;

    private Integer countLicencesRequested;
}
