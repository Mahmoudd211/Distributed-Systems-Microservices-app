package com.homesvc.offer.config;

import com.homesvc.offer.model.Category;
import com.homesvc.offer.repo.CategoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class CategorySeedRunner {

    @Bean
    CommandLineRunner seedCategories(CategoryRepository categoryRepository) {
        return args -> {
            List<String> names = List.of("Plumbing", "Carpentry", "Electrical", "Cleaning", "Painting");
            for (String n : names) {
                if (categoryRepository.findByName(n).isEmpty()) {
                    Category c = new Category();
                    c.setName(n);
                    categoryRepository.save(c);
                }
            }
        };
    }
}
