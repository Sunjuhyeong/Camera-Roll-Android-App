package us.koller.cameraroll.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.GridView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityOptionsCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.FaceServiceRestClient;
import com.microsoft.projectoxford.face.contract.FaceRectangle;
import com.microsoft.projectoxford.face.contract.Person;

import java.io.IOException;

import us.koller.cameraroll.R;
import us.koller.cameraroll.adapter.SearchAdapter;
import us.koller.cameraroll.data.models.Album;

public class SearchActivity extends AppCompatActivity {

    private int SearchCode = 26;
    private static final double FACE_RECT_SCALE_RATIO = 1.3;
    private final String sub_key_face = getApplicationContext().getString(R.string.subscription_key_face);
    private final String endpoint_face = "https://westus.api.cognitive.microsoft.com/face/v1.0/";
    private String mPersonGroupId;
    private FaceServiceClient faceServiceClient = new FaceServiceRestClient(endpoint_face, sub_key_face);
    private SearchAdapter adapter;
    private GridView gv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        mPersonGroupId = getIntent().getStringExtra("mPersonGroupID"); //todo : 제대로 오나?
        adapter = new SearchAdapter();
        gv = (GridView)findViewById(R.id.faceGridView);

        new getPersonListTask().execute();
    }

    private void startAlbumActivity(Album album) {
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


    private class getPersonListTask extends AsyncTask<Void,String, Person[]> {

        @Override
        protected Person[] doInBackground(Void... params) {

            try {
                Person[] result = faceServiceClient.listPersonsInLargePersonGroup(mPersonGroupId);
                if (result == null)
                {
                    //todo: create person when no person
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
                    Bitmap thumbnail = getFaceThumbNail(person);
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

                    //todo : Make Album of selected face

                    startAlbumActivity(album);
                }
            });
        }
    }

    private Bitmap getFaceThumbNail(Person person) throws IOException {
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