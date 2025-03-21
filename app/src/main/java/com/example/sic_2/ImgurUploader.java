package com.example.sic_2;

import android.net.Uri;
import android.util.Base64;

import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ImgurUploader {

    private static final String CLIENT_ID = "YOUR_CLIENT_ID"; // Replace with your Client ID
    private static final String UPLOAD_URL = "https://api.imgur.com/3/image";

    public void uploadImage(Uri imageUri, final UploadCallback callback) {
        try {
            InputStream inputStream = MyApplication.getAppContext().getContentResolver().openInputStream(imageUri); //MyApplication.getAppContext() gets the application context.
            byte[] imageBytes = getBytes(inputStream);
            String base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);

            OkHttpClient client = new OkHttpClient();

            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("image", base64Image)
                    .build();

            Request request = new Request.Builder()
                    .url(UPLOAD_URL)
                    .header("Authorization", "Client-ID " + CLIENT_ID)
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    callback.onFailure(e.getMessage());
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseBody = response.body().string();
                    if (response.isSuccessful()) {
                        Gson gson = new Gson();
                        ImgurResponse imgurResponse = gson.fromJson(responseBody, ImgurResponse.class);
                        if (imgurResponse.success) {
                            callback.onSuccess(imgurResponse.data.link);
                        } else {
                            callback.onFailure("Imgur upload failed: " + imgurResponse.data.error);
                        }
                    } else {
                        callback.onFailure("Imgur upload failed: " + response.code());
                    }
                }
            });

        } catch (IOException e) {
            callback.onFailure(e.getMessage());
        }
    }

    private byte[] getBytes(InputStream inputStream) throws IOException {
        ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
        int bufferSize = 1024;
        byte[] buffer = new byte[bufferSize];

        int len;
        while ((len = inputStream.read(buffer)) != -1) {
            byteBuffer.write(buffer, 0, len);
        }
        return byteBuffer.toByteArray();
    }

    public interface UploadCallback {
        void onSuccess(String imageUrl);
        void onFailure(String errorMessage);
    }

    private static class ImgurResponse {
        public Data data;
        public boolean success;
        public int status;
    }

    private static class Data {
        public String link;
        public String error;
    }

}