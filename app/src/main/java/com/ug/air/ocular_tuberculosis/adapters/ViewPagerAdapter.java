package com.ug.air.ocular_tuberculosis.adapters;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.jsibbold.zoomage.ZoomageView;
import com.ug.air.ocular_tuberculosis.R;
import com.ug.air.ocular_tuberculosis.models.Urls;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class ViewPagerAdapter extends RecyclerView.Adapter<ViewPagerAdapter.ViewHolder> {

    public interface OnDeleteClickListener {
        void onDelete(int position);
    }

    List<Urls> imagesList;
    Context context;
    String category;
    int pos;
    OnDeleteClickListener deleteClickListener;
    OnImageLoadListener listener;

    public interface OnImageLoadListener {
        void onImageLoaded(Bitmap bitmap, String filePath);
    }

    public void setOnImageLoadListener(OnImageLoadListener listener){
        this.listener = listener;
    }

    public ViewPagerAdapter(List<Urls> imagesList, Context context, String category, int pos) {
        this.imagesList = imagesList;
        this.context = context;
        this.category = category;
        this.pos = pos;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.gallery, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (category.equals("original")){
            Urls url = imagesList.get(position);
//            Glide.with(context)
//                    .load(getBitmap(url.getOriginal()))
//                    .into(holder.imageView);
            holder.linearLayout.setVisibility(View.GONE);

            if (url.getOriginal().isEmpty()) {
                Glide.with(context)
                        .load(R.color.crime)
                        .into(holder.imageView);
            }
            else {
                Glide.with(context)
                    .load(getBitmap(url.getOriginal()))
                    .into(new CustomTarget<Drawable>() {
                        @Override
                        public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                            Bitmap loadedBitmap = convertDrawableToBitmap(resource);
                            holder.imageView.setImageBitmap(loadedBitmap);  // Set Bitmap into ImageView
                            if (listener != null) {
                                listener.onImageLoaded(loadedBitmap, url.getOriginal());  // Notify the listener with the Bitmap
                            }
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder) {

                        }
                    });
            }


        }
        else if (category.equals("analysed")) {
            Urls url = imagesList.get(position);
            Glide.with(context)
                    .load(getBitmap(url.getAnalysed()))
                    .into(holder.imageView);
            holder.linearLayout.setVisibility(View.VISIBLE);

            holder.afb.setText(String.valueOf(url.getAfb()));
            holder.time.setText(millisecondsToMinutesSeconds(url.getInferenceTime()));
        }
        else {
            int actualPosition = position / 2;

            Urls url = imagesList.get(actualPosition);
            if (position % 2 == 0) {
                Glide.with(context)
                        .load(getBitmap(url.getOriginal()))
                        .into(holder.imageView);
                holder.linearLayout.setVisibility(View.GONE);
            }
            else {
                Glide.with(context)
                        .load(url.getAnalysed())
                        .into(holder.imageView);
                holder.linearLayout.setVisibility(View.VISIBLE);

                holder.afb.setText(String.valueOf(url.getAfb()));
                holder.time.setText(millisecondsToMinutesSeconds(url.getInferenceTime()));
            }

        }

    }

   private String millisecondsToMinutesSeconds(Long milliseconds) {
       double seconds = milliseconds / 1000.0;
       return String.format("%.2f sec", seconds);
   }

    @Override
    public int getItemCount() {
        if (category.equals("both")) {
            return imagesList.size() * 2;
        }
        return imagesList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView afb, time;
        ZoomageView imageView;
        LinearLayout linearLayout;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            afb = itemView.findViewById(R.id.afb);
            time = itemView.findViewById(R.id.time);
            imageView = itemView.findViewById(R.id.image);
            linearLayout = itemView.findViewById(R.id.info);
        }
    }

    public Bitmap getBitmap(String imagePath) {
        File file = new File(imagePath);
        Uri uri = getImageContentUri(context, file);
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 2; // Loads image at half the size

            // Use options when loading bitmap
            Bitmap bitmap;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ImageDecoder.Source source = ImageDecoder.createSource(context.getContentResolver(), uri);
                bitmap = ImageDecoder.decodeBitmap(source, (decoder, info, src) -> {
                    decoder.setMutableRequired(true);
                    // Can set additional decoder options here
                });
            } else {
                // For older Android versions
                bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
            }

//            bitmap = handleImageOrientation(uri, bitmap);
            return bitmap;
        }catch (IOException e) {
//            e.printStackTrace();
            Log.d("Ocular_Malaria", "getBitmap: " + e.getMessage());
            return null;
        }
    }

    public Uri getImageContentUri(Context context, File file) {
        String filePath = file.getAbsolutePath();
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Images.Media._ID},
                MediaStore.Images.Media.DATA + "=?",
                new String[]{filePath}, null
        );

        if (cursor != null){
            if (cursor.moveToFirst()){
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID));
                Uri contentUri = Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, String.valueOf(id));
                cursor.close();
                return contentUri;
            }else {
                cursor.close();

                ContentValues values = new ContentValues();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // For Android 10 and above
                    values.put(MediaStore.Images.Media.DISPLAY_NAME, file.getName());
                    values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                    values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Ocular");

                } else {
                    // For older Android versions
                    values.put(MediaStore.Images.Media.DATA, filePath);
                }
                return context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            }
        }
        return Uri.fromFile(file);
    }

    private Bitmap handleImageOrientation(Uri imageUri, Bitmap bitmap) throws IOException {
//        InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
        InputStream inputStream = null;
        try {
            inputStream = context.getContentResolver().openInputStream(imageUri);

            if (inputStream == null){
                Log.e("Ocular_Malaria", "Failed to open input stream");
                return bitmap;
            }

            ExifInterface exif = new ExifInterface(inputStream);

            int orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
            );

            int rotation = 0;
            if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
                rotation = 90;
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_180) {
                rotation = 180;
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                rotation = 270;
            }

            if (rotation == 0) {
                return bitmap;
            }

            Matrix matrix = new Matrix();
            matrix.postRotate(rotation);
            return Bitmap.createBitmap(
                    bitmap,
                    0,
                    0,
                    bitmap.getWidth(),
                    bitmap.getHeight(),
                    matrix,
                    true
            );

        } catch (IOException e) {
            Log.e("Ocular_Malaria", "Error handling image orientation: " + e.getMessage());
            return bitmap; // Return original bitmap if there's an error
        } finally {
            // Close the input stream in the finally block
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    Log.e("Ocular_Malaria", "Error closing input stream: " + e.getMessage());
                }
            }
        }
    }

    private Bitmap convertDrawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }
        // Handle other types of drawables if necessary
        return null;
    }
}
