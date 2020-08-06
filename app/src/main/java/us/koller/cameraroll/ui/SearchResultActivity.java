package us.koller.cameraroll.ui;

import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;

import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.contract.FaceRectangle;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import us.koller.cameraroll.R;
import us.koller.cameraroll.adapter.SearchResultAdapter;
import us.koller.cameraroll.room.ImageDB;
import us.koller.cameraroll.room.ImageData;

public class SearchResultActivity extends ThemeableActivity {

    private static final double FACE_RECT_SCALE_RATIO = 1.3;
    private String mPersonId;
    private FaceServiceClient faceServiceClient;
    private SearchResultAdapter adapter;
    private GridView gv;
    private ArrayList<Bitmap> results = new ArrayList<>();
    private String searchResult;
    private List<ImageData> imageDataListDescribe;
    private List<ImageData> imageDataListOCR;
    private List<ImageData> imageDataListFace;
    private int FaceAlbumCode = 40;
    private int VisionAlbumCode = 50;
    private File file;
    private ArrayList<Bitmap> bitmapArrayList = new ArrayList<>();
    private ArrayList<String> pathList = new ArrayList<>();
    private ArrayList<String> checkList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_result);

        String searchResult = getIntent().getStringExtra("keyword");
        toolbarSetting();
        mPersonId = getIntent().getStringExtra("mPersonID");

        //DB 생성
        ImageDB db = ImageDB.getDatabase(this);

        if(searchResult != null){
            //DB에 Thema가 Describe인 애들이랑 OCR인 애들을 부름
            imageDataListDescribe = db.imageDataDao().findByThema("Describe");
            imageDataListOCR = db.imageDataDao().findByThema("OCR");

            pathList.addAll(getPathFromDescribe(imageDataListDescribe, searchResult));
            pathList.addAll(getPathFromOCR(imageDataListOCR, searchResult));

            bitmapArrayList = makeBitmapFromPaths(pathList);
        } else if(mPersonId != null){
            imageDataListFace = db.imageDataDao().findByThema("Face_mPersonID");
            pathList.addAll(getPathFromFace(imageDataListFace, mPersonId));

            bitmapArrayList.addAll(makeBitmapFromPaths(pathList));
        }

        gv = findViewById(R.id.searchresultGridView);

        //todo: implement search from DB

        adapter = new SearchResultAdapter(SearchResultActivity.this, results);
        gv.setAdapter(adapter);
        gv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                //todo: implement to start ItemActivity

            }
        });
    }


    @Override
    public int getDarkThemeRes() {
        return 0;
    }

    @Override
    public int getLightThemeRes() {
        return 0;
    }

    private void toolbarSetting(){
        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
        toolbar.setTitle(R.string.searchTitle);
        AnimatedVectorDrawable drawable = (AnimatedVectorDrawable)
                ContextCompat.getDrawable(SearchResultActivity.this, R.drawable.back_to_cancel_avd);
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

    public ArrayList<String> getPathFromDescribe(List<ImageData> imageDataListDescribe, String searchResult){
        ArrayList<String> pathListDescribe = new ArrayList<>();

        for(int i=0; i<imageDataListDescribe.size(); i++){
            if(imageDataListDescribe.get(i).getDataString().contains(searchResult)){
                if(!checkList.contains(imageDataListDescribe.get(i).getImage_ID())){
                    pathListDescribe.add(imageDataListDescribe.get(i).getFolderName());
                    checkList.add(imageDataListDescribe.get(i).getImage_ID());
                }
            }
        }

        return pathListDescribe;
    }

    public ArrayList<String> getPathFromOCR(List<ImageData> imageDataListOCR, String searchResult){
        ArrayList<String> pathListOCR = new ArrayList<>();

        for(int i=0; i<imageDataListOCR.size(); i++){
            if(imageDataListOCR.get(i).getDataString().contains(searchResult)){
                if(!checkList.contains(imageDataListOCR.get(i).getImage_ID())){
                    pathListOCR.add(imageDataListOCR.get(i).getFolderName());
                    checkList.add(imageDataListOCR.get(i).getImage_ID());
                }
            }
        }

        return pathListOCR;
    }

    public ArrayList<String> getPathFromFace(List<ImageData> imageDataListFace, String mPersonId){
        ArrayList<String> pathListFace = new ArrayList<>();

        for(int i=0; i<imageDataListFace.size(); i++){
            if(imageDataListFace.get(i).getDataString().contains(mPersonId)){
                pathListFace.add(imageDataListFace.get(i).getFolderName());
            }
        }

        return pathListFace;
    }

    public ArrayList<Bitmap> makeBitmapFromPaths(ArrayList<String> pathList){
        ArrayList<Bitmap> bitmaps = new ArrayList<>();

        for(int i=0; i<pathList.size(); i++){
            bitmaps.add(getPicture(pathList.get(i)));
        }

        return bitmaps;
    }


}