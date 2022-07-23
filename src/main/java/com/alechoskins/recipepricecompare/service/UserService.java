package com.alechoskins.recipepricecompare.service;

import com.alechoskins.recipepricecompare.dto.UserDto;
import com.alechoskins.recipepricecompare.model.User;
import com.alechoskins.recipepricecompare.repository.UserRepo;
import com.alechoskins.recipepricecompare.security.model.Role;
import com.alechoskins.recipepricecompare.security.repository.RoleRepo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.persistence.EntityNotFoundException;
import javax.transaction.Transactional;

import static java.rmi.server.LogStream.log;

@Service @Transactional @Slf4j
public class UserService {
    private final UserRepo userRepo;
    private final RoleRepo roleRepo;

    @Autowired
    public UserService(final UserRepo userRepo, RoleRepo roleRepo){

        this.userRepo=userRepo;
        this.roleRepo=roleRepo;
    }

    ///////////////////////////////////////////////// METHODS /////////////////////////////////////////////////////////
    public boolean register(UserDto userDto){
        if(!userRepo.existsByUsername(userDto.getUsername())) {
            User user = new User();
            user.setUsername(userDto.getUsername());
            user.setPassword(userDto.getPassword());

            userRepo.save(user);
            log.info("User \"{}\" saved to database",user.getUsername());
            return true;
        }
        else{
            return false;
        }
    }

    public void saveRole(Role role){
        roleRepo.save(role);
        log.info("Role \"{}\" saved to database",role.getName());
    }

    public void addRoleToUser(String username, String roleName){
        User user = userRepo.findByUsername(username);
        Role role = roleRepo.findByName(roleName);
        user.getRoles().add(role);
        log.info("Added role \"{}\" to user \"{}\" to database",role.getName(),user.getUsername());
    }

    public User getUser(String username){
        return userRepo.findByUsername(username);
    }

}
