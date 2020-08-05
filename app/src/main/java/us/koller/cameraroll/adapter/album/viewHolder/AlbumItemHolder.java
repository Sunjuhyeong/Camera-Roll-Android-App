package us.koller.cameraroll.adapter.album.viewHolder;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.google.gson.Gson;
import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.FaceServiceRestClient;
import com.microsoft.projectoxford.face.contract.CreatePersonResult;
import com.microsoft.projectoxford.face.contract.Face;
import com.microsoft.projectoxford.face.contract.Person;
import com.microsoft.projectoxford.face.contract.VerifyResult;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Locale;
import java.util.UUID;

import edmt.dev.edmtdevcognitivevision.Contract.AnalysisResult;
import edmt.dev.edmtdevcognitivevision.Contract.LanguageCodes;
import edmt.dev.edmtdevcognitivevision.Contract.Line;
import edmt.dev.edmtdevcognitivevision.Contract.OCR;
import edmt.dev.edmtdevcognitivevision.Contract.Region;
import edmt.dev.edmtdevcognitivevision.Contract.Word;
import edmt.dev.edmtdevcognitivevision.Rest.VisionServiceException;
import edmt.dev.edmtdevcognitivevision.VisionServiceClient;
import edmt.dev.edmtdevcognitivevision.VisionServiceRestClient;
import us.koller.cameraroll.R;
import us.koller.cameraroll.data.models.AlbumItem;
import us.koller.cameraroll.room.ImageDB;
import us.koller.cameraroll.room.ImageData;
import us.koller.cameraroll.room.ImageDataDao;
import us.koller.cameraroll.util.Util;
import us.koller.cameraroll.util.animators.ColorFade;

import com.microsoft.projectoxford.face.contract.*;

import org.json.JSONException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import us.koller.cameraroll.R;
import us.koller.cameraroll.data.models.AlbumItem;
import us.koller.cameraroll.util.Util;
import us.koller.cameraroll.util.animators.ColorFade;

public abstract class AlbumItemHolder extends RecyclerView.ViewHolder {

    String mPersonGroupId;
    String path;
    private Bitmap mBitmap;
    HashMap<Face, Boolean> isMatched = new HashMap<Face, Boolean>();
    public AlbumItem albumItem;
    private boolean selected = false;
    private Drawable selectorOverlay;

    private final String sub_key_face = itemView.getContext().getString(R.string.subscription_key_face);
    private final String endpoint_face = "https://westus.api.cognitive.microsoft.com/face/v1.0/";
    private final String sub_key_vision = itemView.getContext().getString(R.string.subscription_key_vision);
    private final String endpoint_vision = "https://eastus.api.cognitive.microsoft.com/vision/v1.0";

    private VisionServiceClient visionServiceClient = new VisionServiceRestClient(sub_key_vision, endpoint_vision);
    private FaceServiceClient faceServiceClient = new FaceServiceRestClient(endpoint_face, sub_key_face);

    AlbumItemHolder(View itemView) {
        super(itemView);
        addIndicatorDrawable(itemView);
    }

    public AlbumItem getAlbumItem() {
        return albumItem;
    }

    public void setAlbumItem(AlbumItem albumItem, String PersonGroupId) {
        if (this.albumItem == albumItem) {
            return;
        }
        mPersonGroupId = PersonGroupId;
        this.albumItem = albumItem;
        ImageView imageView = itemView.findViewById(R.id.image);
        loadImage(imageView, albumItem);
    }

    private void addIndicatorDrawable(View itemView) {
        int indicatorRes = getIndicatorDrawableResource();
        if (indicatorRes != -1) {
            final ImageView imageView = itemView.findViewById(R.id.image);
            final Drawable indicatorOverlay
                    = ContextCompat.getDrawable(itemView.getContext(), indicatorRes);
            imageView.post(new Runnable() {
                @Override
                public void run() {
                    final int overlayPadding = (int) (imageView.getWidth() * 0.05f);
                    final int overlayDimens = (int) (imageView.getWidth() * 0.3f);
                    indicatorOverlay.setBounds(
                            imageView.getWidth() - overlayDimens - overlayPadding,
                            imageView.getHeight() - overlayDimens,
                            imageView.getWidth() - overlayPadding,
                            imageView.getHeight());
                    imageView.getOverlay().add(indicatorOverlay);
                }
            });
        }
    }

