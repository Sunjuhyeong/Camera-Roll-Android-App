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
    List<ImageData> findByImage_ID(String search);

    @Query("SELECT * FROM ImageData WHERE thema LIKE :search")
    List<ImageData> findByThema(String search);

    @Query("SELECT * FROM ImageData WHERE dataString LIKE :search")
    List<ImageData> findByPersonID(String search);

    @Query("SELECT * FROM ImageData WHERE image_ID LIKE :image_ID AND thema LIKE :thema")
    ImageData findFaceRectangle(String image_ID, String thema);

    @Insert
    void insert(ImageData imageData);

    @Update
    void update(ImageData imageData);

    @Delete
    void delete(ImageData imageData);

}