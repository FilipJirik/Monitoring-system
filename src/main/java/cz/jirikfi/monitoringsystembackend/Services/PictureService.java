package cz.jirikfi.monitoringsystembackend.Services;

import cz.jirikfi.monitoringsystembackend.Entities.Picture;
import cz.jirikfi.monitoringsystembackend.Repositories.PictureRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;


@Service
public class PictureService {
    @Autowired
    PictureRepository pictureDatabase;

    private final String DEFAULT_IMAGE_NAME = "default_white.png";

//    private static final String DEFAULT_IMAGE_PATH = "static/images/default_white.png";
//
//    public static byte[] loadDefaultPicture(){
//        try{
//            return new ClassPathResource(DEFAULT_IMAGE_PATH).getInputStream().readAllBytes();
//        }
//        catch(IOException e){
//            throw new RuntimeException("Unable to load default picture for device");
//        }
//    }

    public Picture getDefaultPicture() {
        Picture defaultPicture = pictureDatabase.findPictureByFilename(DEFAULT_IMAGE_NAME);

        if (defaultPicture == null) {
            return Picture.builder()
                    .id(GenerateUUIDService.v7())
                    .data(new byte[0])
                    .filename("default_not_found")
                    .build();
        }
        return defaultPicture;
    }
}
