package com.alechoskins.recipepricecompare.controller;

import com.alechoskins.recipepricecompare.dto.UserDto;
import com.alechoskins.recipepricecompare.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
public class UserController {

    @Autowired
    UserService userService;

    //login
    @PostMapping("/login")
    public void login(){
        
    }

    //register
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/register")
    public void register(@Valid @RequestBody UserDto userDto){
        userService.register(userDto);
    }

    //delete account

}
