package com.example.crypt;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Intent;
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
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class Main extends AppCompatActivity {

    byte[] buffer;
    String dir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/CRYPT/";
    int file = 0;
    String type;
    private RandomAccessFile RandomAccessFileout;

    @RequiresApi(api = Build.VERSION_CODES.O)
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
    boolean activ = false;

    public void execute(View view) throws IOException {
        if(activ){
            Toast.makeText(this, "Дождитесь завершения предыдущего процесса", Toast.LENGTH_SHORT).show();
            return;
        }
        EditText key = findViewById(R.id.key);
        symbol = key.getText().toString().getBytes();
        if (!key.getText().toString().equals("")) {
            if (key.length() > 5) {
                if (file != 0) {
                    RadioButton check = findViewById(R.id.crypt);
                    process(key.getText().toString(), check.isChecked());
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
    public void process(String key, boolean crypt) throws IOException {
        View relative = findViewById(R.id.relative);
        ProgressBar bar = findViewById(R.id.progressBar);
        final FileInputStream[] input = {new FileInputStream(fd)};
        String file_name = "";
        Date date = new Date();
        file_name += date.getTime();
        RandomAccessFile out = null;
        if(crypt){
            file_name += "(crypt)";
        }
        else{
            file_name += "(decrypt)";
        }
        try{
            out = new RandomAccessFile(dir + file_name + "." + type, "rw");
        }
        catch (FileNotFoundException ignored){
        }
        relative.setVisibility(View.VISIBLE);
        Log.i("length", String.valueOf(input[0].available()));
        out.setLength(input[0].available());
        activ = true;
        RandomAccessFile finalOut = out;
        new Thread(() -> {
            int t = -1;
            if(crypt){
                t = 1;
            }
            int bit = 1024;
            final int[] key_len = {0};
            AtomicInteger thread_close = new AtomicInteger();
            final int[] last = {0}; // последнее значение прогресса
            final int[] progress = {0};
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
                final AtomicBoolean[] error = {new AtomicBoolean(false)};
                int finalCheck = check;
                new Thread(()->{
                    while(true){
                        progress[0] = thread_close.get() * 100 / finalCheck;
                        if(last[0] < progress[0]){
                            last[0] = progress[0];
                            bar.setProgress(progress[0]);
                        }
                        if(progress[0] == 100){
                            activ = false;
                            file = 0;
                            relative.setVisibility(View.INVISIBLE);
                            break;
                        }
                    }
                }).start();
                for(int i = 0; i <= check; i++) {
                    if(i == check && control){
                        int finalDop = dop;
                        new Thread(()->{
                            buffer = new byte[finalDop];
                            error[0].set(true);
                            while(error[0].get()) {
                                try {
                                    input[0].read(buffer, 0, buffer.length);
                                    error[0].set(false);
                                } catch (IOException e) {
                                    error[0].set(true);
                                    try {
                                        input[0].close();
                                    } catch (IOException ioException) {
                                        ioException.printStackTrace();
                                    }
                                    fd = uri.getFileDescriptor();
                                    input[0] = new FileInputStream(fd);
                                    e.printStackTrace();
                                }
                            }
                            for(int g = 0; g < buffer.length; g++){
                                if(key_len[0] == key.length()){
                                    key_len[0] = 0;
                                }
                                //buffer[g] = (byte) (buffer[g] + t * symbol[key_len[0]]);
                            }
                            try {
                                finalOut.write(buffer, 0, buffer.length);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            thread_close.set(thread_close.get() + 1);
                        }).start();
                    }
                    if(i != check) {
                        new Thread(() -> {
                            error[0].set(true);
                            while (error[0].get()) {
                                try {
                                    input[0].read(buffer, 0, buffer.length);
                                    error[0].set(false);
                                } catch (IOException e) {
                                    error[0].set(true);
                                    try {
                                        input[0].close();
                                    } catch (IOException ioException) {
                                        ioException.printStackTrace();
                                    }
                                    fd = uri.getFileDescriptor();
                                    input[0] = new FileInputStream(fd);
                                    e.printStackTrace();
                                }
                            }
                            for (int g = 0; g < buffer.length; g++) {
                                if (key_len[0] == key.length()) {
                                    key_len[0] = 0;
                                }
                                //buffer[g] = (byte) (buffer[g] + t * symbol[key_len[0]]);
                                key_len[0]++;
                            }
                            try {
                                finalOut.write(buffer, 0, buffer.length);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            thread_close.set(thread_close.get() + 1);
                        }).start();
                    }
                }
            } catch (IOException e2) {
                e2.printStackTrace();
            }
        }).start();
    }

}