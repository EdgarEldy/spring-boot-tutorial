package com.edgareldy.springboottutorial.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.edgareldy.springboottutorial.config.StorageProperties;
import com.edgareldy.springboottutorial.exception.BusinessRuleException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

/**
 * Unit tests for {@link LocalFileStorageService}. Uses JUnit 5's
 * {@link TempDir} to get an isolated directory for each test class run,
 * avoiding leftover files from one test affecting another.
 * <p>
 * Created edgar.muhamyangabo on 11/17/26
 * Author : edgar.muhamyangabo
 * Date : 11/17/26
 * Project : spring-boot-tutorial
 */
class LocalFileStorageServiceTest {

    @TempDir
    Path tempDir;

    private LocalFileStorageService storageService;

    @BeforeEach
    void setUp() {
        StorageProperties props = new StorageProperties(tempDir.toString(), 5);
        storageService = new LocalFileStorageService(props);
    }

    @Test
    void _01_ShouldStoreFile_WhenValidJpeg() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", MediaType.IMAGE_JPEG_VALUE, "jpg-bytes".getBytes());

        String url = storageService.store(file, "products");

        assertThat(url).startsWith("/uploads/products/").endsWith(".jpg");
        // Verify the file actually landed on disk
        String filename = url.substring("/uploads/products/".length());
        assertThat(Files.exists(tempDir.resolve("products").resolve(filename))).isTrue();
    }

    @Test
    void _02_ShouldRejectFile_WhenExtensionNotAllowed() {
        MockMultipartFile gif = new MockMultipartFile(
                "file", "animation.gif", "image/gif", "gif-bytes".getBytes());

        assertThatThrownBy(() -> storageService.store(gif, "products"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("File type not allowed");
    }

    @Test
    void _03_ShouldRejectFile_WhenTooLarge() {
        // 6 MB > 5 MB limit
        byte[] oversized = new byte[6 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile(
                "file", "big.jpg", MediaType.IMAGE_JPEG_VALUE, oversized);

        assertThatThrownBy(() -> storageService.store(file, "products"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("exceeds the maximum");
    }

    @Test
    void _04_ShouldRejectFile_WhenEmpty() {
        MockMultipartFile empty = new MockMultipartFile(
                "file", "empty.jpg", MediaType.IMAGE_JPEG_VALUE, new byte[0]);

        assertThatThrownBy(() -> storageService.store(empty, "products"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("must not be empty");
    }

    @Test
    void _05_ShouldRejectSubdirectory_WhenPathTraversalAttempted() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", MediaType.IMAGE_JPEG_VALUE, "jpg-bytes".getBytes());

        assertThatThrownBy(() -> storageService.store(file, "../secret"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Invalid storage subdirectory");
    }

    @Test
    void _06_ShouldDeleteFile_WhenItExists() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", MediaType.IMAGE_JPEG_VALUE, "jpg-bytes".getBytes());
        String url = storageService.store(file, "products");

        storageService.delete(url);

        String filename = url.substring("/uploads/products/".length());
        assertThat(Files.exists(tempDir.resolve("products").resolve(filename))).isFalse();
    }

    @Test
    void _07_ShouldIgnoreDelete_WhenUrlDoesNotStartWithUploads() {
        // Should not throw
        storageService.delete("/static/logo.png");
    }

    @Test
    void _08_ShouldRejectFile_WhenMimeTypeMismatch() {
        // .jpg extension (allowed) but image/gif content-type (not allowed)
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/gif", "gif-bytes".getBytes());

        assertThatThrownBy(() -> storageService.store(file, "products"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("MIME type not allowed");
    }
}
