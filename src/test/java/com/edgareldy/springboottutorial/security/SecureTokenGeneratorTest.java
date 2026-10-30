package com.edgareldy.springboottutorial.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SecureTokenGenerator}.
 * <p>
 * Created edgar.muhamyangabo on 10/30/26
 * Author : edgar.muhamyangabo
 * Date : 10/30/26
 * Project : spring-boot-tutorial
 */
class SecureTokenGeneratorTest {

    private final SecureTokenGenerator generator = new SecureTokenGenerator();

    @Test
    void _01_ShouldReturnNonBlankValue_WhenTokenIsGenerated() {
        assertThat(generator.generate()).isNotBlank();
    }

    @Test
    void _02_ShouldReturnDifferentValues_WhenTokensAreGeneratedRepeatedly() {
        String first = generator.generate();
        String second = generator.generate();

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void _03_ShouldProduceUrlSafeTokens_WhenManyTokensAreGenerated() {
        boolean allUrlSafe = IntStream.range(0, 100)
                .mapToObj(i -> generator.generate())
                .allMatch(token -> token.matches("[A-Za-z0-9_-]+"));

        assertThat(allUrlSafe).isTrue();
    }
}