    int getIndicatorDrawableResource() {
        return -1;
    }

    public void loadImage(final ImageView imageView, final AlbumItem albumItem) {
        path = albumItem.getPath();
        Glide.with(imageView.getContext())
                .asBitmap()
                .load(path)
                .listener(new RequestListener<Bitmap>() {
                            @Override
                            public boolean onLoadFailed(@Nullable GlideException e, Object model,
                                    Target<Bitmap> target, boolean isFirstResource) {
                                albumItem.error = true;
                                return false;
                            }

                            @Override
                            public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target,
                                    DataSource dataSource, boolean isFirstResource) {
                                if (!albumItem.hasFadedIn) {
                                    fadeIn();
                                } else {
                                    imageView.clearColorFilter();
                                }
                                return false;
                    }
                })
                .apply(albumItem.getGlideRequestOptions(imageView.getContext()))
                .into(new SimpleTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(Bitmap resource, Transition<? super Bitmap> transition) {
                        imageView.setImageBitmap(resource);
                        mBitmap = resource.copy(resource.getConfig(), false);
                        boolean canDescribe = true;
                        boolean canOCR = true;
                        boolean canFace = true;

                        // /를 기준으로 폴더명을 따로 파싱할거임
//                        String [] getFolderName = albumItem.getPath().split("/");
                        // 끝에서 두번째가 폴더명임.
//                        String folderName = getFolderName[getFolderName.length -2];
                        String folderName = albumItem.getPath();
                        //DB 생성
                        ImageDB db = ImageDB.getDatabase(itemView.getContext());

                        List<ImageData> debug = db.imageDataDao().findByImage_ID(albumItem.getName());

                        //image파일을 처음보는 경우
                        if(debug.size() == 0){
                            //computer vision describe
                            doDescribeComputerVision(albumItem.getName(), folderName, resource.copy(resource.getConfig(), false));

                            //computer vision OCR
                            doOCR(albumItem.getName(), folderName, resource.copy(resource.getConfig(), false));

                            //detect face
                            detect(albumItem.getName(), folderName, mBitmap);
                        } else{
                            for(int i=0; i<debug.size(); i++){
                                if(debug.get(i).getThema().equals("Describe")){
                                    canDescribe = false;
                                }

                                if(debug.get(i).getThema().equals("OCR")){
                                    canOCR = false;
                                }

                                if(debug.get(i).getThema().equals("Face_mPersonID")){
                                    canFace = false;
                                }
                            }

                            if(canDescribe){
                                //computer vision describe
                                doDescribeComputerVision(albumItem.getName(), folderName, mBitmap);
                            }

                            if(canOCR){
                                //computer vision OCR
                                doOCR(albumItem.getName(), folderName, mBitmap);
                            }

                            if(canFace){
                                //detect face
                                detect(albumItem.getName(), folderName, mBitmap);
                            }
                        }
                    }
                });
            }

    void fadeIn() {
        albumItem.hasFadedIn = true;
        ColorFade.fadeSaturation((ImageView) itemView.findViewById(R.id.image));
    }

    public void setSelected(boolean selected) {
        boolean animate = this.selected != selected;
        this.selected = selected;
        if (animate) {
            animateSelected();
        }
    }

    private void animateSelected() {
        final View imageView = itemView.findViewById(R.id.image);

        float scale = selected ? 0.8f : 1.0f;
        imageView.animate()
                .scaleX(scale)
                .scaleY(scale)
                .start();

        if (selectorOverlay == null) {
            selectorOverlay = Util.getAlbumItemSelectorOverlay(imageView.getContext());
        }
        if (selected) {
            imageView.post(new Runnable() {
                @Override
                public void run() {
                    imageView.getOverlay().remove(selectorOverlay);
                    selectorOverlay.setBounds(0, 0,
                            imageView.getWidth(),
                            imageView.getHeight());
                    imageView.getOverlay().add(selectorOverlay);
                }
            });
        } else {
            imageView.post(new Runnable() {
                @Override
                public void run() {
                    imageView.getOverlay().remove(selectorOverlay);
                }
            });
        }
    }

    private void detect(String imageName, String folderName, final Bitmap imageBitmap)
    {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        new detectTask(imageName, folderName).execute(inputStream);
    }

    private class detectTask extends AsyncTask<InputStream,String,Face[]> {

        private String imageName;
        private String folderName;

        public detectTask(String imageName, String folderName){
            this.imageName = imageName;
            this.folderName = folderName;
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
                                result.length));
                return result;
            } catch (Exception e) {
                publishProgress("Detection failed");
                return null;
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.N)
        @Override
        protected void onPostExecute(Face[] faces) {
            if (faces == null) return;
            for(Face face : faces) {
                isMatched.put(face, false);
                new getPersonListTask(this.imageName, this.folderName ,face, mPersonGroupId).execute();
            }
        }
    }

    private class getPersonListTask extends AsyncTask<Void,String,Person[]>{
        private Face mFace;
        private String mPersonGroupId;
        private String imageName;
        private String folderName;

        getPersonListTask (String imageName, String folderName, Face face, String personGroupId) {
            mFace = face;
            mPersonGroupId = personGroupId;
            this.imageName = imageName;
            this.folderName = folderName;
        }

        @Override
        protected Person[] doInBackground(Void... params) {

            try {
                Person[] result = faceServiceClient.listPersonsInLargePersonGroup(mPersonGroupId);
                if (result == null)
                {
                    //todo create person when no person
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
                new VerificationTask(this.imageName, this.folderName, mFace, mPersonGroupId, person.personId).execute();
            }
        }
    }

    private class VerificationTask extends AsyncTask<Void, String, VerifyResult> {
        // The IDs of two face to verify.
        private Face mFace;
        private UUID mPersonId;
        private String mPersonGroupId;
        private String imageName;
        private String folderName;

        VerificationTask (String imageName, String folderName, Face face, String personGroupId, UUID personId1) {
            mFace = face;
            mPersonGroupId = personGroupId;
            mPersonId = personId1;
            this.imageName = imageName;
            this.folderName = folderName;
        }

        @Override
        protected VerifyResult doInBackground(Void... params) {
            // Get an instance of face service client to detect faces in image.
            try{
                publishProgress("Verifying...");

                // Start verification.
                return faceServiceClient.verifyInLargePersonGroup(
                        mFace.faceId,      /* The face ID to verify */
                        mPersonGroupId, /* The person group ID of the person*/
                        mPersonId);     /* The person ID to verify */
            }  catch (Exception e) {
                publishProgress(e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPostExecute(VerifyResult result) {
            if (result != null) {
                if (result.isIdentical){
                    new AddFaceTask(this.imageName, this.folderName, mPersonId, mPersonGroupId, mBitmap, mFace).execute();
                    isMatched.put(mFace, true);
                }
                new AddPersonTask(this.imageName, this.folderName, mBitmap, mFace).execute(mPersonGroupId);
            }
        }
    }

    private class AddPersonTask extends AsyncTask<String, String, UUID> {
        // Indicate the next step is to add face in this person, or finish editing this person.
        Bitmap mBitmap;
        Face mFace;
        private String imageName;
        private String folderName;

        AddPersonTask (String imageName, String folderName, Bitmap bitmap, Face face) {
            mBitmap = bitmap;
            mFace = face;
            this.imageName = imageName;
            this.folderName = folderName;
        }

        @Override
        protected UUID doInBackground(String... params) {
            try{
                if(!isMatched.get(mFace)){
                    // Start the request to creating person.
                    CreatePersonResult createPersonResult = faceServiceClient.createPersonInLargePersonGroup(
                            params[0],
                            "unknown" + mFace.faceId,
                            "user data");

                    return createPersonResult.personId;
                } return null;
            } catch (Exception e) {
                publishProgress(e.getMessage());
                return null;
            }
        }

        @Override
        protected void onPostExecute(UUID result) {
            if (result != null) {
                new AddFaceTask(this.imageName,this.folderName, result, mPersonGroupId, mBitmap, mFace).execute();
            }
        }
    }

    private class AddFaceTask extends AsyncTask<Void, String, Boolean> {
        UUID mPersonId;
        String mPersonGroupId;
        Bitmap mBitmap;
        Face mFace;
        private String imageName;
        private String folderName;

        AddFaceTask(String imageName, String folderName, UUID personId, String personGroupId, Bitmap bitmap, Face face) {
            mPersonId = personId;
            mPersonGroupId = personGroupId;
            mBitmap = bitmap;
            mFace = face;
            this.imageName = imageName;
            this.folderName = folderName;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // Get an instance of face service client to detect faces in image.
            try {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                mBitmap.compress(Bitmap.CompressFormat.JPEG, 50, stream);
                InputStream imageInputStream = new ByteArrayInputStream(stream.toByteArray());

                faceServiceClient.addPersonFaceInLargePersonGroup(
                        mPersonGroupId,
                        mPersonId,
                        imageInputStream,
                        "user face",
                        mFace.faceRectangle);
                return true;
            } catch (Exception e) {
                publishProgress(e.getMessage());
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean aBoolean) {
            if (aBoolean){
                //todo: DB에 mPersonId 저장
                try {
                    insertToDB(this.imageName, this.folderName, "Face_mPersonID", mPersonId.toString());
                    insertToDB(this.imageName, this.folderName, "Face_mFace_height", Integer.toString(mFace.faceRectangle.height));
                    insertToDB(this.imageName, this.folderName, "Face_mFace_width", Integer.toString(mFace.faceRectangle.width));
                    insertToDB(this.imageName, this.folderName, "Face_mFace_left", Integer.toString(mFace.faceRectangle.left));
                    insertToDB(this.imageName, this.folderName, "Face_mFace_top", Integer.toString(mFace.faceRectangle.top));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class deletePersoninLargePersonGroupTask extends AsyncTask<Void, String, Void> {
        private UUID mPersonId;
        private String mPersonGroupId;

        deletePersoninLargePersonGroupTask (UUID personId, String personGroupId) {
            mPersonId = personId;
            mPersonGroupId = personGroupId;
        }

        @Override
        protected Void doInBackground(Void... params) {

            try {
                faceServiceClient.deletePersonInLargePersonGroup(mPersonGroupId, mPersonId);

            } catch (Exception e) {
                publishProgress("get PersonList failed");
            }
            return null;
        }
    }

    //Computer Vision describe
    public void doDescribeComputerVision(String imageName, String folderName, Bitmap visionImage) {

        try {
            new visionDescribeTask(imageName, folderName, visionImage).execute();
        } catch (Exception e)
        {
            Toast.makeText(itemView.getContext(), "Error encountered. Exception is: " + e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    public void doOCR(String imageName, String folderName, Bitmap visionImage) {
        try {
            new visionOCRTask(imageName, folderName, visionImage).execute();
        } catch (Exception e)
        {
            Toast.makeText(itemView.getContext(), "Error encountered. Exception is: " + e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    //computer vision - Describe
    private class visionDescribeTask extends AsyncTask<String, String, String> {
        // Store error message
        private Exception e = null;
        private Bitmap visionImage;
        private String imageName;
        private String folderName;

        public visionDescribeTask(String imageName, String folderName, Bitmap visionImage) {
            this.visionImage = visionImage;
            this.imageName = imageName;
            this.folderName = folderName;
        }

        @Override
        protected String doInBackground(String... args) {
            try {
                return processDescribe(this.visionImage);
            } catch (Exception e) {
                this.e = e;    // Store error
            }
            return null;
        }

        @Override
        protected void onPostExecute(String describeData) {
            super.onPostExecute(describeData);

            if (e != null) {
                this.e = null;
                Toast.makeText(itemView.getContext(), "onPostExecute Error: Describe", Toast.LENGTH_SHORT).show();
            } else {
                //DB에 데이터 저장
                try {
                    insertToDB(this.imageName, this.folderName, "Describe", describeData);
                } catch (JSONException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    //computer vision - OCR
    private class visionOCRTask extends AsyncTask<String, String, String> {
        // Store error message
        private Exception e = null;
        private Bitmap visionImage;
        private String imageName;
        private String folderName;

        public visionOCRTask(String imageName, String folderName, Bitmap visionImage) {
            this.visionImage = visionImage;
            this.imageName = imageName;
            this.folderName = folderName;
        }

        @Override
        protected String doInBackground(String... args) {
            try {
                return processOCR(this.visionImage);
            } catch (Exception e) {
                this.e = e;    // Store error
            }

            return null;
        }

        @Override
        protected void onPostExecute(String ocrData) {
            super.onPostExecute(ocrData);

            if (e != null) {
                this.e = null;
                Toast.makeText(itemView.getContext(), "onPostExecute Error: OCR", Toast.LENGTH_SHORT).show();
            } else {
                Gson gson = new Gson();

                OCR r = gson.fromJson(ocrData, OCR.class);

                String ocrResult = "";
                for (Region reg : r.regions) {
                    for (Line line : reg.lines) {
                        for (Word word : line.words) {
                            ocrResult += word.text + " ";
                        }
                        ocrResult += "\n";
                    }
                    ocrResult += "\n\n";
                }

                try {
                    insertToDB(this.imageName, this.folderName, "OCR", ocrResult);
                } catch (JSONException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    //computer vision Describe process
    private String processDescribe(Bitmap visionImage) throws VisionServiceException, IOException {
        Gson gson = new Gson();

        // Put the image into an input stream for detection.
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        visionImage.compress(Bitmap.CompressFormat.JPEG, 50, output);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());

        AnalysisResult v = this.visionServiceClient.describe(inputStream, 1);

        String result = gson.toJson(v);
        Log.d("result", result);

        return result;
    }

    //computer vision OCR process
    private String processOCR(Bitmap visionImage) throws VisionServiceException, IOException {
        Gson gson = new Gson();

        // Put the image into an input stream for detection.
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        visionImage.compress(Bitmap.CompressFormat.JPEG, 50, output);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());

        OCR ocr;
        ocr = this.visionServiceClient.recognizeText(inputStream, LanguageCodes.AutoDetect, true);

        String result = gson.toJson(ocr);
        Log.d("result", result);

        return result;
    }

    //DB 관련 내용
    public void insertToDB(String imageName, String folderName, String thema, String dataString) throws JSONException {
        //DB 생성
        ImageDB db = ImageDB.getDatabase(itemView.getContext());

        //메인쓰레드가 아니라 백그라운드에서 작업이 일어나도록 insertasync함수를 쓴다.
        //db에 데이터를 보내는거임
        ImageData imageData = new ImageData(imageName, folderName, thema, dataString);

        new InsertAsyncTask(db.imageDataDao()).execute(imageData);
        Toast.makeText(itemView.getContext(), "정보가 저장되었습니다.", Toast.LENGTH_SHORT).show();
    }

    //메인스레드에서 데이터베이스에 접근할 수 없으므로 AsyncTask를 사용하도록 한다.
    public static class InsertAsyncTask extends AsyncTask<ImageData, Void, Void> {
        private ImageDataDao imageDataDao;

        public InsertAsyncTask(ImageDataDao describeDataDao) {
            this.imageDataDao = describeDataDao;
        }

        @Override //백그라운드작업(메인스레드 X)
        protected Void doInBackground(ImageData... imageData) {
            imageDataDao.insert(imageData[0]);
            return null;
        }
    }

}


