package com.alechoskins.recipepricecompare.repository;

import com.alechoskins.recipepricecompare.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepo extends JpaRepository<User, String> {
}
