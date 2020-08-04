package us.koller.cameraroll.adapter.album.viewHolder;

import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.SimpleTarget;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.request.transition.Transition;
import com.microsoft.projectoxford.face.FaceServiceClient;
import com.microsoft.projectoxford.face.FaceServiceRestClient;
import com.microsoft.projectoxford.face.contract.CreatePersonResult;
import com.microsoft.projectoxford.face.contract.Face;
import com.microsoft.projectoxford.face.contract.Person;
import com.microsoft.projectoxford.face.contract.VerifyResult;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
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
    private FaceServiceClient faceServiceClient = new FaceServiceRestClient("https://westus.api.cognitive.microsoft.com/face/v1.0/", "");

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
                        detect(mBitmap);
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

    private void detect(final Bitmap imageBitmap)
    {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
        new detectTask().execute(inputStream);
    }

    private class detectTask extends AsyncTask<InputStream,String,Face[]> {

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
                new getPersonListTask(face, mPersonGroupId).execute();
            }
        }
    }

    private class getPersonListTask extends AsyncTask<Void,String,Person[]>{
        private Face mFace;
        private String mPersonGroupId;

        getPersonListTask (Face face, String personGroupId) {
            mFace = face;
            mPersonGroupId = personGroupId;
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
                new VerificationTask(mFace, mPersonGroupId, person.personId).execute();
            }
        }
    }

    private class VerificationTask extends AsyncTask<Void, String, VerifyResult> {
        // The IDs of two face to verify.
        private Face mFace;
        private UUID mPersonId;
        private String mPersonGroupId;

        VerificationTask (Face face, String personGroupId, UUID personId1) {
            mFace = face;
            mPersonGroupId = personGroupId;
            mPersonId = personId1;
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
                    new AddFaceTask(mPersonId, mPersonGroupId, mBitmap, mFace).execute();
                    isMatched.put(mFace, true);
                }
                new AddPersonTask(mBitmap, mFace).execute();
            }
        }
    }

    private class AddPersonTask extends AsyncTask<String, String, UUID> {
        // Indicate the next step is to add face in this person, or finish editing this person.
        Bitmap mBitmap;
        Face mFace;

        AddPersonTask (Bitmap bitmap, Face face) {
            mBitmap = bitmap;
            mFace = face;
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
                new AddFaceTask( result, mPersonGroupId, mBitmap, mFace).execute();
            }
        }
    }

    private class AddFaceTask extends AsyncTask<Void, String, Boolean> {
        UUID mPersonId;
        String mPersonGroupId;
        Bitmap mBitmap;
        Face mFace;

        AddFaceTask(UUID personId, String personGroupId, Bitmap bitmap, Face face) {
            mPersonId = personId;
            mPersonGroupId = personGroupId;
            mBitmap = bitmap;
            mFace = face;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // Get an instance of face service client to detect faces in image.
            try {
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
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

}