package com.edgareldy.springboottutorial.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edgareldy.springboottutorial.entity.Category;
import com.edgareldy.springboottutorial.entity.Product;
import com.edgareldy.springboottutorial.entity.Role;
import com.edgareldy.springboottutorial.entity.user.AppUser;
import com.edgareldy.springboottutorial.repository.AppUserRepository;
import com.edgareldy.springboottutorial.repository.CategoryRepository;
import com.edgareldy.springboottutorial.repository.ProductRepository;
import com.edgareldy.springboottutorial.repository.RoleRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Unit tests for {@link DataSeeder}, with all repositories and
 * {@link PasswordEncoder} mocked. Two independent idempotence guards are
 * exercised: the category/product block and the admin-user block.
 * <p>
 * Created edgar.muhamyangabo on 7/4/26
 * Author : edgar.muhamyangabo
 * Date : 7/4/26
 * Project : spring-boot-tutorial
 */
@ExtendWith(MockitoExtension.class)
class DataSeederTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private DataSeeder dataSeeder;

    @Test
    void _01_ShouldSeedCategoriesProductsAndAdminUser_WhenDatabaseIsEmpty() throws Exception {
        Role adminRole = Role.builder().id(1L).roleName("ADMIN").build();
        when(categoryRepository.count()).thenReturn(0L);
        when(categoryRepository.save(any(Category.class)))
                .thenReturn(Category.builder().id(1L).categoryName("Electronics").build())
                .thenReturn(Category.builder().id(2L).categoryName("Books").build());
        when(appUserRepository.count()).thenReturn(0L);
        when(roleRepository.findByRoleNameIgnoreCase("ADMIN")).thenReturn(Optional.of(adminRole));
        when(passwordEncoder.encode(any())).thenReturn("hashed");

        dataSeeder.seedDemoData().run();

        verify(categoryRepository, times(2)).save(any(Category.class));
        verify(productRepository, times(3)).save(any(Product.class));
        verify(appUserRepository).save(any(AppUser.class));
    }

    @Test
    void _02_ShouldSkipSeeding_WhenDataAlreadyExists() throws Exception {
        when(categoryRepository.count()).thenReturn(1L);
        when(appUserRepository.count()).thenReturn(1L);

        dataSeeder.seedDemoData().run();

        verify(categoryRepository, never()).save(any());
        verify(productRepository, never()).save(any());
        verify(appUserRepository, never()).save(any());
    }
}
