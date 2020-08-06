package us.koller.cameraroll.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.SearchView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.FaceServiceRestClient;
import com.microsoft.projectoxford.face.contract.FaceRectangle;
import com.microsoft.projectoxford.face.contract.Person;

import java.io.File;
import java.io.IOException;
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

    private int AlbumCode = 39;
    private static final double FACE_RECT_SCALE_RATIO = 1.3;
    private String mPersonGroupId;
    private FaceServiceClient faceServiceClient;
    private SearchAdapter adapter;
    private GridView gv;
    private File file;
    private String searchResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

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

        String sub_key_face = getResources().getString(R.string.subscription_key_face);
        String endpoint_face = "https://westus.api.cognitive.microsoft.com/face/v1.0/";
        faceServiceClient = new FaceServiceRestClient(endpoint_face, sub_key_face);
        searchResult = "search";
        ArrayList<String> checkId = new ArrayList<>();
        ArrayList<AlbumItem> albumItemList = new ArrayList<>();

        //DB 생성
        ImageDB db = ImageDB.getDatabase(this);

        //DB에 Thema가 Describe인 애들이랑 OCR인 애들을 부름
        List<ImageData> imageDataListDescribe = db.imageDataDao().findByThema("Describe");
        List<ImageData> imageDataListOCR = db.imageDataDao().findByThema("OCR");

        //folderName이 path라고 생각하면됨
        //먼저 Describe에서 찾는다.
        for(int i=0; i<imageDataListDescribe.size(); i++){
            //이미 찾았던거면 안넣는다.
            if(!checkId.contains(imageDataListDescribe.get(i).getImage_ID())) {
                if (imageDataListDescribe.get(i).getDataString().contains(searchResult)) {
                    //folderName(path)를 통해 albumItem instance 만든다.
                    albumItemList.add(AlbumItem.getInstance(imageDataListDescribe.get(i).folderName));

                    //뭐 넣었는지 체크용
                    checkId.add(imageDataListDescribe.get(i).getImage_ID());
                }
            }
        }

        //OCR 정보에서 검색하기
        for(int i=0; i<imageDataListOCR.size(); i++){
            if(!checkId.contains(imageDataListOCR.get(i).getImage_ID())) {
                //이미 찾았던거면 안넣는다.
                if (imageDataListOCR.get(i).getDataString().contains(searchResult)) {
                    //folderName(path)를 통해 albumItem instance 만든다.
                    albumItemList.add(AlbumItem.getInstance(imageDataListOCR.get(i).folderName));

                    //뭐 넣었는지 체크용
                    checkId.add(imageDataListOCR.get(i).getImage_ID());
                }
            }
        }

        mPersonGroupId = getIntent().getStringExtra("mPersonGroupID");
        adapter = new SearchAdapter();
        gv = findViewById(R.id.faceGridView);

//        new getPersonListTask().execute();
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
    public boolean onOptionsItemSelected (MenuItem item){
        if(item.getItemId() == R.id.search) {

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
                    //todo: 검색 시작
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
                    Album album = new Album().setPath("");
                    ArrayList<AlbumItem> albumItems = new ArrayList<>();
                    String targetId = personList[position].personId.toString();

                    //todo : Make Album of selected person


                    album.setCached(true);
                    file = new File(getApplicationContext().getFilesDir(), "cache"+ targetId);
                    album.setPath(file.getPath());
//                    startAlbumActivity(album);


                }
            });
        }
    }

    private Bitmap getFaceThumbnail(Person person) throws IOException {
        String personID = person.personId.toString();
        FaceRectangle faceRectangle = null;
        Bitmap bitmap = null;
        String path = null;

        //todo: get faceRect & image path From DB
        // by personID

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
}