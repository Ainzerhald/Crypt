package com.example.crypt;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import org.w3c.dom.Text;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;

@RequiresApi(api = Build.VERSION_CODES.O)
public class Main extends AppCompatActivity {

    byte[] buffer;
    String dir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/CRYPT/";
    int file = 0;
    String type;

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
        Intent file = new Intent(Intent.ACTION_GET_CONTENT);//Выбор файла
        file.setType("*/*");
        startActivityForResult(file, 1);
    }

    FileDescriptor fd = null;

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            ParcelFileDescriptor uri = getContentResolver().openFileDescriptor(data.getData(), "r");
            fd = uri.getFileDescriptor();//Получение дескриптора файла
            ContentResolver cR = getContentResolver();
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            type = mime.getExtensionFromMimeType(cR.getType(data.getData()));//Получение расширение файла
            file = 1;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected byte[] symbol;

    public void execute(View view) {
        EditText key = findViewById(R.id.key);
        symbol = key.getText().toString().getBytes();
        if (!key.getText().toString().equals("")) {
            if (key.length() > 5) {
                if (file != 0) {
                    RadioButton check = findViewById(R.id.crypt);
                    if (check.isChecked()) {
                        crypt(key.getText().toString());
                    } else {
                        decrypt(key.getText().toString());
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

    protected void crypt(String key) {
        process(key, true);
    }

    protected void decrypt(String key) {
        process(key, false);
    }

    @SuppressLint("StaticFieldLeak")
    public void process(String key, boolean crypt) {

        ProgressBar bar = findViewById(R.id.progressBar);
        new AsyncTask<Void, Double, Void>(){

            @SuppressLint("StaticFieldLeak")
            @Override
            protected Void doInBackground(Void... voids) {
                String file_name = "";
                Date date = new Date();
                file_name += date.getTime();
                FileInputStream input = new FileInputStream(fd);
                FileOutputStream out = null;
                try {
                    out = new FileOutputStream(dir + file_name + "." + type);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                int t = -1;
                if(crypt){
                    t = 1;
                }
                int bit = 1024;
                int key_len = 0;
                symbol = key.getBytes();
                try {
                    int size = input.available();
                    int check = 0;
                    while(size > bit) {
                        size -= bit;
                        check++;
                    }
                    boolean control = false;
                    int dop = 0;
                    if(size != 0) {
                        control = true;
                        dop = size;
                    }
                    int metr = input.available();
                    buffer = new byte[bit];
                    for(int i = 0; i < check; i++) {
                        input.read(buffer, 0, buffer.length);
                        for(int g = 0; g < buffer.length; g++) {
                            publishProgress((double) (((i * bit + g) / metr) * 100));
                            if(key_len == key.length()){
                                key_len = 0;
                            }
                            buffer[g] = (byte) (buffer[g] + t * symbol[key_len]);
                            key_len++;
                        }
                        out.write(buffer, 0, buffer.length);
                    }
                    if(control) {
                        buffer = new byte[dop];
                        input.read(buffer, 0, buffer.length);
                        for(int i = 0; i < buffer.length; i++){
                            publishProgress((double) (((check * bit + i) / metr) * 100));
                            if(key_len == key.length()){
                                key_len = 0;
                            }
                            buffer[i] = (byte) (buffer[i] + t * symbol[key_len]);
                        }
                        out.write(buffer, 0, buffer.length);
                        input.close();
                        out.close();
                        publishProgress((double) (100));
                    }
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
                return null;
            }

            protected void onProgressUpdate(Double... values) {
                super.onProgressUpdate(values);
                TextView progress = findViewById(R.id.progress);
                progress.setText(values[0] + "%");
            }

            protected void onPreExecute() {
                super.onPreExecute();
                bar.setVisibility(View.VISIBLE);
            }

            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                bar.setVisibility(View.INVISIBLE);
            }
        }.execute();

    }

}