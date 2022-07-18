package com.alechoskins.recipepricecompare.service;

import com.alechoskins.recipepricecompare.repository.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {
    private final UserRepo userRepo;

    @Autowired
    public UserService(final UserRepo userRepo){
        this.userRepo=userRepo;
    }
}
