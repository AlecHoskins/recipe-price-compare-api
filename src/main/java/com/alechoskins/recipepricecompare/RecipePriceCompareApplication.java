package com.alechoskins.recipepricecompare;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

//if UserRepo starts acting up, try uncommenting below annotation
//@EnableJpaRepositories(basePackageClasses = UserRepo.class)
@SpringBootApplication
public class RecipePriceCompareApplication {

	public static void main(String[] args) {
		SpringApplication.run(RecipePriceCompareApplication.class, args);
	}

}
