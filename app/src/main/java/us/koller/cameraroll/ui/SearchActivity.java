package us.koller.cameraroll.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.models.Album;

public class SearchActivity extends AppCompatActivity {

    public Album album;
    private int SearchCode = 26;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        album = new Album().setPath("");

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