package com.sdi.fileuploader;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.protobuf.ByteString;
import com.proto.uploader.FileRequest;
import com.proto.uploader.FileResponse;
import com.proto.uploader.FileUploaderGrpc;
import com.sdi.fileuploader.databinding.ActivityMainBinding;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import io.grpc.ManagedChannelBuilder;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;
    private ActivityResultLauncher<Intent> filePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null && data.getData() != null) {
                            Uri fileUri = data.getData();
                            uploadFile(fileUri);
                        }
                    }
                }
        );

        Button selectButton = binding.selectFileButton;
        selectButton.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            filePickerLauncher.launch(intent);
        });

    }

    private void uploadFile(Uri fileUri) {
        try {
            String fileName = getFileName(fileUri);

            InputStream inputStream = getContentResolver().openInputStream(fileUri);
            if (inputStream == null) {
                Toast.makeText(this, "No se pudo leer el archivo.", Toast.LENGTH_SHORT).show();
                return;
            }
            byte[] fileData = readBytesFromStream(inputStream);


            FileUploaderGrpc.FileUploaderBlockingStub stub = FileUploaderGrpc.newBlockingStub(ManagedChannelBuilder
                    .forAddress("192.168.1.101", 50051)
                    .usePlaintext()
                    .build());

            FileRequest request = FileRequest.newBuilder()
                    .setFileName(fileName)
                    .setFileData(ByteString.copyFrom(fileData))
                    .build();

            FileResponse response = stub.uploadFile(request);

            if (response.getSuccess()) {
                Toast.makeText(this, "Archivo subido con éxito: " + response.getMessage(), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Error al subir el archivo: " + response.getMessage(), Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Ocurrió un error al subir el archivo.", Toast.LENGTH_SHORT).show();
        }

    }

    private String getFileName(Uri fileUri) {
        String fileName = null;
        Cursor cursor = getContentResolver().query(fileUri, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            if (nameIndex != -1) {
                fileName = cursor.getString(nameIndex);
            }
            cursor.close();
        }
        return fileName != null ? fileName : "unknown_file";
    }

    private byte[] readBytesFromStream(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] temp = new byte[1024];
        int read;
        while ((read = inputStream.read(temp)) != -1) {
            buffer.write(temp, 0, read);
        }
        return buffer.toByteArray();
    }

}

