package com.example.asus_pc.cekbulyeni;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class Kullanici extends AppCompatActivity {
    TextView text;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kullanici);

        getSupportActionBar().hide();
        text=(TextView)findViewById(R.id.textView);
        Intent gelen=getIntent();
        int sicil=gelen.getIntExtra("sicilNo",0);
        String sifre=gelen.getStringExtra("pass");
        text.setText("SicilNo:"+ sicil+ "  Sifre:"+sifre);
    }
}
