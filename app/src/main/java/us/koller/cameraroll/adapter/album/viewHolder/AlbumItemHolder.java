package us.koller.cameraroll.adapter.album.viewHolder;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

public abstract class AlbumItemHolder extends RecyclerView.ViewHolder {
    public AlbumItem albumItem;
    private boolean selected = false;
    private Drawable selectorOverlay;

//    private String sub_key_face = String.valueOf(R.string.subscription_key);
//    private String endpoint_face = String.valueOf(R.string.endpoint);
//    private String sub_key_vision = String.valueOf(R.string.subscription_key);
//    private String endpoint_vision = String.valueOf(R.string.endpoint);

    private final String sub_key_face = "23217359959645caa965c459892d5a47";
    private final String endpoint_face = "https://westus.api.cognitive.microsoft.com/face/v1.0/";
    private final String sub_key_vision = "e77f79b5c0124a459198612807cae2c6";
    private final String endpoint_vision = "https://eastus.api.cognitive.microsoft.com/vision/v1.0";

    private VisionServiceClient visionServiceClient = new VisionServiceRestClient(sub_key_vision, endpoint_vision);
    private FaceServiceClient faceServiceClient = new FaceServiceRestClient(endpoint_face, sub_key_face);


    //todo Endpoint랑 subscription key ignore하기
    AlbumItemHolder(View itemView) {
        super(itemView);
        addIndicatorDrawable(itemView);
    }

    public AlbumItem getAlbumItem() {
        return albumItem;
    }

    public void setAlbumItem(AlbumItem albumItem) {
        if (this.albumItem == albumItem) {
            return;
        }

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
        Glide.with(imageView.getContext())
                .asBitmap()
                .load(albumItem.getPath())
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

                        // /를 기준으로 폴더명을 따로 파싱할거임
                        String [] getFolderName = albumItem.getPath().split("/");

                        // 끝에서 두번째가 폴더명임.
                        String folderName = getFolderName[getFolderName.length -2];

//                        detectAndFrame(resource.copy(resource.getConfig(), false));
//
//                        //computer vision describe
//                        doDescribeComputerVision(albumItem.getName(), folderName, resource.copy(resource.getConfig(), false));
//
//                        //computer vision OCR
//                        doOCR(albumItem.getName(), folderName, resource.copy(resource.getConfig(), false));

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

    private void detectAndFrame(final Bitmap imageBitmap)
    {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        new detectTask().execute(inputStream);
    }

    private class detectTask extends AsyncTask<InputStream,String,Face[]> {

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
            for(Face face : faces)
                Toast.makeText(itemView.getContext(), face.faceId.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    //Computer Vision describe
    public void doDescribeComputerVision(String imageName, String folderName, Bitmap visionImage) {
//        textView.setText("Describing...");
        try {
            new visionDescribeTask(imageName, folderName, visionImage).execute();
        } catch (Exception e)
        {
            Toast.makeText(itemView.getContext(), "Error encountered. Exception is: " + e.toString(), Toast.LENGTH_SHORT).show();
//            textView.setText("Error encountered. Exception is: " + e.toString());
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
            // Display based on error existence
//            textView.setText("");
            if (e != null) {
//                textView.setText("Error: " + e.getMessage());
                this.e = null;
            } else {
//                Gson gson = new Gson();
//                String temp= "";

                //result에서 원하는 타이틀을 꺼낼수 있음. ex) metadata, captions
                //그냥 describeData는 이걸 json 파일이 String으로 저장되어있음.
//                AnalysisResult result = gson.fromJson(describeData, AnalysisResult.class);

//                for (Caption caption: result.description.captions) {
//                    temp += "Caption: " + caption.text + ", confidence: " + caption.confidence + "\n";
//                }


                //DB에 넣을 값을 함수로 지정해줌.
//                setDescribe(describeData);
                insertToDB(this.imageName, this.folderName, "Describe", describeData);
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
//                textView.setText("Error: " + e.getMessage());
                this.e = null;
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

                //DB에 저장할 값을 지정해주는 것
                //굳이 함수로 안해도 되는데 나중에 디버깅하기 쉬울라고 함.
//                setOCR(ocrResult);

                insertToDB(this.imageName, this.folderName, "OCR", ocrResult);
            }
        }
    }

    //computer vision Describe process
    private String processDescribe(Bitmap visionImage) throws VisionServiceException, IOException {
        Gson gson = new Gson();

        // Put the image into an input stream for detection.
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        visionImage.compress(Bitmap.CompressFormat.JPEG, 100, output);
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
        visionImage.compress(Bitmap.CompressFormat.JPEG, 100, output);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(output.toByteArray());

        OCR ocr;
        ocr = this.visionServiceClient.recognizeText(inputStream, LanguageCodes.AutoDetect, true);

        String result = gson.toJson(ocr);
        Log.d("result", result);

        return result;
    }

    //DB 관련 내용
    public void insertToDB(String imageName, String folderName, String thema, String dataString){
        //DB 생성
        ImageDB db = ImageDB.getDatabase(itemView.getContext());

        //메인쓰레드가 아니라 백그라운드에서 작업이 일어나도록 insertasync함수를 쓴다.
        //db에 데이터를 보내는거임
        new InsertAsyncTask(db.imageDataDao()).execute(new ImageData(imageName, folderName, thema, dataString));
        Toast.makeText(itemView.getContext(), "정보가 저장되었습니다.", Toast.LENGTH_SHORT).show();
    }

    //메인스레드에서 데이터베이스에 접근할 수 없으므로 AsyncTask를 사용하도록 한다.
    public static class InsertAsyncTask extends AsyncTask<ImageData, Void, Void> {
        private ImageDataDao imageDataDao;

        public  InsertAsyncTask(ImageDataDao describeDataDao){
            this.imageDataDao = describeDataDao;
        }

        @Override //백그라운드작업(메인스레드 X)
        protected Void doInBackground(ImageData... imageData) {
            imageDataDao.insert(imageData[0]);
            return null;
        }
    }
}