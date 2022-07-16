package com.alechoskins.recipepricecompare.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Setter
@Getter
@NoArgsConstructor
public class WmProductDto {
    String name;
    String sku;
    BigDecimal price;
    String weight;
    BigDecimal centsPerOz;
    String url;
    String thumbnailUrl;
    String userSearchTerm;
}
