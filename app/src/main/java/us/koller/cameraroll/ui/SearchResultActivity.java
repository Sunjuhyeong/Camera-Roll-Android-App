package us.koller.cameraroll.ui;

import android.content.Intent;
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

import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.contract.FaceRectangle;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import us.koller.cameraroll.R;
import us.koller.cameraroll.adapter.SearchResultAdapter;
import us.koller.cameraroll.data.Settings;
import us.koller.cameraroll.data.models.Album;
import us.koller.cameraroll.data.models.AlbumItem;
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
    private Album album;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_result);

        String searchResult = getIntent().getStringExtra("keyword");
        toolbarSetting().setTitle(searchResult);
        mPersonId = getIntent().getStringExtra("mPersonID");
        album = (Album) getIntent().getSerializableExtra("album");

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

        adapter = new SearchResultAdapter(SearchResultActivity.this, bitmapArrayList);
        gv.setAdapter(adapter);
        gv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                Intent intent = new Intent(SearchResultActivity.this, BigImageActivity.class);
                intent.putExtra("path", pathList.get(position));
                startActivity(intent);
//                AlbumItem albumItem = AlbumItem.getInstance(pathList.get(position));
//                Bitmap debug = BitmapFactory.decodeFile(pathList.get(position));
//
//
//                Intent intent = new Intent(SearchResultActivity.this, ItemActivity.class);
//                intent.putExtra(ItemActivity.ALBUM_ITEM, albumItem);
//                intent.putExtra(ItemActivity.ALBUM_PATH, album.getPath());
//                intent.putExtra(ItemActivity.ITEM_POSITION, album.getAlbumItems().indexOf(albumItem));
//
//                if (Settings.getInstance(SearchResultActivity.this).showAnimations()) {
//                    ActivityOptionsCompat options =
//                            ActivityOptionsCompat.makeSceneTransitionAnimation(
//                                    SearchResultActivity.this, findViewById(R.id.resultimageview), //todo 이거 맞음?
//                                    albumItem.getPath());
//                    ActivityCompat.startActivityForResult(SearchResultActivity.this, intent,
//                            ItemActivity.VIEW_IMAGE, options.toBundle());
//                } else {
//                    ActivityCompat.startActivityForResult( SearchResultActivity.this, intent,
//                            ItemActivity.VIEW_IMAGE, null);
//                }
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

    private Toolbar toolbarSetting(){
        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayShowTitleEnabled(false);
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
        return toolbar;
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