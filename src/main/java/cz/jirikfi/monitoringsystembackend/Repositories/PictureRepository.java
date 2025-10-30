package cz.jirikfi.monitoringsystembackend.Repositories;

import cz.jirikfi.monitoringsystembackend.Entities.Picture;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PictureRepository extends JpaRepository<Picture, UUID> {
    Picture findPictureByFilename(String filename);
}
