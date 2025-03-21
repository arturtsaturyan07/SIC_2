package com.example.sic_2;

import android.net.Uri;
import okhttp3.*;
import org.json.JSONObject;
import java.io.IOException;

public class ImgurUploader {

    private static final String IMGUR_CLIENT_ID = "0a49ee5288a0303";
    private static final String IMGUR_UPLOAD_URL = "https://api.imgur.com/3/image";

    public interface UploadCallback {
        void onSuccess(String imageUrl);
        void onFailure(String errorMessage);
    }

    public void uploadImage(Uri imageUri, UploadCallback callback) {
        OkHttpClient client = new OkHttpClient();

        try {
            // Convert Uri to byte array (you need to implement this method)
            byte[] imageData = getImageDataFromUri(imageUri);

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("image", "image.jpg", RequestBody.create(imageData, MediaType.parse("image/*")))
                    .build();

            Request request = new Request.Builder()
                    .url(IMGUR_UPLOAD_URL)
                    .header("Authorization", "Client-ID " + IMGUR_CLIENT_ID)
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onFailure(e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        try {
                            JSONObject json = new JSONObject(response.body().string());
                            String imageUrl = json.getJSONObject("data").getString("link");
                            callback.onSuccess(imageUrl);
                        } catch (Exception e) {
                            callback.onFailure("Failed to parse Imgur response");
                        }
                    } else {
                        callback.onFailure("Upload failed: " + response.message());
                    }
                }
            });
        } catch (Exception e) {
            callback.onFailure("Failed to read image data");
        }
    }

    private byte[] getImageDataFromUri(Uri imageUri) throws IOException {
        // Implement this method to convert Uri to byte array
        // Example: Use a ContentResolver to read the image data
        return new byte[0]; // Replace with actual implementation
    }
}