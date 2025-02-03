package com.itexpert.content.lib.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserPost {

    private UUID id;

    private String firstname;

    private String lastname;

    private String email;

    private String password;

    private List<String> roles;

    private List<String> projects;

}
