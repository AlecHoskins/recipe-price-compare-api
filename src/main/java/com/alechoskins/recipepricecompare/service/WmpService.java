package com.alechoskins.recipepricecompare.service;

import com.alechoskins.recipepricecompare.dto.WmProductDto;
import com.alechoskins.recipepricecompare.model.WmProduct;
import com.alechoskins.recipepricecompare.scraper.WmpScraper;
import com.alechoskins.recipepricecompare.repository.WmProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.util.ArrayList;
import java.util.List;

@Service
public class WmpService {

    WmpScraper scraper;
    WmProductRepository repo;

    @Autowired
    public WmpService(WmpScraper scraper, WmProductRepository repo){
        this.scraper=scraper;
        this.repo=repo;
    }

    public List<WmProductDto> getMatchingProducts(String userSearchTerm){

        List<WmProduct> matchingProducts = repo.findByUserSearchTerm(userSearchTerm);
        if(matchingProducts.isEmpty()){
            repo.saveAll(scraper.getProductList(userSearchTerm));
            matchingProducts=repo.findByUserSearchTerm(userSearchTerm);
        }
        return makeDtoList(matchingProducts);
    }

    private List<WmProductDto> makeDtoList(List<WmProduct> productList){
        List<WmProductDto> dtoList = new ArrayList<>();
        for (WmProduct product : productList){
            WmProductDto currDto = new WmProductDto();
            currDto.setName(product.getName());
            currDto.setSku(product.getSku());
            currDto.setPrice(product.getPrice());
            if(product.getWeight()!=null){
                currDto.setWeight(product.getWeight());
            }
            if(product.getCentsPerOz()!=null){
                currDto.setCentsPerOz(product.getCentsPerOz());
            }
            currDto.setUrl(product.getUrl());
            currDto.setThumbnailUrl(product.getThumbnailUrl());
            currDto.setUserSearchTerm(product.getUserSearchTerm());
            dtoList.add(currDto);
        }
        return dtoList;
    }
}
