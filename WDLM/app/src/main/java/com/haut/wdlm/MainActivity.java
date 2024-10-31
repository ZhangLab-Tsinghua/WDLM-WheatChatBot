package com.haut.wdlm;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_IMAGE_CAPTURE = 1;

    private RecyclerView recyclerView;
    private MessageAdapter messageAdapter;
    private List<Message> messageList;
    private EditText editTextMessage;
    private ImageButton buttonSend;
    private LinearLayout layoutRoot;
    private ImageButton delete;
    private LinearLayout start;
    private ImageButton buttonPhoto;
    private Bitmap selectedImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setStatusBar();
        recyclerView = findViewById(R.id.recyclerView);
        editTextMessage = findViewById(R.id.editTextMessage);
        buttonSend = findViewById(R.id.send);
        layoutRoot = findViewById(R.id.layoutRoot);
        start = findViewById(R.id.start);
        delete = findViewById(R.id.delete);
        buttonPhoto = findViewById(R.id.photo);

        // Initialize the message list and adapter
        messageList = new ArrayList<>();
        messageAdapter = new MessageAdapter(this, messageList);

        // Set up the RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(messageAdapter);

        // Set up the send button click listener
        buttonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                start.setVisibility(View.INVISIBLE);
                String messageContent = editTextMessage.getText().toString();
                if (!messageContent.isEmpty() || selectedImage != null) {
                    Message message = new Message(messageContent, selectedImage, true);
                    messageList.add(message);
                    messageAdapter.notifyItemInserted(messageList.size() - 1);
                    editTextMessage.setText("");
                    // Scroll to the bottom
                    recyclerView.scrollToPosition(messageList.size() - 1);

                    // Upload the image and text if any exist
                    if (selectedImage != null || !messageContent.isEmpty()) {
                        uploadImageAndText(selectedImage, messageContent);
                        selectedImage = null; // Clear the selected image after uploading
                    }

                    // Simulate receiving a response
                    downloadText(messageContent);
                }
            }
        });

        // Add a global layout listener to handle keyboard visibility changes
        layoutRoot.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                Rect r = new Rect();
                layoutRoot.getWindowVisibleDisplayFrame(r);
                int screenHeight = layoutRoot.getRootView().getHeight();
                int keypadHeight = screenHeight - r.bottom;

                if (keypadHeight > screenHeight * 0.15) {
                    // Keyboard is open
                    recyclerView.scrollToPosition(messageList.size() - 1);
                }
            }
        });

        // Set up the delete button click listener
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Clear the message list
                messageList.clear();
                messageAdapter.notifyDataSetChanged();

                // Make the start layout visible
                start.setVisibility(View.VISIBLE);
            }
        });

        // Set up the photo button click listener
        buttonPhoto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Start CameraActivity to capture or pick a photo
                Intent intent = new Intent(MainActivity.this, CameraActivity.class);
                startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
            }
        });
    }

    //是否使用特殊的标题栏背景颜色，android5.0以上可以设置状态栏背景色，如果不使用则使用透明色值
    protected boolean useThemestatusBarColor = false;
    //是否使用状态栏文字和图标为暗色，如果状态栏采用了白色系，则需要使状态栏和图标为暗色，android6.0以上可以设置
    protected boolean useStatusBarColor = true;
    protected void setStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {//5.0及以上
            View decorView = getWindow().getDecorView();
            int option = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            decorView.setSystemUiVisibility(option);
            //根据上面设置是否对状态栏单独设置颜色
            if (useThemestatusBarColor) {
                getWindow().setStatusBarColor(getResources().getColor(R.color.colortheme));//设置状态栏背景色
            } else {
                getWindow().setStatusBarColor(Color.TRANSPARENT);//透明
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {//4.4到5.0
            WindowManager.LayoutParams localLayoutParams = getWindow().getAttributes();
            localLayoutParams.flags = (WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS | localLayoutParams.flags);
        } else {
            Toast.makeText(this, "低于4.4的android系统版本不存在沉浸式状态栏", Toast.LENGTH_SHORT).show();
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && useStatusBarColor) {//android6.0以后可以对状态栏文字颜色和图标进行修改
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            String selectedImageUriString = data.getStringExtra("selectedImageUri");
            if (selectedImageUriString != null) {
                Uri selectedImageUri = Uri.parse(selectedImageUriString);
                try {
                    InputStream imageStream = getContentResolver().openInputStream(selectedImageUri);
                    selectedImage = BitmapFactory.decodeStream(imageStream);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void uploadImageAndText(Bitmap bitmap, String text) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    MultipartBody.Builder builder = new MultipartBody.Builder()
                            .setType(MultipartBody.FORM);

                    if (bitmap != null) {
                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                        byte[] byteArray = stream.toByteArray();
                        String imageFilename = "upload_" + System.currentTimeMillis() + ".jpg";
                        builder.addFormDataPart("image", imageFilename,
                                RequestBody.create(byteArray, MediaType.parse("image/jpeg")));
                    }

                    if (text != null && !text.isEmpty()) {
                        // 创建一个临时文本文件并将文本写入其中
                        File textFile = createTempTextFile(text);
                        String textFilename = "upload_" + System.currentTimeMillis() + ".txt";
                        builder.addFormDataPart("text", textFilename,
                                RequestBody.create(textFile, MediaType.parse("text/plain")));
                    }

                    RequestBody requestBody = builder.build();

                    OkHttpClient client = new OkHttpClient();

                    Request request = new Request.Builder()
                            .url("https://daa5-123-15-50-25.ngrok-free.app/upload")
                            .post(requestBody)
                            .build();

                    client.newCall(request).enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            e.printStackTrace();
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            if (!response.isSuccessful()) {
                                throw new IOException("Unexpected code " + response);
                            }
                            // 处理服务器的响应（如果需要）
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private File createTempTextFile(String text) throws IOException {
        File tempFile = File.createTempFile("upload", ".txt", getCacheDir());
        tempFile.deleteOnExit();
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write(text);
        }
        return tempFile;
    }

    // 下载服务器返回的txt文件并解析
    private void downloadText(String imageFilename) {
        // 构建请求URL
        String url = "https://daa5-123-15-50-25.ngrok-free.app/return_content/" + imageFilename;

        // 发起GET请求
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    try {
                        JSONObject json = new JSONObject(responseBody);
                        boolean success = json.getBoolean("success");
                        if (success) {
                            String content = json.getString("content");
                            // 在界面上显示解析后的文本
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Message responseMessage = new Message(content, null, false);
                                    messageList.add(responseMessage);
                                    messageAdapter.notifyItemInserted(messageList.size() - 1);
                                    recyclerView.scrollToPosition(messageList.size() - 1);
                                }
                            });
                        } else {
                            String message = json.getString("message");
                            // 处理错误消息
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } else {
                    // 处理响应失败的情况
                }
            }
        });
    }
}