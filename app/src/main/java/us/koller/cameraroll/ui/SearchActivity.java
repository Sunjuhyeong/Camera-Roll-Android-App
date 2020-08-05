package us.koller.cameraroll.ui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.models.Album;

public class SearchActivity extends AppCompatActivity {


    public Album album;
    private int SearchCode = 26;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        //todo: Make Album by search Results

        startAlbumActivity();
    }

    private void startAlbumActivity() {
        Intent intent = new Intent(getApplicationContext(), AlbumActivity.class);
        intent.putExtra(AlbumActivity.ALBUM_PATH, album.getPath());

        ActivityOptionsCompat options;

        //noinspection unchecked
        options = ActivityOptionsCompat.makeSceneTransitionAnimation((Activity) getApplicationContext());
        startActivityForResult(intent,
                SearchCode, options.toBundle());
    }

}