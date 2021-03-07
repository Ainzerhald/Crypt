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
import android.util.Log;
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
    ParcelFileDescriptor uri = null;

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            uri = getContentResolver().openFileDescriptor(data.getData(), "r");
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
        final FileInputStream[] input = {new FileInputStream(fd)};
        FileOutputStream out = null;
        String file_name = "";
        Date date = new Date();
        file_name += date.getTime();
        try {
            out = new FileOutputStream(dir + file_name + "." + type);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        FileOutputStream finalOut = out;
        new AsyncTask<Void, Integer, Void>(){

            @SuppressLint("StaticFieldLeak")
            @Override
            protected Void doInBackground(Void... voids) {
                int t = -1;
                if(crypt){
                    t = 1;
                }
                int bit = 1024
                        ;
                int key_len = 0;
                symbol = key.getBytes();
                try {
                    int size = input[0].available();
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
                    buffer = new byte[bit];
                    boolean error = false;
                    for(int i = 0; i < check; i++) {
                        error = true;
                        while(error) {
                            try {
                                input[0] = new FileInputStream(fd);
                                input[0].read(buffer, 0, buffer.length);
                                error = false;
                            } catch (IOException e) {
                                error = true;
                                input[0].close();
                                fd = uri.getFileDescriptor();
                                e.printStackTrace();
                            }
                        }
                        publishProgress( (int)((double) i / check * 100));
                        for(int g = 0; g < buffer.length; g++) {
                            if(key_len == key.length()){
                                key_len = 0;
                            }
                            //buffer[g] = (byte) (buffer[g] + t * symbol[key_len]);
                            key_len++;
                        }
                        finalOut.write(buffer, 0, buffer.length);
                    }
                    if(control) {
                        buffer = new byte[dop];
                        error = true;
                        while(error) {
                            try {
                                input[0] = new FileInputStream(fd);
                                input[0].read(buffer, 0, buffer.length);
                                error = false;
                            } catch (IOException e) {
                                error = true;
                                input[0].close();
                                fd = uri.getFileDescriptor();
                                e.printStackTrace();
                            }
                        }
                        for(int i = 0; i < buffer.length; i++){
                            if(key_len == key.length()){
                                key_len = 0;
                            }
                            //buffer[i] = (byte) (buffer[i] + t * symbol[key_len]);
                        }
                        finalOut.write(buffer, 0, buffer.length);
                        Log.i("Progress", "i = " + check + " check = " + check + " result = " + (int)((double) check / check * 100));
                        input[0].close();
                        finalOut.close();
                        publishProgress( 100);
                    }
                } catch (IOException e2) {
                    e2.printStackTrace();
                }
                return null;
            }

            protected void onProgressUpdate(Integer... values) {
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