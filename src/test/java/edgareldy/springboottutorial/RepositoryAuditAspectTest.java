package edgareldy.springboottutorial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import edgareldy.springboottutorial.aspect.RepositoryAuditAspect;
import edgareldy.springboottutorial.entity.Category;
import edgareldy.springboottutorial.repository.CategoryRepository;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Verifies {@link RepositoryAuditAspect} actually intercepts calls to
 * {@code categoryRepository.save(...)}, a method {@code CategoryRepository}
 * inherits from {@code CrudRepository} rather than declaring itself.
 * Confirmed empirically (not just by reading the pointcut expressions):
 * Spring AOP's {@code execution()} matching for a JDK proxy considers the
 * full interface hierarchy the proxy implements, so the advice fires even
 * though the join point's declaring class is {@code CrudRepository}, in a
 * different package than the {@code execution()} pointcut's type pattern.
 * <p>
 * Created edgar.muhamyangabo on 7/8/26
 * Author : edgar.muhamyangabo
 * Date : 7/8/26
 * Project : spring-boot-tutorial
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class RepositoryAuditAspectTest {

    @Autowired
    private CategoryRepository categoryRepository;

    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void attachAppender() {
        Logger logger = (Logger) LoggerFactory.getLogger(RepositoryAuditAspect.class);
        logger.setLevel(Level.DEBUG);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
    }

    @AfterEach
    void detachAppender() {
        ((Logger) LoggerFactory.getLogger(RepositoryAuditAspect.class)).detachAppender(appender);
    }

    @Test
    void saveTriggersBeforeAfterAndAfterReturning() {
        categoryRepository.save(Category.builder().categoryName("Aspect Probe").build());

        List<String> messages = appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
        assertThat(messages).anyMatch(msg -> msg.contains("About to call") && msg.contains("save"));
        assertThat(messages).anyMatch(msg -> msg.contains("Finished calling") && msg.contains("save"));
        assertThat(messages).anyMatch(msg -> msg.contains("persisted"));
    }

    @Test
    void savingInvalidEntityTriggersAfterThrowing() {
        assertThatThrownBy(() -> categoryRepository.save(Category.builder().categoryName(null).build()))
                .isInstanceOf(DataIntegrityViolationException.class);

        List<String> messages = appender.list.stream().map(ILoggingEvent::getFormattedMessage).toList();
        assertThat(messages).anyMatch(msg -> msg.contains("threw"));
    }
}
