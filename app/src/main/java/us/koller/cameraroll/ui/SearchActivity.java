package us.koller.cameraroll.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.SearchView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.FaceServiceRestClient;
import com.microsoft.projectoxford.face.contract.FaceRectangle;
import com.microsoft.projectoxford.face.contract.Person;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import us.koller.cameraroll.R;
import us.koller.cameraroll.adapter.SearchAdapter;
import us.koller.cameraroll.data.models.Album;
import us.koller.cameraroll.data.models.AlbumItem;
import us.koller.cameraroll.room.ImageDB;
import us.koller.cameraroll.room.ImageData;

public class SearchActivity extends ThemeableActivity {

    private static final double FACE_RECT_SCALE_RATIO = 1.3;
    private String mPersonGroupId;
    private FaceServiceClient faceServiceClient;
    private SearchAdapter adapter;
    private GridView gv;
    private ArrayList<Bitmap> thumbnails = new ArrayList<>();
    ArrayList<String> checkId = new ArrayList<>();
    private String searchResult;
    private List<ImageData> imageDataListDescribe;
    private List<ImageData> imageDataListOCR;
    private int FaceAlbumCode = 40;
    private int VisionAlbumCode = 50;
    private int SearchResultCode = 60;
    File file;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        toolbarSetting();

        String sub_key_face = getResources().getString(R.string.subscription_key_face);
        String endpoint_face = "https://westus.api.cognitive.microsoft.com/face/v1.0/";
        faceServiceClient = new FaceServiceRestClient(endpoint_face, sub_key_face);
        searchResult = "search";

        //Don't need these functions when using SearchResultActivity.
//        //DB 생성
//        ImageDB db = ImageDB.getDatabase(this);
//
//        //DB에 Thema가 Describe인 애들이랑 OCR인 애들을 부름
//        imageDataListDescribe = db.imageDataDao().findByThema("Describe");
//        imageDataListOCR = db.imageDataDao().findByThema("OCR");

        mPersonGroupId = getIntent().getStringExtra("mPersonGroupID");
        gv = findViewById(R.id.faceGridView);

