package com.example.mymwapplication;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.squareup.picasso.Picasso;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private TextView textView;
    private RecyclerView recyclerView;
    private ImageAdapter adapter;
    private List<String> imageUrlList = new ArrayList<>();
    private static final String TAG = "MainActivity";
    private Button btnToggleImage;
    private boolean isImageHidden = false;  // 이미지 숨김 상태

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.textView);
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ImageAdapter(imageUrlList);
        recyclerView.setAdapter(adapter);

        btnToggleImage = findViewById(R.id.btn_toggle_image);
        btnToggleImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleImageVisibility();
            }
        });

        Button btnLoad = findViewById(R.id.btn_load);
        btnLoad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new ImageFetchTask().execute();
            }
        });

        Button btnUpload = findViewById(R.id.btn_save);
        btnUpload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new ImageUploadTask().execute("sky.png");
            }
        });
    }

    private void toggleImageVisibility() {
        if (isImageHidden) {
            recyclerView.setVisibility(View.VISIBLE);
            isImageHidden = false;
            btnToggleImage.setText("이미지 숨기기");
        } else {
            recyclerView.setVisibility(View.GONE);
            isImageHidden = true;
            btnToggleImage.setText("이미지 보기");
        }
    }

    private class ImageFetchTask extends AsyncTask<Void, Void, List<String>> {
        @Override
        protected List<String> doInBackground(Void... voids) {
            List<String> urls = new ArrayList<>();
            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder()
                    .url("https://mwmid325.pythonanywhere.com/api_root/Post/")
                    .get()
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

                JSONObject jsonResponse = new JSONObject(response.body().string());
                JSONArray imagesArray = jsonResponse.getJSONArray("results");

                for (int i = 0; i < imagesArray.length(); i++) {
                    JSONObject imageObject = imagesArray.getJSONObject(i);
                    if (imageObject.has("image")) {
                        urls.add(imageObject.getString("image"));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return urls;
        }

        @Override
        protected void onPostExecute(List<String> result) {
            if (result.isEmpty()) {
                textView.setText("이미지를 불러올 수 없습니다.");
            } else {
                textView.setText("이미지 로드 성공");
                imageUrlList.clear();
                imageUrlList.addAll(result);
                adapter.notifyDataSetChanged();
            }
        }
    }

    private class ImageUploadTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            String filePath = "/sdcard/Download/" + params[0];
            File file = new File(filePath);

            OkHttpClient client = new OkHttpClient();
            RequestBody fileBody = RequestBody.create(MediaType.parse("image/png"), file);
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("author", "1")
                    .addFormDataPart("title", "Test Image Upload")
                    .addFormDataPart("text", "Image upload from Android")
                    .addFormDataPart("created_date", "2024-11-03T10:00:00Z")
                    .addFormDataPart("published_date", "2024-11-03T10:00:00Z")
                    .addFormDataPart("image", file.getName(), fileBody)
                    .build();

            Request request = new Request.Builder()
                    .url("https://mwmid325.pythonanywhere.com/api_root/Post/")
                    .post(requestBody)
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);
                return response.body().string();
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                Toast.makeText(MainActivity.this, "Upload successful!", Toast.LENGTH_SHORT).show();
                Log.d("UploadResult", result);
            } else {
                Toast.makeText(MainActivity.this, "Upload failed", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
