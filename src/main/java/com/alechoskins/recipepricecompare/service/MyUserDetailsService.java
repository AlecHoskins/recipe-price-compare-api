package com.alechoskins.recipepricecompare.service;

import com.alechoskins.recipepricecompare.model.MyUserDetails;
import com.alechoskins.recipepricecompare.model.User;
import com.alechoskins.recipepricecompare.repository.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;


import java.util.Optional;

@Service
public class MyUserDetailsService implements UserDetailsService {

    @Autowired
    UserRepo userRepo;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<User> user = userRepo.findByUsername(username);

        user.orElseThrow(() -> new UsernameNotFoundException("No User Found For Username: "+username));

        return user.map(MyUserDetails::new).get();
    }

}
