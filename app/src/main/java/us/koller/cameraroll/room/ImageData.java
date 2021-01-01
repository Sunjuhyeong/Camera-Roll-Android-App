package us.koller.cameraroll.room;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.google.gson.JsonArray;

import org.json.JSONArray;
import org.json.JSONException;

@Entity
public class ImageData {
    @PrimaryKey(autoGenerate = true)
    public int ID;

    @ColumnInfo
    public String image_ID;
    public String folderName;
    public String dataString;

    //thema는 "Describe", "OCR", "Face_mPersonID",
    //"Face_mFace_height", "Face_mFace_width", "Face_mFace_top", "Face_mFace_left"가 존재
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

//    public String getDescribe_tags() {
//        return describe_tags;
//    }
//
//    public void setDescribe_tags(String describe_tags) {
//        this.describe_tags = describe_tags;
//    }
//
//    //thema가 describe라면
//    public void setDescribe_tagsByDataString(String dataString) throws JSONException {
//        JSONArray jsonArray = new JSONArray(dataString);
//
//        if(jsonArray.length()>0){
//            String tagsString = jsonArray.getJSONObject(0)
//                    .getJSONObject("description")
//                    .getString("tags")
//                    .replace("[", "")
//                    .replace("]","");
//
//            this.describe_tags = tagsString;
//        }
//    }

    @Override
    public String toString() {
        return "Data ID: " + this.ID + "\n\n"
                + "IMAGE NAME: " + this.image_ID + "\n\n"
                + "IMAGE FOLDER: " + this.folderName + "\n\n"
                + "DATA THEMA: " + this.thema + "\n\n"
                + "DATA: " + this.dataString + "\n\n";
    }
}