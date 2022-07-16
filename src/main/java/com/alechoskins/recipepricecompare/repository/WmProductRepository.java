package com.alechoskins.recipepricecompare.repository;


import com.alechoskins.recipepricecompare.model.WmProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WmProductRepository extends JpaRepository<WmProduct, String> {
    List<WmProduct> findByUserSearchTerm(String userSearchTerm);
}

