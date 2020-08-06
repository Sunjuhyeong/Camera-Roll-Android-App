package us.koller.cameraroll.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.FaceServiceRestClient;
import com.microsoft.projectoxford.face.contract.FaceRectangle;
import com.microsoft.projectoxford.face.contract.Person;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import us.koller.cameraroll.R;
import us.koller.cameraroll.adapter.SearchAdapter;
import us.koller.cameraroll.data.models.Album;
import us.koller.cameraroll.data.models.AlbumItem;
import us.koller.cameraroll.room.ImageDB;
import us.koller.cameraroll.room.ImageData;

public class SearchActivity extends AppCompatActivity {

    private int AlbumCode = 39;
    private static final double FACE_RECT_SCALE_RATIO = 1.3;
    private String sub_key_face = null;
    private final String endpoint_face = "https://westus.api.cognitive.microsoft.com/face/v1.0/";
    private String mPersonGroupId;
    private FaceServiceClient faceServiceClient;
    private SearchAdapter adapter;
    private GridView gv;
    private File file;
    ArrayList<String> checkId = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        sub_key_face = getResources().getString(R.string.subscription_key_face);
        faceServiceClient = new FaceServiceRestClient(endpoint_face, sub_key_face);
        String searchResult = "";

        //DB 생성
        ImageDB db = ImageDB.getDatabase(this);

        //DB에 Thema가 Describe인 애들이랑 OCR인 애들을 부름
        List<ImageData> imageDataListDescribe = db.imageDataDao().findByThema("Describe");
        List<ImageData> imageDataListOCR = db.imageDataDao().findByThema("OCR");

        //파일 저장함.
        albumItemDescribe(imageDataListDescribe, searchResult);
        albumItemOCR(imageDataListOCR, searchResult);

        ImageSaver forPath = new ImageSaver(this);
        String folder = "/data/user/0/us.koller.cameraroll.debug/app_temp2";

        Album album = new Album().setPath(folder);



