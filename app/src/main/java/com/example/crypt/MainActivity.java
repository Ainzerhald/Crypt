package com.example.crypt;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RequiresApi(api = Build.VERSION_CODES.O)
public class MainActivity extends AppCompatActivity {

    static char[] symbol = {' ', 'А', 'Б', 'В', 'Г', 'Д', 'Е', 'Ё', 'Ж', 'З', 'И', 'Й', 'К',
            'Л', 'М', 'Н', 'О', 'П', 'Р', 'С', 'Т', 'У', 'Ф', 'Х', 'Ц', 'Ч', 'Ш', 'Щ', 'Ъ',
            'Ы', 'Ь', 'Э', 'Ю', 'Я', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'}; // Алфавит для ключа
    byte[] buffer;
    String dir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/CRYPT/";
    int file = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if(!Files.exists(Paths.get(dir))){//Создание папки CRYPT в случае его отсутствия
            try {
                Files.createDirectories(Paths.get(dir));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);//Запрос на доступ к файлам
    }

    public void open(View view){
        Intent file = new Intent(Intent.ACTION_GET_CONTENT);//Выбор файла
        file.setType("*/*");
        startActivityForResult(file, 1);
    }

    public void onActivityResult (int requestCode, int resultCode, Intent data ) {
        super.onActivityResult(requestCode, resultCode, data);
        try {
            ParcelFileDescriptor uri = getContentResolver().openFileDescriptor(data.getData(), "r");
            FileDescriptor fd = uri.getFileDescriptor();//Получение дескриптора файла
            ContentResolver cR = getContentResolver();
            MimeTypeMap mime = MimeTypeMap.getSingleton();
            String type = mime.getExtensionFromMimeType(cR.getType(data.getData()));//Получение расширение файла
            FileInputStream input = new FileInputStream(fd);
            buffer = new byte[input.available()];
            try {
                input.read(buffer, 0, buffer.length);//Получение массива байт кода файла
                input.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        file = 1;
    }

    public void execute(View view) throws IOException {
        EditText key = findViewById(R.id.key);
        if(!key.getText().toString().equals("")){
            if(key.getText().length() > 5){
                if(file != 0){
                    FileOutputStream asd = new FileOutputStream(dir + "sda.txt");
                    asd.write(buffer, 0, buffer.length);//Переписать шифровку или дешировку с условием выбора пользователя
                }
                else{
                    Toast.makeText(this, "Выберите файл", Toast.LENGTH_SHORT).show();
                }
            }
            else{
                Toast.makeText(this, "Ключ должен быть не менее 6 символов", Toast.LENGTH_SHORT).show();
            }
        }
        else{
            Toast.makeText(this, "Введите ключ", Toast.LENGTH_SHORT).show();
        }
    }
}