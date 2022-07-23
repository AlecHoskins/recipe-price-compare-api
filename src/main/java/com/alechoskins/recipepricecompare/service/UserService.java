package com.alechoskins.recipepricecompare.service;

import com.alechoskins.recipepricecompare.dto.UserDto;
import com.alechoskins.recipepricecompare.model.User;
import com.alechoskins.recipepricecompare.repository.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {
    private final UserRepo userRepo;

    @Autowired
    public UserService(final UserRepo userRepo){
        this.userRepo=userRepo;
    }

    ///////////////////////////////////////////////// METHODS /////////////////////////////////////////////////////////
    public void register(UserDto userDto){

        if(!userRepo.existsById(userDto.getUsername())){
            User user = new User();
            user.setUsername(userDto.getUsername());
            user.setPassword(userDto.getPassword());

            userRepo.save(user);
        }
        else{
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User registration failed.");
        }
    }

}
