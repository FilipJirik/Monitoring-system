package cz.jirikfi.monitoringsystembackend.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

class ImageServiceTest {

    private ImageService imageService;

    // JUnit 5 creates a fresh temp directory before each test and cleans it up
    // after
    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        imageService = new ImageService();

        // Inject @Value fields that Spring would normally set
        ReflectionTestUtils.setField(imageService, "uploadDir", tempDir.toString());
        ReflectionTestUtils.setField(imageService, "defaultFilename", "default.png");

        // Call @PostConstruct manually — creates the upload directory and default image
        imageService.init();
    }

    // =====================================================================
    // saveImage()
    // =====================================================================
    @Nested
    @DisplayName("saveImage()")
    class SaveImage {

        @Test
        @DisplayName("Should save a valid PNG file and return its generated filename")
        void saveImage_ValidPng_SavesAndReturnsFilename() {
            // Arrange
            MockMultipartFile file = new MockMultipartFile(
                    "picture", "photo.png", "image/png", new byte[] { 1, 2, 3 });

            // Act
            String filename = imageService.saveImage(file);

            // Assert — filename is a UUID + .png, file exists on disk
            assertThat(filename).endsWith(".png");
            assertThat(Files.exists(tempDir.resolve(filename))).isTrue();
        }

        @Test
        @DisplayName("Should save a valid JPEG file")
        void saveImage_ValidJpeg_SavesAndReturnsFilename() {
            // Arrange
            MockMultipartFile file = new MockMultipartFile(
                    "picture", "photo.jpg", "image/jpeg", new byte[] { 1, 2, 3 });

            // Act
            String filename = imageService.saveImage(file);

            // Assert
            assertThat(filename).endsWith(".jpg");
            assertThat(Files.exists(tempDir.resolve(filename))).isTrue();
        }

        @Test
        @DisplayName("Should throw when file is null")
        void saveImage_NullFile_ThrowsIllegalArgumentException() {
            // Act & Assert
            assertThatThrownBy(() -> imageService.saveImage(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("empty");
        }

        @Test
        @DisplayName("Should throw when file is empty")
        void saveImage_EmptyFile_ThrowsIllegalArgumentException() {
            // Arrange
            MockMultipartFile emptyFile = new MockMultipartFile(
                    "picture", "photo.png", "image/png", new byte[0]);

            // Act & Assert
            assertThatThrownBy(() -> imageService.saveImage(emptyFile))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("Should throw when MIME type is not allowed")
        void saveImage_InvalidMimeType_ThrowsIllegalArgumentException() {
            // Arrange
            MockMultipartFile pdfFile = new MockMultipartFile(
                    "picture", "document.pdf", "application/pdf", new byte[] { 1, 2, 3 });

            // Act & Assert
            assertThatThrownBy(() -> imageService.saveImage(pdfFile))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid file type");
        }
    }

    // =====================================================================
    // deleteImage()
    // =====================================================================
    @Nested
    @DisplayName("deleteImage()")
    class DeleteImage {

        @Test
        @DisplayName("Should delete the file from disk")
        void deleteImage_FileExists_DeletesIt() throws IOException {
            // Arrange — create a file to delete
            Path fileToDelete = tempDir.resolve("to-delete.png");
            Files.write(fileToDelete, new byte[] { 1, 2, 3 });

            // Act
            imageService.deleteImage("to-delete.png");

            // Assert
            assertThat(Files.exists(fileToDelete)).isFalse();
        }

        @Test
        @DisplayName("Should not delete the default image")
        void deleteImage_DefaultFilename_DoesNothing() {
            // Arrange — default.png was created by init()

            // Act
            imageService.deleteImage("default.png");

            // Assert — default.png still exists
            assertThat(Files.exists(tempDir.resolve("default.png"))).isTrue();
        }

        @Test
        @DisplayName("Should do nothing when filename is null")
        void deleteImage_NullFilename_DoesNothing() {
            // Act & Assert — no exception thrown
            imageService.deleteImage(null);
        }
    }

    // =====================================================================
    // loadImageAsResource()
    // =====================================================================
    @Nested
    @DisplayName("loadImageAsResource()")
    class LoadImageAsResource {

        @Test
        @DisplayName("Should load an existing image as a resource")
        void loadImageAsResource_FileExists_ReturnsResource() throws IOException {
            // Arrange — create a test image
            Files.write(tempDir.resolve("test-image.png"), new byte[] { 1, 2, 3 });

            // Act
            Resource result = imageService.loadImageAsResource("test-image.png");

            // Assert
            assertThat(result.exists()).isTrue();
        }

        @Test
        @DisplayName("Should fall back to default image when file does not exist")
        void loadImageAsResource_FileMissing_ReturnsDefaultImage() {
            // Act — request a file that doesn't exist
            Resource result = imageService.loadImageAsResource("nonexistent.png");

            // Assert — returns default image instead
            assertThat(result.exists()).isTrue();
        }

        @Test
        @DisplayName("Should fall back to default image when filename is null or blank")
        void loadImageAsResource_NullFilename_ReturnsDefaultImage() {
            // Act
            Resource result = imageService.loadImageAsResource(null);

            // Assert
            assertThat(result.exists()).isTrue();
        }
    }

    // =====================================================================
    // getContentType()
    // =====================================================================
    @Nested
    @DisplayName("getContentType()")
    class GetContentType {

        @Test
        @DisplayName("Should return image/jpeg for .jpg files")
        void getContentType_Jpg_ReturnsImageJpeg() {
            assertThat(imageService.getContentType("photo.jpg")).isEqualTo("image/jpeg");
        }

        @Test
        @DisplayName("Should return image/png for .png files")
        void getContentType_Png_ReturnsImagePng() {
            assertThat(imageService.getContentType("photo.png")).isEqualTo("image/png");
        }

        @Test
        @DisplayName("Should return image/webp for .webp files")
        void getContentType_Webp_ReturnsImageWebp() {
            assertThat(imageService.getContentType("photo.webp")).isEqualTo("image/webp");
        }

        @Test
        @DisplayName("Should return default image/png for null or blank filename")
        void getContentType_NullOrBlank_ReturnsDefaultPng() {
            assertThat(imageService.getContentType(null)).isEqualTo("image/png");
            assertThat(imageService.getContentType("")).isEqualTo("image/png");
        }
    }
}
