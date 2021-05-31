package com.example.crypt;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.squareup.okhttp.Protocol;
import com.yandex.authsdk.YandexAuthAccount;
import com.yandex.authsdk.YandexAuthException;
import com.yandex.authsdk.YandexAuthLoginOptions;
import com.yandex.authsdk.YandexAuthOptions;
import com.yandex.authsdk.YandexAuthSdk;
import com.yandex.authsdk.YandexAuthToken;
import com.yandex.disk.rest.Credentials;
import com.yandex.disk.rest.OkHttpClientFactory;
import com.yandex.disk.rest.ResourcesArgs;
import com.yandex.disk.rest.RestClient;
import com.yandex.disk.rest.exceptions.NetworkIOException;
import com.yandex.disk.rest.exceptions.ServerException;
import com.yandex.disk.rest.exceptions.ServerIOException;
import com.yandex.disk.rest.json.Resource;
import com.yandex.disk.rest.json.ResourceList;
import com.yandex.disk.rest.retrofit.CloudApi;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import retrofit.RestAdapter;

public class Main extends AppCompatActivity {

    private static final int FIND_FILE = 1;
    private static final int REQUEST_LOGIN_SDK = 2;
    YandexAuthSdk sdk;

    byte[] buffer;
    String dir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/CRYPT/";
    int file = 0;
    int tmpFIle = 0;
    File tmp;
    String type;
    String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);//Запрос на доступ к файлам
        if (!Files.exists(Paths.get(dir))) {//Создание папки CRYPT в случае его отсутствия
            try {
                Files.createDirectories(Paths.get(dir));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void open(View view) {
        if (activ) {
            Toast.makeText(this, "Дождитесь завершения предыдущего процесса", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent file = new Intent(Intent.ACTION_GET_CONTENT);//Выбор файла
        file.setType("*/*");
        startActivityForResult(file, FIND_FILE);
    }

    FileDescriptor fd = null;
    ParcelFileDescriptor uri = null;

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == FIND_FILE) {
            try {
                uri = getContentResolver().openFileDescriptor(data.getData(), "r");
                fd = uri.getFileDescriptor();//Получение дескриптора файла
                ContentResolver cR = getContentResolver();
                MimeTypeMap mime = MimeTypeMap.getSingleton();
                type = mime.getExtensionFromMimeType(cR.getType(data.getData()));//Получение расширение файла
                file = 1;
                tmpFIle = 0;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (requestCode == REQUEST_LOGIN_SDK) {
            try {
                final YandexAuthToken yandexAuthToken = sdk.extractToken(resultCode, data);
                if (yandexAuthToken != null) {
                    token = yandexAuthToken.getValue();
                }
                else{
                    RadioButton fromMemory = findViewById(R.id.saveMemory);
                    fromMemory.setChecked(true);
                }
            } catch (YandexAuthException ignored) {
            }
        }
    }

    protected byte[] symbol;
    boolean activ = false;

    public void execute(View view) throws FileNotFoundException {
        if (activ) {
            Toast.makeText(this, "Дождитесь завершения предыдущего процесса", Toast.LENGTH_SHORT).show();
            return;
        }
        EditText key = findViewById(R.id.key);
        symbol = key.getText().toString().getBytes();
        RadioButton saves = findViewById(R.id.saveDisk);
        boolean fromDisk;
        if(saves.isChecked()){
            fromDisk = true;
        }
        else{
            fromDisk = false;
        }
        if (!key.getText().toString().equals("")) {
            if (key.length() > 5) {
                if (file != 0 || tmpFIle != 0) {
                    RadioButton check = findViewById(R.id.crypt);
                    if (check.isChecked()) {
                        Log.i("sad", String.valueOf(fromDisk));
                        process(key.getText().toString(), true, fromDisk);
                    } else {
                        process(key.getText().toString(), false, fromDisk);
                    }
                } else {
                    Toast.makeText(this, "Выберите файл", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(this, "Ключ должен быть не менее 6 символов", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Введите ключ", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressLint("StaticFieldLeak")
    public void process(String key, boolean crypt, boolean fromDisk) throws FileNotFoundException {

        String fullPath = null;
        View relative = findViewById(R.id.relative);
        ProgressBar bar = findViewById(R.id.progressBar);
        final FileInputStream[] input = new FileInputStream[1];
        if(tmpFIle == 1){
            input[0] = new FileInputStream(tmp);
        }
        else if(file == 1){
            input[0] = new FileInputStream(fd);
        }
        FileOutputStream out = null;
        String file_name = "";
        Date date = new Date();
        file_name += date.getTime();
        if (crypt) {
            file_name += "(crypt)";
        } else {
            file_name += "(decrypt)";
        }
        try {
            fullPath = dir + file_name + "." + type;
            out = new FileOutputStream(fullPath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        FileOutputStream finalOut = out;
        String finalFile_name = file_name;
        String finalFullPath = fullPath;;
        new AsyncTask<Void, Integer, Void>() {

            @SuppressLint("StaticFieldLeak")
            @Override
            protected Void doInBackground(Void... voids) {
                int t = -1;
                if (crypt) {
                    t = 1;
                }
                int bit = 1024;
                int key_len = 0;
                symbol = key.getBytes();
                try {
                    int size = input[0].available();
                    int check = 0;
                    while (size > bit) {
                        size -= bit;
                        check++;
                    }
                    boolean control = false;
                    int dop = 0;
                    if (size != 0) {
                        control = true;
                        dop = size;
                    }
                    buffer = new byte[bit];
                    boolean error;
                    for (int i = 0; i < check; i++) {
                        error = true;
                        while (error) {
                            try {
                                input[0].read(buffer, 0, buffer.length);
                                error = false;
                            } catch (IOException e) {
                                error = true;
                                input[0].close();
                                fd = uri.getFileDescriptor();
                                input[0] = new FileInputStream(fd);
                                e.printStackTrace();
                            }
                        }
                        publishProgress((int) ((double) i / check * 100));
                        for (int g = 0; g < buffer.length; g++) {
                            if (key_len == key.length()) {
                                key_len = 0;
                            }
                            buffer[g] = (byte) (buffer[g] + t * symbol[key_len]);
                            key_len++;
                        }
                        finalOut.write(buffer, 0, buffer.length);
                    }
                    if (control) {
                        buffer = new byte[dop];
                        error = true;
                        while (error) {
                            try {
                                input[0].read(buffer, 0, buffer.length);
                                error = false;
                            } catch (IOException e) {
                                error = true;
                                input[0].close();
                                fd = uri.getFileDescriptor();
                                input[0] = new FileInputStream(fd);
                                e.printStackTrace();
                            }
                        }
                        for (int i = 0; i < buffer.length; i++) {
                            if (key_len == key.length()) {
                                key_len = 0;
                            }
                            buffer[i] = (byte) (buffer[i] + t * symbol[key_len]);
                        }
                        finalOut.write(buffer, 0, buffer.length);
                        input[0].close();
                        finalOut.close();
                        publishProgress(100);
                    }
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
                return null;
            }

            protected void onProgressUpdate(Integer... values) {
                super.onProgressUpdate(values);
                bar.setProgress(values[0]);
            }

            protected void onPreExecute() {
                super.onPreExecute();
                relative.setVisibility(View.VISIBLE);
                activ = true;
            }

            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                relative.setVisibility(View.INVISIBLE);
                if(fromDisk){
                    Credentials credentials = new Credentials("LK-9D9", token);
                    RestClient rest = new RestClient(credentials, OkHttpClientFactory.makeClient().setProtocols(Arrays.asList(Protocol.HTTP_1_1)));
                    new Thread(() -> {
                        try {
                            Log.i("ads", "Загрузка началась");
                            rest.uploadFile(rest.getUploadLink("/CRYPT/" + finalFile_name + "." + type, true), true, new File(finalFullPath), null);
                            Log.i("ads", "Загрузка завершена");
                        } catch (IOException | ServerException e) {
                            e.printStackTrace();
                        }
                        try {
                            Files.delete(new File(finalFullPath).toPath());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }).start();
                }
                activ = false;
                if(tmpFIle == 1){
                    tmp.delete();
                }
                file = 0;
                tmpFIle = 0;
            }
        }.execute();
    }

    public void get_token() {
        sdk = new YandexAuthSdk(this, new YandexAuthOptions.Builder(this)
                .enableLogging().build());
        final YandexAuthLoginOptions.Builder loginOptionsBuilder = new YandexAuthLoginOptions.Builder();
        startActivityForResult(sdk.createLoginIntent(loginOptionsBuilder.build()), REQUEST_LOGIN_SDK);
    }

    public void manager(View view) {
        if (token == null) {
            get_token();
        }
        else{
            setContentView(R.layout.disk);
            folder("/");
        }
    }

    private void folder(String path){
        Thread thread = new Thread(()-> {
            try {
                Credentials credentials = new Credentials("LK-9D9", token);
                RestClient rest = new RestClient(credentials, OkHttpClientFactory.makeClient().setProtocols(Arrays.asList(Protocol.HTTP_1_1)));
                ResourcesArgs arg = new ResourcesArgs.Builder().setPath(path).build();
                ResourceList resourceList = rest.getResources(arg).getResourceList();
                List<Resource> items = resourceList.getItems();
                runOnUiThread(() -> button_builder(path, items));
            } catch (IOException | ServerException e) {
                e.printStackTrace();
            }
        });
        thread.start();
    }

    public  void button_builder(String path, List<Resource> items){
        setContentView(R.layout.disk);
        String back = path.substring(0, path.lastIndexOf("/"));
        final LinearLayout linear = findViewById(R.id.linear);
        for (int i = 0; i <= items.size(); i++) {
            final Button b = new Button(getApplicationContext());
            b.setLayoutParams(
                    new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT)
            );
            b.setId(i);
            if(i == items.size()){
                b.setText("Назад");
                b.setOnClickListener(v -> {
                    if(path.equals("/")){
                        setContentView(R.layout.main);
                    }
                    else{
                        folder(back.substring(0, back.lastIndexOf("/") + 1));
                    }
                });
            }
            else{
                b.setText(items.get(i).getName());
                if(items.get(i).getMediaType() == null){
                    int finalI = i;
                    b.setOnClickListener(v -> {
                        Log.i("yandex", path);
                        folder(path + items.get(finalI).getName() + "/");
                    });
                }
                else{
                    int finalI = i;
                    b.setOnClickListener(V -> {
                        type = items.get(finalI).getName();
                        type = type.substring(type.lastIndexOf(".") + 1);
                        download(path + items.get(finalI).getName() + "/");
                    });
                }
            }
            linear.addView(b);
        }
    }
    public void download(String path){
        new Thread(() -> {
            try{
                Credentials credentials = new Credentials("LK-9D9", token);
                RestClient rest = new RestClient(credentials, OkHttpClientFactory.makeClient().setProtocols(Arrays.asList(Protocol.HTTP_1_1)));
                tmp = File.createTempFile("data", null);
                rest.downloadFile(path, tmp, null);
                runOnUiThread(()->setContentView(R.layout.main));
                tmpFIle = 1;
                file = 0;
            } catch (IOException | ServerException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void checkToken(View view){
        if(token == null){
            get_token();
        }
    }
}