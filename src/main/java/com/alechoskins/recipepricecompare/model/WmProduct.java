package com.alechoskins.recipepricecompare.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Transient;
import java.math.BigDecimal;

//Wal-Mart Product

@Getter
@Entity
@NoArgsConstructor
public class WmProduct {

    //Nested json object fields
    @Transient
    ImageInfo imageInfo;
    @Transient
    PriceInfo priceInfo;

    String name;
    @JsonProperty("usItemId")
    @Id
    String sku;
    @Setter
    BigDecimal price;
    @Setter
    String weight;
    @Setter
    BigDecimal centsPerOz; //calculate from price and weight, since unit of measurement varies
    @JsonProperty("canonicalUrl")
    String url;
    @Setter
    String thumbnailUrl;
    @Setter
    String userSearchTerm;

    //************************************* Methods ******************************************

    @Override
    public String toString(){
        return String.format(
                "Name:  %s | Sku: %s | Price: %s | Weight: %s | Cents Per Oz: %s | User Search Term: %s\n",
                name, sku, price, weight, centsPerOz, userSearchTerm
        );

    }
    //**************************** Nested json object classes *********************************
    @Getter
    public class ImageInfo{
        @JsonProperty("thumbnailUrl")
        String thumbnailUrl;
    }

    @NoArgsConstructor
    @Getter
    public class PriceInfo{
        CurrentPrice currentPrice;
        UnitPrice unitPrice;

        @NoArgsConstructor
        @Getter
        public class CurrentPrice{
            @JsonProperty("price")
            BigDecimal price;
        }
        @NoArgsConstructor
        @Getter
        public class UnitPrice{
            @JsonProperty("price")
            BigDecimal price;
            @JsonProperty("priceString")
            String priceString;
        }
    }
}
