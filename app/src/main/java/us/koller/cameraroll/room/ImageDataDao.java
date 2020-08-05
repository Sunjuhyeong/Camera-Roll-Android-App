package us.koller.cameraroll.room;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ImageDataDao {

    @Query("SELECT * FROM ImageData")
    List<ImageData> getAll();

    @Query("SELECT * FROM ImageData WHERE image_ID LIKE :search")
    ImageData findByImage_ID(String search);

    @Insert
    void insert(ImageData imageData);

    @Update
    void update(ImageData imageData);

    @Delete
    void delete(ImageData imageData);

}