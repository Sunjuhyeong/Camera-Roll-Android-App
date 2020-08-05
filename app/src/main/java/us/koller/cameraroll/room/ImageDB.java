package us.koller.cameraroll.room;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {ImageData.class}, version = 1, exportSchema = false)
public abstract class ImageDB extends RoomDatabase {
    public abstract ImageDataDao imageDataDao();
    private static ImageDB INSTANCE;

    public static ImageDB getDatabase(final Context context){
        if(INSTANCE == null){
            synchronized (ImageDB.class){
                if(INSTANCE == null){
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            ImageDB.class,
                            "imagesInformation-db")
                            .allowMainThreadQueries()
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    //DB 객체제거
    public static void destroyInstance() {
        INSTANCE = null;
    }
}