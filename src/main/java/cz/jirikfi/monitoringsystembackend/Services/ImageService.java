package cz.jirikfi.monitoringsystembackend.Services;

import cz.jirikfi.monitoringsystembackend.Exceptions.InternalErrorException;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Slf4j
@Service
public class ImageService {

    @Value("${app.pictures.upload-dir:uploads/pictures}")
    private String uploadDir;

    @Getter
    @Value("${app.pictures.default-filename:default.png}")
    private String defaultFilename;

    private Path uploadPath;

    @PostConstruct
    public void init() {
        try {
            uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);
            log.info("Image upload directory initialized: {}", uploadPath);

            ensureDefaultPictureExists();
        } catch (IOException e) {
            log.error("Failed to initialize image upload directory", e);
            throw new InternalErrorException("Failed to initialize image storage");
        }
    }

    private void ensureDefaultPictureExists() {
        Path defaultPath = uploadPath.resolve(defaultFilename);
        
        // If default file doesn't exist, create a placeholder
        if (!Files.exists(defaultPath)) {
            try {
                byte[] defaultImageData = createDefaultImage();

                Files.write(defaultPath, defaultImageData);
                log.info("Created default image file: {}", defaultPath);
            } catch (IOException e) {
                log.error("Failed to create default image file", e);
                throw new InternalErrorException("Failed to create default image");
            }
        }
    }
    private byte[] createDefaultImage() {
        // 1x1 transparent PNG
        return new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
            0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte) 0xC4,
            (byte) 0x89, 0x00, 0x00, 0x00, 0x0A, 0x49, 0x44, 0x41,
            0x54, 0x78, (byte) 0x9C, 0x63, 0x00, 0x01, 0x00, 0x00,
            0x05, 0x00, 0x01, 0x0D, 0x0A, 0x2D, (byte) 0xB4, 0x00,
            0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44, (byte) 0xAE,
            0x42, 0x60, (byte) 0x82
        };
    }

    public String saveImage(MultipartFile file) {
        if (file.isEmpty()) {
            throw new InternalErrorException("Cannot save empty file");
        }

        try {
            String originalFilename = file.getOriginalFilename();

            String extension = ".png"; // Fallback
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String filename = UUID.randomUUID() + extension;

            Path filePath = uploadPath.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Saved image file: {}", filename);

            return filename;

        } catch (IOException e) {
            log.error("Failed to save image file", e);
            throw new InternalErrorException("Failed to save image: " + e.getMessage());
        }
    }

    public void deleteImage(String filename) {
        if (filename == null || filename.equals(defaultFilename)) {
            throw new InternalErrorException("Cannot delete default image");
        }

        try {
            Path filePath = uploadPath.resolve(filename);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("Deleted image file: {}", filename);
            } else {
                log.warn("Image file not found for deletion: {}", filename);
            }
        } catch (IOException e) {
            log.error("Failed to delete image file: {}", filename, e);
            throw new InternalErrorException("Failed to delete image file");
        }
    }

    public Resource loadImageAsResource(String filename) {
        try {
            if (filename == null || filename.isBlank()) {
                filename = defaultFilename;
            }

            Path filePath = uploadPath.resolve(filename);
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                log.warn("Image file not found or not readable: {}", filename);
                // Return default image if requested image doesn't exist
                return loadDefaultImageAsResource();
            }
        } catch (Exception e) {
            log.error("Failed to load image resource: {}", filename, e);
            return loadDefaultImageAsResource();
        }
    }

    public Resource loadDefaultImageAsResource() {
        try {
            Path filePath = uploadPath.resolve(defaultFilename);
            Resource resource = new UrlResource(filePath.toUri());
            
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new InternalErrorException("Default image file not found");
            }
        } catch (MalformedURLException e) {
            log.error("Failed to load default image resource", e);
            throw new InternalErrorException("Failed to load default image");
        }
    }

    public String getContentType(String filename) {
        if (filename == null || filename.isBlank()) {
            return "image/png";
        }
        String lowerFilename = filename.toLowerCase();
        if (lowerFilename.endsWith(".jpg") || lowerFilename.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerFilename.endsWith(".png")) {
            return "image/png";
        } else if (lowerFilename.endsWith(".gif")) {
            return "image/gif";
        } else if (lowerFilename.endsWith(".webp")) {
            return "image/webp";
        }
        return "image/png"; // default
    }
}
