package com.thecodereveal.shopease.config;

import com.thecodereveal.shopease.auth.entities.*;
import com.thecodereveal.shopease.auth.repositories.*;
import com.thecodereveal.shopease.entities.*;
import com.thecodereveal.shopease.repositories.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;

@Component
@Profile("local")
public class LocalDataInitializer implements CommandLineRunner {
    private final CategoryRepository categories;
    private final ProductRepository products;
    private final UserDetailRepository users;
    private final AuthorityRepository authorities;
    private final PasswordEncoder encoder;

    public LocalDataInitializer(CategoryRepository categories, ProductRepository products,
            UserDetailRepository users, AuthorityRepository authorities, PasswordEncoder encoder) {
        this.categories=categories; this.products=products; this.users=users;
        this.authorities=authorities; this.encoder=encoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        Authority userRole=role("USER");
        Authority adminRole=role("ADMIN");
        account("demo@shopease.local", "Demo123!", userRole);
        account("admin@shopease.local", "Admin123!", adminRole);
        if (categories.count() != 0) return;
        String[] types={"T-Shirts", "Shirts", "Jeans", "Hoodies"};
        String[][] images={{"category-men-tshirts.png","category-men-shirts.jpg","category-men-jeans.jpg","category-men-hoodies.png"},
                           {"category-women-tshirts.png","category-women-coats.jpg","category-women-shorts.jpg","category-women-hoodies.jpg"}};
        int group=0;
        for (String code : List.of("MEN","WOMEN")) {
            Category category=Category.builder().name(code.equals("MEN") ? "Men" : "Women").code(code).description("Clothing for " + code.toLowerCase()).build();
            List<CategoryType> categoryTypes=new ArrayList<>();
            for (String type : types) categoryTypes.add(CategoryType.builder().name(type).code(type.toUpperCase()).description(type).category(category).build());
            category.setCategoryTypes(categoryTypes);
            categories.save(category);
            for (int i=0;i<types.length;i++) {
                Product product=Product.builder().name(category.getName()+" "+types[i]).slug(code.toLowerCase()+"-"+types[i].toLowerCase()).description("Comfortable everyday clothing. Local demo product.").brand("ShopEase").price(BigDecimal.valueOf(25+10*i)).rating(4.5f).isNewArrival(true).category(category).categoryType(categoryTypes.get(i)).build();
                List<ProductVariant> variants=new ArrayList<>();
                for (String size : List.of("S","M","L","XL")) variants.add(ProductVariant.builder().color("Black").size(size).stockQuantity(20).product(product).build());
                product.setProductVariants(variants);
                product.setResources(List.of(Resources.builder().name(product.getName()).url("/images/"+images[group][i]).isPrimary(true).type("image").product(product).build()));
                products.save(product);
            }
            group++;
        }
    }
    private Authority role(String code) {
        Authority role=authorities.findByRoleCode(code);
        return role != null ? role : authorities.save(Authority.builder().roleCode(code).roleDescription(code).build());
    }
    private void account(String email, String password, Authority role) {
        if (users.findByEmail(email) != null) return;
        User user=User.builder().firstName("Demo").lastName(role.getRoleCode()).email(email).password(encoder.encode(password)).enabled(true).provider("manual").authorities(List.of(role)).build();
        user.setAddressList(List.of(Address.builder().name("Demo").street("Av. Siempre Viva 123").city("Buenos Aires").state("Buenos Aires").zipCode("1000").phoneNumber("1100000000").user(user).build()));
        users.save(user);
    }
}
