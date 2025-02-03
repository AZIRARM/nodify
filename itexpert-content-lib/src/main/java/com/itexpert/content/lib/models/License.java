package com.itexpert.content.lib.models;

import com.itexpert.content.lib.enums.LicenseTypeEnum;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
public class License {
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
