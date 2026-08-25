package edgareldy.springboottutorial.actuator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import edgareldy.springboottutorial.actuator.AppInfoEndpoint.AppInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

/**
 * Unit test for {@link AppInfoEndpoint}, with {@link Environment} mocked.
 * <p>
 * Created edgar.muhamyangabo on 8/25/26
 * Author : edgar.muhamyangabo
 * Date : 8/25/26
 * Project : spring-boot-tutorial
 */
@ExtendWith(MockitoExtension.class)
class AppInfoEndpointTest {

    @Mock
    private Environment environment;

    @Test
    void appInfoReportsApplicationNameAndActiveProfiles() {
        when(environment.getProperty("spring.application.name")).thenReturn("spring-boot-tutorial");
        when(environment.getActiveProfiles()).thenReturn(new String[] {"dev"});

        AppInfoEndpoint endpoint = new AppInfoEndpoint(environment);
        AppInfo appInfo = endpoint.appInfo();

        assertThat(appInfo.applicationName()).isEqualTo("spring-boot-tutorial");
        assertThat(appInfo.activeProfiles()).containsExactly("dev");
        assertThat(appInfo.javaVersion()).isEqualTo(Runtime.version().toString());
    }
}
