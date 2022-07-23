package com.alechoskins.recipepricecompare.dto;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotEmpty;

@Getter
@Setter
public class UserDto {

    @NotEmpty
    private String username;
    @NotEmpty
    private String password;

}
