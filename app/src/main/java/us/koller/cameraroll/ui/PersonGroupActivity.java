package us.koller.cameraroll.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.widget.GridView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.transition.Transition;
import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.FaceServiceRestClient;
import com.microsoft.projectoxford.face.contract.Face;
import com.microsoft.projectoxford.face.contract.LargePersonGroup;
import com.microsoft.projectoxford.face.contract.VerifyResult;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Locale;
import java.util.UUID;

import us.koller.cameraroll.R;


public class PersonGroupActivity extends AppCompatActivity {

    private FaceServiceClient faceServiceClient;
    private UUID mFaceId;
    private UUID mPersonId;
    String mPersonGroupId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_person_group);

        faceServiceClient = new FaceServiceRestClient("https://westus.api.cognitive.microsoft.com/face/v1.0/", "");

        new InitPersonGroupTask().execute();

//
//        String path = getIntent().getStringExtra("path");
//        getImage(path);
    }

    private void getImage(String path){
        Glide.with(getApplicationContext())
                .asBitmap()
                .load(path)
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, Transition<? super Bitmap> transition) {
                        detect(resource.copy(resource.getConfig(), false));
                    }
                });
    }

    private void detect(final Bitmap imageBitmap)
    {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        new detectTask().execute(inputStream);
    }

    private void verify(UUID faceId){
        mFaceId = faceId;
        new VerificationTask(mFaceId, mPersonGroupId, mPersonId).execute();
    }

    private class detectTask extends AsyncTask<InputStream,String, Face[]> {

        @Override
        protected void onPreExecute() {

        }


        @Override
        protected void onProgressUpdate(String... values) {

        }

        @Override
        protected Face[] doInBackground(InputStream... params) {

            try {
                publishProgress("Detecting...");
                Face[] result = faceServiceClient.detect(
                        params[0],
                        true,         // returnFaceId
                        false,        // returnFaceLandmarks
                        null           // returnFaceAttributes: a string like "age, gender"
                );
                if (result == null)
                {
                    publishProgress("Detection Finished. Nothing detected");
                    return null;
                }
                publishProgress(
                        String.format(Locale.ENGLISH, "Detection Finished. %d face(s) detected",
                                result.length)); //todo Locale이 뭔가요
                return result;
            } catch (Exception e) {
                publishProgress("Detection failed");
                return null;
            }
        }

        @Override
        protected void onPostExecute(Face[] faces) {
            if (faces == null) return;
            for(Face face : faces) {
                Toast.makeText(getApplicationContext(), face.faceId.toString(), Toast.LENGTH_SHORT).show();
//                verify(face.faceId);

            }
        }
    }

    private class VerificationTask extends AsyncTask<Void, String, VerifyResult> {
        // The IDs of two face to verify.
        private UUID mFaceId;
        private UUID mPersonId;
        private String mPersonGroupId;

        VerificationTask (UUID faceId, String personGroupId, UUID personId1) {
            mFaceId = faceId;
            mPersonGroupId = personGroupId;
            mPersonId = personId1;
        }

        @Override
        protected VerifyResult doInBackground(Void... params) {

            try{
                publishProgress("Verifying...");

                // Start verification.
                return faceServiceClient.verifyInLargePersonGroup(
                        mFaceId,      /* The face ID to verify */
                        mPersonGroupId, /* The person group ID of the person*/
                        mPersonId);     /* The person ID to verify */
            }  catch (Exception e) {
                publishProgress(e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPreExecute() {

        }

        @Override
        protected void onProgressUpdate(String... progress) {

        }

        @Override
        protected void onPostExecute(VerifyResult result) {
            if (result != null) {
                Toast.makeText(getApplicationContext(),"Response: Success. Face " + PersonGroupActivity.this.mFaceId + " "
                        + mPersonId + (result.isIdentical ? " " : " don't ")
                        + "belong to person "+ PersonGroupActivity.this.mPersonId, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class InitPersonGroupTask extends AsyncTask<String, String, LargePersonGroup[]> {

        @Override
        protected LargePersonGroup[] doInBackground(String... params) {

            try{
                // get a list of LargePersonGroups

                return faceServiceClient.listLargePersonGroups();
            } catch (Exception e) {
                publishProgress(e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPostExecute(LargePersonGroup[] result) {
            if (result != null) {
                mPersonGroupId = result[0].largePersonGroupId;
            }
            else {//todo 이게 실제로 작동하지는 않는듯
                try {
                    faceServiceClient.createLargePersonGroup(
                        "madcamp3", //group Id
                        "People", //group Name
                        null); //group description
                }
                catch (Exception e) {
                    publishProgress(e.getMessage());
                }
            }
            Intent intent = new Intent();
            intent.putExtra("result", mPersonGroupId);
            setResult(RESULT_OK, intent);
            finish();
        }
    }

    private void createPersonGroup(){};

    private void addPerson(){};

}