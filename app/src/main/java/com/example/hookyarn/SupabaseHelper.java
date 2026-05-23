package com.example.hookyarn;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.webkit.MimeTypeMap;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SupabaseHelper {

    private static final String SUPABASE_URL = "https://ujjkykrqsexwumkllzaq.supabase.co";
    private static final String ANON_KEY = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6InVqamt5a3Jxc2V4d3Vta2xsemFxIiwicm9sZSI6ImFub24iLCJpYXQiOjE3Nzg3NjAzOTksImV4cCI6MjA5NDMzNjM5OX0.BXCsEmfCaeN1VEY9YIdiq66pgsHa8Wwov6gvdLdcBCE";

    private static final ExecutorService executor = Executors.newFixedThreadPool(4);
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    public interface UploadCallback {
        void onSuccess(String publicUrl);
        void onError(Exception e);
    }

    public static void uploadFile(Context context, String bucketName, String fileName, Uri fileUri, UploadCallback callback) {
        executor.execute(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(SUPABASE_URL + "/storage/v1/object/" + bucketName + "/" + fileName);
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(30000);
                conn.setReadTimeout(60000);
                conn.setRequestProperty("Authorization", "Bearer " + ANON_KEY);
                conn.setRequestProperty("apikey", ANON_KEY);
                
                String mimeType = context.getContentResolver().getType(fileUri);
                if (mimeType == null || mimeType.isEmpty() || mimeType.contains("*")) {
                    String extension = MimeTypeMap.getFileExtensionFromUrl(fileName);
                    if (extension == null || extension.isEmpty()) {
                        int lastDot = fileName.lastIndexOf('.');
                        if (lastDot != -1) extension = fileName.substring(lastDot + 1);
                    }
                    if (extension != null) {
                        mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.toLowerCase());
                    }
                }
                
                if (mimeType == null || mimeType.isEmpty()) {
                    if (fileName.toLowerCase().endsWith(".mp4")) mimeType = "video/mp4";
                    else if (fileName.toLowerCase().endsWith(".png")) mimeType = "image/png";
                    else mimeType = "image/jpeg";
                }
                
                Log.d("SupabaseHelper", "Uploading " + fileName + " as " + mimeType);
                conn.setRequestProperty("Content-Type", mimeType);
                conn.setDoOutput(true);

                long fileSize = -1;
                try (android.database.Cursor cursor = context.getContentResolver().query(fileUri, null, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE);
                        if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex);
                    }
                }
                if (fileSize > 0) conn.setFixedLengthStreamingMode(fileSize);

                try (InputStream is = context.getContentResolver().openInputStream(fileUri);
                     OutputStream os = conn.getOutputStream()) {
                    
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = is.read(buffer)) != -1) {
                        os.write(buffer, 0, bytesRead);
                    }
                    os.flush();
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == 200 || responseCode == 201) {
                    String publicUrl = SUPABASE_URL + "/storage/v1/object/public/" + bucketName + "/" + fileName;
                    mainHandler.post(() -> callback.onSuccess(publicUrl));
                } else {
                    StringBuilder errorResponse = new StringBuilder();
                    try (java.io.InputStream es = conn.getErrorStream()) {
                        if (es != null) {
                            java.util.Scanner s = new java.util.Scanner(es).useDelimiter("\\A");
                            errorResponse.append(s.hasNext() ? s.next() : "");
                        }
                    }
                    String errorMessage = errorResponse.toString();
                    Log.e("SupabaseHelper", "Error Response: " + errorMessage);
                    throw new Exception("Supabase Upload Failed (" + responseCode + "): " + errorMessage);
                }

            } catch (Exception e) {
                Log.e("SupabaseHelper", "Upload error", e);
                mainHandler.post(() -> callback.onError(e));
            } finally {
                if (conn != null) conn.disconnect();
            }
        });
    }
}