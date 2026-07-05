package edgareldy.springboottutorial.repository;

import edgareldy.springboottutorial.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Spring Data JPA repository for {@link Category}.
 * <p>
 * Created edgar.muhamyangabo on 7/4/26
 * Author : edgar.muhamyangabo
 * Date : 7/4/26
 * Project : spring-boot-tutorial
 */
public interface CategoryRepository extends JpaRepository<Category, Long> {
}
