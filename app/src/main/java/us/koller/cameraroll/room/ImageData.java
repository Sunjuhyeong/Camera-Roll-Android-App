package us.koller.cameraroll.room;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class ImageData {
    @PrimaryKey(autoGenerate = true)
    public int ID;

    @ColumnInfo
    public String image_ID;
    public String folderName;
    public String dataString;
    //thema는 "Describe"와 "OCR"이 존재
    public String thema;

    public ImageData(String image_ID, String folderName, String thema, String dataString){
        this.image_ID = image_ID;
        this.folderName = folderName;
        this.thema = thema;
        this.dataString = dataString;
    }

    public int getID() {
        return ID;
    }

    public void setID(int ID) {
        this.ID = ID;
    }

    public String getFolderName() {
        return folderName;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    public String getImage_ID() {
        return image_ID;
    }

    public void setImage_ID(String image_ID) {
        this.image_ID = image_ID;
    }

    public String getDataString() {
        return dataString;
    }

    public void setDataString(String dataString) {
        this.dataString = dataString;
    }

    public String getThema() {
        return thema;
    }

    public void setThema(String thema) {
        this.thema = thema;
    }

        @Override
    public String toString() {
        return "Data ID: " + this.ID + "\n\n"
                + "IMAGE NAME: " + this.image_ID + "\n\n"
                + "IMAGE FOLDER: " + this.folderName + "\n\n"
                + "IMAGE THEMA: " + this.thema + "\n\n"
                + "IMAGE DATA: " + this.dataString + "\n\n";
    }
}