        //todo: 주형이형이 쓴 코드
//        mPersonGroupId = getIntent().getStringExtra("mPersonGroupID");
//        adapter = new SearchAdapter();
//        gv = (GridView)findViewById(R.id.faceGridView);
//
//        new getPersonListTask().execute();
    }

    private class getPersonListTask extends AsyncTask<Void,String, Person[]> {

        @Override
        protected Person[] doInBackground(Void... params) {

            try {
                Person[] result = faceServiceClient.listPersonsInLargePersonGroup(mPersonGroupId);
                if (result == null)
                {
                    return null;
                }
                return result;
            } catch (Exception e) {
                publishProgress("get PersonList failed");
                return null;
            }
        }

        @Override
        protected void onPostExecute(Person[] personList) {
            if (personList == null) {
                return;
            }
            for (Person person : personList) {
                try {
                    Bitmap thumbnail = getFaceThumbnail(person);
                    adapter.addItem(thumbnail);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            gv.setAdapter(adapter);
            gv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view,
                                        int position, long id) {
                    String targetId = personList[position].personId.toString();
                    String folderName = "/data/user/0/us.koller.cameraroll.debug/app_tempFace";

                    //todo : Make Album of selected person
                    ImageDB db = ImageDB.getDatabase(view.getContext());
                    List<ImageData> personList = db.imageDataDao().findByPersonID(targetId);

                    albumItemFace(personList);
                    Album album = new Album().setPath(folderName);

                    //todo: 주형이형이 쓴 코드
//                    album.setCached(true);
//                    file = new File(getApplicationContext().getFilesDir(), "cache"+ targetId);
//                    album.setPath(file.getPath());
//                    startAlbumActivity(album);
                }
            });
        }
    }

    private Bitmap getFaceThumbnail(Person person) throws IOException {
        String personID = person.personId.toString();
        FaceRectangle faceRectangle = new FaceRectangle();
        Bitmap bitmap = null;
        String path = null;
        String personImageID = null;

        //todo: get faceRect & image path From DB
        // by personID
        //DB 생성
        ImageDB db = ImageDB.getDatabase(this);
        List<ImageData> personImageList = db.imageDataDao().findByPersonID(personID);
        ImageData temp = personImageList.get(0);
        personImageID = temp.getImage_ID();
        path = temp.getFolderName();

        faceRectangle.height = Integer.parseInt(db.imageDataDao().findFaceRectangle(personImageID, "Face_mFace_height").getDataString());
        faceRectangle.top = Integer.parseInt(db.imageDataDao().findFaceRectangle(personImageID, "Face_mFace_top").getDataString());
        faceRectangle.left = Integer.parseInt(db.imageDataDao().findFaceRectangle(personImageID, "Face_mFace_left").getDataString());
        faceRectangle.width = Integer.parseInt(db.imageDataDao().findFaceRectangle(personImageID, "Face_mFace_width").getDataString());

        bitmap = getBitmapFromPath(path);
        return ImageHelper.generateFaceThumbnail(bitmap, faceRectangle);
    }

    private Bitmap getBitmapFromPath(String path){
        final Bitmap[] bitmap = new Bitmap[1];

        Glide.with(getApplicationContext())
                .asBitmap()
                .load(path)
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                        bitmap[0] = resource;
                    }
                });

        return bitmap[0];
    }

    private void startAlbumActivity(Album album) {
        Intent intent = new Intent(getApplicationContext(), AlbumActivity.class);
        intent.putExtra(AlbumActivity.ALBUM_PATH, album.getPath());

        ActivityOptionsCompat options;

        //noinspection unchecked
        options = ActivityOptionsCompat.makeSceneTransitionAnimation((Activity) this);
        startActivityForResult(intent,
                AlbumCode, options.toBundle());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == AlbumCode) {
            if (resultCode != RESULT_CANCELED) {
                setResult(RESULT_OK, data);
            }
            if(file.delete())
                this.finish();
            else
                Log.d("MyTag", "wrong file deleted in searchActivity");
        }
    }

    public static class ImageHelper {
        // Resize face rectangle, for better view for human
        // To make the rectangle larger, faceRectEnlargeRatio should be larger than 1, recommend 1.3
        private static FaceRectangle calculateFaceRectangle(
                Bitmap bitmap, FaceRectangle faceRectangle, double faceRectEnlargeRatio) {
            // Get the resized side length of the face rectangle
            double sideLength = faceRectangle.width * faceRectEnlargeRatio;
            sideLength = Math.min(sideLength, bitmap.getWidth());
            sideLength = Math.min(sideLength, bitmap.getHeight());

            // Make the left edge to left more.
            double left = faceRectangle.left
                    - faceRectangle.width * (faceRectEnlargeRatio - 1.0) * 0.5;
            left = Math.max(left, 0.0);
            left = Math.min(left, bitmap.getWidth() - sideLength);

            // Make the top edge to top more.
            double top = faceRectangle.top
                    - faceRectangle.height * (faceRectEnlargeRatio - 1.0) * 0.5;
            top = Math.max(top, 0.0);
            top = Math.min(top, bitmap.getHeight() - sideLength);

            // Shift the top edge to top more, for better view for human
            double shiftTop = faceRectEnlargeRatio - 1.0;
            shiftTop = Math.max(shiftTop, 0.0);
            shiftTop = Math.min(shiftTop, 1.0);
            top -= 0.15 * shiftTop * faceRectangle.height;
            top = Math.max(top, 0.0);

            // Set the result.
            FaceRectangle result = new FaceRectangle();
            result.left = (int) left;
            result.top = (int) top;
            result.width = (int) sideLength;
            result.height = (int) sideLength;
            return result;
        }

        // Crop the face thumbnail out from the original image.
        // For better view for human, face rectangles are resized to the rate faceRectEnlargeRatio.
        public static Bitmap generateFaceThumbnail(
                Bitmap originalBitmap,
                FaceRectangle faceRectangle) throws IOException {
            FaceRectangle faceRect =
                    calculateFaceRectangle(originalBitmap, faceRectangle, FACE_RECT_SCALE_RATIO);

            return Bitmap.createBitmap(
                    originalBitmap, faceRect.left, faceRect.top, faceRect.width, faceRect.height);
        }
    }

    private void albumItemDescribe(List<ImageData> imageDataListDescribe, String searchResult){
        for(int i=0; i<imageDataListDescribe.size(); i++){
            //이미 찾았던거면 안넣는다.
            if(!checkId.contains(imageDataListDescribe.get(i).getImage_ID())) {
                if (imageDataListDescribe.get(i).getDataString().contains(searchResult)) {
                    Bitmap bitmap = BitmapFactory.decodeFile(imageDataListDescribe.get(i).getFolderName());
                    String debug = imageDataListDescribe.get(i).getFolderName();
                    //이미지 파일 저장
                    new ImageSaver(this)
                            .setFileName(imageDataListDescribe.get(i).image_ID)
                            .setDirectoryName("temp2")
                            .save(bitmap);

                    //뭐 넣었는지 체크용
                    checkId.add(imageDataListDescribe.get(i).getImage_ID());
                }
            }
        }
    }

    private void albumItemOCR(List<ImageData> imageDataListOCR, String searchResult){
        //OCR 정보에서 검색하기
        for(int i=0; i<imageDataListOCR.size(); i++){
            //이미 찾았던거면 안넣는다.
            if(!checkId.contains(imageDataListOCR.get(i).getImage_ID())) {
                if (imageDataListOCR.get(i).getDataString().contains(searchResult)) {
                    Bitmap bitmap = BitmapFactory.decodeFile(imageDataListOCR.get(i).getFolderName());

                    //이미지 파일 저장
                    new ImageSaver(this)
                            .setFileName(imageDataListOCR.get(i).image_ID)
                            .setDirectoryName("temp2")
                            .save(bitmap);

                    //뭐 넣었는지 체크용
                    checkId.add(imageDataListOCR.get(i).getImage_ID());
                }
            }
        }
    }

    private void albumItemFace(List<ImageData> imageDataListFace){
        //FACE 정보에서 검색하기
        for(int i=0; i<imageDataListFace.size(); i++){
                Bitmap bitmap = BitmapFactory.decodeFile(imageDataListFace.get(i).getFolderName());

                //이미지 파일 저장
                new ImageSaver(this)
                        .setFileName(imageDataListFace.get(i).image_ID)
                        .setDirectoryName("tempFace")
                        .save(bitmap);

        }
    }

    //이미지 새로 만들어서 저장하기
    public class ImageSaver {

        private String directoryName = "images";
        private String fileName = "image.png";
        private Context context;
        private boolean external;

        public ImageSaver(Context context) {
            this.context = context;
        }

        public ImageSaver setFileName(String fileName) {
            this.fileName = fileName;
            return this;
        }

        public ImageSaver setExternal(boolean external) {
            this.external = external;
            return this;
        }

        public ImageSaver setDirectoryName(String directoryName) {
            this.directoryName = directoryName;
            return this;
        }

        public void save(Bitmap bitmapImage) {
            FileOutputStream fileOutputStream = null;
            try {
                fileOutputStream = new FileOutputStream(createFile());
                bitmapImage.compress(Bitmap.CompressFormat.JPEG, 50, fileOutputStream);
                Toast.makeText(SearchActivity.this, "이미지가 생성되었습니다", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(SearchActivity.this, "이미지 생성 Exception: " + e.toString(), Toast.LENGTH_SHORT).show();
            } finally {
                try {
                    if (fileOutputStream != null) {
                        fileOutputStream.close();
                        Toast.makeText(SearchActivity.this, "이미지가 저장되었습니다", Toast.LENGTH_SHORT).show();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Toast.makeText(SearchActivity.this, "이미지 저장 Exception: " + e.toString(), Toast.LENGTH_SHORT).show();
                }
            }
        }

        @NonNull
        private File createFile() {
            File directory;
            if(external){
                directory = getAlbumStorageDir(directoryName);
            }
            else {
                directory = context.getDir(directoryName, Context.MODE_PRIVATE);
            }
            if(!directory.exists() && !directory.mkdirs()){
                Log.e("ImageSaver","Error creating directory " + directory);
            }

            return new File(directory, fileName);
        }

        public File getAlbumStorageDir(String albumName) {
            return new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES), albumName);
        }
    }
}