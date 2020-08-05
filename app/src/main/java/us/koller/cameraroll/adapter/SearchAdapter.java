package us.koller.cameraroll.adapter;

import android.graphics.Bitmap;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

import com.microsoft.projectoxford.face.contract.FaceRectangle;

import java.io.IOException;
import java.util.ArrayList;

import us.koller.cameraroll.R;

public class SearchAdapter extends BaseAdapter {

    // Ratio to scale a detected face rectangle, the face rectangle scaled up looks more natural.
    private static final double FACE_RECT_SCALE_RATIO = 1.3;

    ArrayList<Bitmap> items = new ArrayList<>();

    public void addItem(Bitmap bitmap){
        items.add(bitmap);
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int i) {
        return items.get(i);
    }

    @Override
    public long getItemId(int i) {
        return 0;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ImageView iv = viewGroup.findViewById(R.id.faceThumbnail); //viewGroup 맞나?
        iv.setImageBitmap(items.get(i));

        return iv;
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