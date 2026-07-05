package edgareldy.springboottutorial.dto.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for the {@link ApiResponse} static factories, checked here in
 * isolation since every controller and {@code GlobalExceptionHandler} builds
 * its response through them.
 * <p>
 * Created edgar.muhamyangabo on 7/4/26
 * Author : edgar.muhamyangabo
 * Date : 7/4/26
 * Project : spring-boot-tutorial
 */
class ApiResponseTest {

    @Test
    void successSetsSuccessTrueAndCarriesData() {
        ApiResponse<String> response = ApiResponse.success("payload", "created");

        assertThat(response.success()).isTrue();
        assertThat(response.message()).isEqualTo("created");
        assertThat(response.data()).isEqualTo("payload");
        assertThat(response.timestamp()).isNotNull();
    }

    @Test
    void errorSetsSuccessFalseAndNullData() {
        ApiResponse<String> response = ApiResponse.error("not found");

        assertThat(response.success()).isFalse();
        assertThat(response.message()).isEqualTo("not found");
        assertThat(response.data()).isNull();
        assertThat(response.timestamp()).isNotNull();
    }
}
