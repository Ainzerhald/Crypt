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
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.util.Date;

@RequiresApi(api = Build.VERSION_CODES.O)
public class MainActivity extends AppCompatActivity {

    static char[] symbol = {' ', 'А', 'Б', 'В', 'Г', 'Д', 'Е', 'Ё', 'Ж', 'З', 'И', 'Й', 'К',
            'Л', 'М', 'Н', 'О', 'П', 'Р', 'С', 'Т', 'У', 'Ф', 'Х', 'Ц', 'Ч', 'Ш', 'Щ', 'Ъ',
            'Ы', 'Ь', 'Э', 'Ю', 'Я', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'}; // Алфавит для ключа
    byte[] buffer;
    String dir = Environment.getExternalStorageDirectory().getAbsolutePath() + "/CRYPT/";
    int file = 0;
    String type;

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
            type = mime.getExtensionFromMimeType(cR.getType(data.getData()));//Получение расширение файла
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
        key.getText().toString().toUpperCase();
        if(!key.equals("")){
            if(key.length() > 5){
                if(file != 0){
                    String file_name = "";
                    Date date = new Date();
                    file_name += String.valueOf(date.getTime());
                    RadioButton check = findViewById(R.id.crypt);
                    if(check.isChecked()){
                        buffer = crypt(buffer, key.getText().toString().toUpperCase());
                        file_name += "(crypt).";
                    }
                    else{
                        buffer = decrypt(buffer, key.getText().toString().toUpperCase());
                        file_name += "(decrypt).";
                    }
                    FileOutputStream fout = new FileOutputStream(dir + file_name + type);
                    fout.write(buffer, 0, buffer.length);
                    Toast.makeText(this, "Операция выполнена", Toast.LENGTH_SHORT).show();
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

    protected byte[] crypt(byte[] buf, String key){
        int control = 0;
        for(int i = 0; i < buffer.length; i++) {
            if(control == key.length()) {
                control = 0;
            }
            buffer[i] = (byte) (buffer[i] + new String(symbol).indexOf(key.charAt(control)));
            control++;
        }
        return buf;
    }

    protected byte[] decrypt(byte[] buf, String key){
        int control = 0;
        for(int i = 0; i < buffer.length; i++) {
            if(control == key.length()) {
                control = 0;
            }
            buffer[i] = (byte) (buffer[i] - new String(symbol).indexOf(key.charAt(control)));
            control++;
        }
        return buf;
    }
}