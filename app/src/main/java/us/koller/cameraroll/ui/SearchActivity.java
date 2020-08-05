package us.koller.cameraroll.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.util.Pools;

import java.util.ArrayList;
import java.util.List;
import us.koller.cameraroll.R;
import us.koller.cameraroll.data.models.Album;
import us.koller.cameraroll.data.models.AlbumItem;
import us.koller.cameraroll.room.ImageDB;
import us.koller.cameraroll.room.ImageData;

public class SearchActivity extends AppCompatActivity {

    public Album album;
    private int SearchCode = 26;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        String searchResult = "search";
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


        //todo: Make Album by search Results


        //todo: or selected face


        startAlbumActivity();
    }

    private void startAlbumActivity() {
        Intent intent = new Intent(getApplicationContext(), AlbumActivity.class);
        intent.putExtra(AlbumActivity.ALBUM_PATH, album.getPath());

        ActivityOptionsCompat options;

        //noinspection unchecked
        options = ActivityOptionsCompat.makeSceneTransitionAnimation((Activity) this);
        startActivityForResult(intent,
                SearchCode, options.toBundle());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SearchCode) {
            if (resultCode != RESULT_CANCELED) {
                setResult(RESULT_OK, data);
            }
            this.finish();
        }
    }
}