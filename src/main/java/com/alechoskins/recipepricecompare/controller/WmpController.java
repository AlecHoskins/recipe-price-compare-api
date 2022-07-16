package com.alechoskins.recipepricecompare.controller;

import com.alechoskins.recipepricecompare.dto.WmProductDto;
import com.alechoskins.recipepricecompare.service.WmpService;
import com.alechoskins.recipepricecompare.repository.WmProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


import java.io.IOException;
import java.util.List;


@RestController
@RequestMapping(path = "/")
public class WmpController {

    WmpService wmpService;
    WmProductRepository repo;

    @Autowired
    public WmpController(WmProductRepository repo, WmpService wmpService) throws IOException {
        this.repo=repo;
        this.wmpService=wmpService;
    }

    @GetMapping("/search")
    public List<WmProductDto> getMatchingProducts(@RequestParam(value = "productToSearch", defaultValue = "spinach") String searchTerm){
        return wmpService.getMatchingProducts(searchTerm);
    }

}
