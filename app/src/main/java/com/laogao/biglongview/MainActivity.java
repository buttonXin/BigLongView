package com.laogao.biglongview;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        BigLongView bigLongView = findViewById(R.id.blv_view);


        try {
            InputStream is = getAssets().open("bing_long_pic.jpg");
            bigLongView.setImage(is);
        } catch (IOException e) {
            e.printStackTrace();
        }


    }
}
