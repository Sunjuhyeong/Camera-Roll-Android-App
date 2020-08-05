package us.koller.cameraroll.ui;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.FaceServiceRestClient;
import com.microsoft.projectoxford.face.contract.LargePersonGroup;

import us.koller.cameraroll.R;


public class PersonGroupActivity extends AppCompatActivity {

    private FaceServiceClient faceServiceClient;
    String mPersonGroupId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_person_group);
        String face_sub_key = getApplicationContext().getString(R.string.subscription_key_face);
        faceServiceClient = new FaceServiceRestClient("https://westus.api.cognitive.microsoft.com/face/v1.0/", face_sub_key);

        new InitPersonGroupTask().execute();
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

}