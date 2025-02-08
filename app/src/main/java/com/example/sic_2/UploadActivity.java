package com.example.sic_2;

import android.annotation.SuppressLint;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.text.DateFormat;
import java.util.Calendar;

public class UploadActivity extends AppCompatActivity {
    private EditText uploadTopic;
    private EditText uploadDesc;
    private EditText uploadLang;

    @SuppressLint("SetTextI18n")
    public void uploadData() {
        String title = uploadTopic.getText().toString();
        String desc = uploadDesc.getText().toString();
        String lang = uploadLang.getText().toString();
        String currentDate = DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime());

        boolean uploadSuccessful = true;

        showSuccessDialog(title, desc + "\nUploaded on: " + currentDate);
    }

    private void showSuccessDialog(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(UploadActivity.this);
        View dialogView = getLayoutInflater().inflate(R.layout.activity_upload, null);
        builder.setView(dialogView);

        TextView cardTitle = dialogView.findViewById(R.id.uploadTopic);
        TextView cardMessage = dialogView.findViewById(R.id.uploadDesc);
        Button closeButton = dialogView.findViewById(R.id.uploadImage);

        cardTitle.setText(title);
        cardMessage.setText(message);

        AlertDialog successDialog = builder.create();
        closeButton.setOnClickListener(v -> successDialog.dismiss());
        successDialog.show();
    }

}