        new getPersonListTask().execute();
    }

    @Override
    public int getDarkThemeRes() {
        return 0;
    }

    @Override
    public int getLightThemeRes() {
        return 0;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        //search_menu.xml 등록
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.search_menu, menu);

        menu.findItem(R.id.search);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected (MenuItem item) {
        if (item.getItemId() == R.id.search) {

            //menuItem을 이용해서 SearchView 변수 생성
            SearchView sv = (SearchView) item.getActionView();
            sv.setFocusable(true);
            sv.setIconified(false);
            sv.clearFocus();
            sv.requestFocusFromTouch();
            sv.setSubmitButtonEnabled(true);

            //SearchView의 검색 이벤트
            sv.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

                //검색버튼을 눌렀을 경우
                @Override
                public boolean onQueryTextSubmit(String query) {
                    searchResult = query;
                    Intent intent = new Intent(SearchActivity.this.getApplicationContext(), SearchResultActivity.class);
                    intent.putExtra("keyword", searchResult);
                    ActivityOptionsCompat options;
                    options = ActivityOptionsCompat.makeSceneTransitionAnimation(SearchActivity.this);
                    startActivityForResult(intent,
                            SearchResultCode, options.toBundle());
                     return true;
                }

                //텍스트가 바뀔때마다 호출
                @Override
                public boolean onQueryTextChange(String newText) {
                    return true;
                }
            });
        }
        return super.onOptionsItemSelected(item);
    }

    private void toolbarSetting(){
        //toolbar
        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
        toolbar.setTitle(R.string.searchTitle);
        AnimatedVectorDrawable drawable = (AnimatedVectorDrawable)
                ContextCompat.getDrawable(SearchActivity.this, R.drawable.back_to_cancel_avd);
        //mutating avd to reset it
        assert drawable != null;
        drawable.mutate();
        toolbar.setNavigationIcon(drawable);
        Drawable navIcon = toolbar.getNavigationIcon();
        if (navIcon != null) {
            navIcon = DrawableCompat.wrap(navIcon);
            DrawableCompat.setTint(navIcon.mutate(), textColorSecondary);
            toolbar.setNavigationIcon(navIcon);
        }
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
    }

    private class getPersonListTask extends AsyncTask<Void,String, Person[]> {

        @Override
        protected Person[] doInBackground(Void... params) {
            try {
                Person[] result = faceServiceClient.listPersonsInLargePersonGroup(mPersonGroupId);
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
//            for (Person person : personList) { //todo 나중에 하나의 폰에서만 하기
            for (int i = 0; i<2; i++) {
                try {
                    Bitmap thumbnail = getFaceThumbnail(personList[i]);
                    thumbnails.add(thumbnail);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            adapter = new SearchAdapter(SearchActivity.this, thumbnails);
            gv.setAdapter(adapter);
            gv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view,
                                        int position, long id) {
                    String targetId = personList[position].personId.toString();
                    Intent intent = new Intent(SearchActivity.this.getApplicationContext(), SearchResultActivity.class);
                    intent.putExtra("mPersonID", targetId);
                    ActivityOptionsCompat options;
                    options = ActivityOptionsCompat.makeSceneTransitionAnimation(SearchActivity.this);
                    startActivityForResult(intent,
                            SearchResultCode, options.toBundle());

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

        bitmap = getPicture(path);

        return ImageHelper.generateFaceThumbnail(bitmap, faceRectangle);
    }

    private Bitmap getPicture(String currentPhotoPath) {
        Bitmap bitmap = BitmapFactory.decodeFile(currentPhotoPath);

        ExifInterface exif = null;
        try { exif = new ExifInterface(currentPhotoPath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        int exifOrientation;
        int exifDegree;
        if (exif != null) {
            exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            exifDegree = exifOrientationToDegrees(exifOrientation);
        } else {
            exifDegree = 0;
        }

        return rotate(bitmap, exifDegree);
    }

    private int exifOrientationToDegrees(int exifOrientation) {
        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
            return 90;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
            return 180;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
            return 270;
        } return 0;
    }

    private Bitmap rotate(Bitmap src, float degree) {
        // Matrix 객체 생성
        Matrix matrix = new Matrix();
        // 회전 각도 셋팅
        matrix.postRotate(degree); // 이미지와 Matrix 를 셋팅해서 Bitmap 객체 생성
        return Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SearchResultCode) {
            if (resultCode != RESULT_CANCELED) {
                setResult(RESULT_OK, data);
            }
            //todo: implement if needed
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
            file = new File(directory, fileName);

            return file;
        }

        public File getAlbumStorageDir(String albumName) {
            return new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_PICTURES), albumName);
        }
    }


    //Don't need these functions when using SearchResultActivity.
    private void makeAlbumVision(List<ImageData> imageDataListDescribe, List<ImageData> imageDataListOCR) {
        //파일 저장함.
        //albumItemList도 만듬
        ArrayList<AlbumItem> albumItemList = new ArrayList<>();
        albumItemList.addAll(albumItemDescribe(imageDataListDescribe, searchResult));
        albumItemList.addAll(albumItemOCR(imageDataListOCR, searchResult));

        String folder = "/data/user/0/us.koller.cameraroll.debug/app_temp2";

        Album album = new Album().setPath(folder);
        album.setCached(true);
        album.setAlbumItems(albumItemList);
        startAlbumActivity(album, VisionAlbumCode);
    }
    private void startAlbumActivity(Album album, int code) {
        Intent intent = new Intent(getApplicationContext(), AlbumActivity.class);
        intent.putExtra(AlbumActivity.ALBUM_PATH, album.getPath());
        intent.putExtra("albumFromSearch", (Serializable) album);
        intent.putExtra("from","fromSearch");

        String debug = album.getPath();
        ActivityOptionsCompat options;

        //noinspection unchecked
        options = ActivityOptionsCompat.makeSceneTransitionAnimation((Activity) this);
        startActivityForResult(intent,
                code, options.toBundle());
    }
    private ArrayList<AlbumItem> albumItemDescribe(List<ImageData> imageDataListDescribe, String searchResult){
        ArrayList<AlbumItem> albumItemList = new ArrayList<>();

        for(int i=0; i<imageDataListDescribe.size(); i++){
            //이미 찾았던거면 안넣는다.
            if(!checkId.contains(imageDataListDescribe.get(i).getImage_ID())) {
                if (imageDataListDescribe.get(i).getDataString().contains(searchResult)) {
                    Bitmap bitmap = BitmapFactory.decodeFile(imageDataListDescribe.get(i).getFolderName());
                    String imagePath = imageDataListDescribe.get(i).getFolderName();
                    albumItemList.add(AlbumItem.getInstance(imagePath));

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
        return albumItemList;
    }
    private ArrayList<AlbumItem> albumItemOCR(List<ImageData> imageDataListOCR, String searchResult){
        ArrayList<AlbumItem> albumItemList = new ArrayList<>();

        //OCR 정보에서 검색하기
        for(int i=0; i<imageDataListOCR.size(); i++){
            //이미 찾았던거면 안넣는다.
            if(!checkId.contains(imageDataListOCR.get(i).getImage_ID())) {
                if (imageDataListOCR.get(i).getDataString().contains(searchResult)) {
                    Bitmap bitmap = BitmapFactory.decodeFile(imageDataListOCR.get(i).getFolderName());
                    String imagePath = imageDataListOCR.get(i).getFolderName();
                    albumItemList.add(AlbumItem.getInstance(imagePath));

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
        return albumItemList;
    }
    private ArrayList<AlbumItem> albumItemFace(List<ImageData> imageDataListFace){
        ArrayList<AlbumItem> albumItemList = new ArrayList<>();

        //FACE 정보에서 검색하기
        for(int i=0; i<imageDataListFace.size(); i++){
            String facePath = imageDataListFace.get(i).getFolderName();
            Bitmap bitmap = BitmapFactory.decodeFile(facePath);
            albumItemList.add(AlbumItem.getInstance(facePath));
            //이미지 파일 저장
            new ImageSaver(this)
                    .setFileName(imageDataListFace.get(i).image_ID)
                    .setDirectoryName("tempFace")
                    .save(bitmap);
        }
        return albumItemList;
    }

}