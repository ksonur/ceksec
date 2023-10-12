package com.example.asus_pc.cekbulyeni;

import android.app.Dialog;
import android.app.KeyguardManager;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.regex.Matcher;

import cz.msebera.android.httpclient.Header;

public class Yonetici extends AppCompatActivity {
    EditText sicilNo,ad,soyad,sifre,eklenecekSicilNo;
    Button silButton;
    Intent inte;
    String userToken;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_yonetici);
        inte=this.getIntent();
        userToken=inte.getExtras().getString("token");
        silButton=(Button)findViewById(R.id.silButton);
        sicilNo=(EditText)findViewById(R.id.sicilNoEditText);
        eklenecekSicilNo=(EditText)findViewById(R.id.eklenecekSicilNoEditText);
        sifre=(EditText)findViewById(R.id.sifreEditText);
        ad=(EditText)findViewById(R.id.adıEditText);
        soyad=(EditText)findViewById(R.id.soyadıEditText);
//        text=(TextView)findViewById(R.id.textView);
//        Intent gelen=getIntent();
//        int sicil=gelen.getIntExtra("sicilNo",0);
//        String sifre=gelen.getStringExtra("pass");
//        text.setText("SicilNo:"+ sicil+ "  Sifre:"+sifre);
    }

    public void kayıtEkle(View view){
        try {
            addUser();
        }catch (ConnectException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (SocketTimeoutException e) {
            e.printStackTrace();
        }
    }

    public void addUser()  throws java.net.SocketTimeoutException, java.net.ConnectException, JSONException {
        String url="http://192.168.12.1:8080/api/kullaniciEkle";
        try {
            AsyncHttpClient client = new AsyncHttpClient();
            client.setTimeout(5000);
            RequestParams postParams = new RequestParams();

            postParams.put("sicilNo", eklenecekSicilNo.getText());
            postParams.put("token",userToken);
            postParams.put("parola",sifre.getText());
            postParams.put("adi",ad.getText());
            postParams.put("soyadi",soyad.getText());

            //postParams.put("yöneticiMi",pozisyonID); error tcNo adi soyadi dogumTarihi sicil askerlik araniyorMu
            client.post(url,postParams,new JsonHttpResponseHandler(){
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject json){
                    try {
                        String error_ = json.getString("error");
                        String message_=json.getString("message");

                        if(error_.equals("True")){
                            if(message_.equals("tokenGecersiz")){
                                Kamera.showDialog("Lütfen Uygulamayı Tekrar Başlatınız.",Yonetici.this);
                            }
                            else if(message_.equals("yoneticiDegil")){
                                Kamera.showDialog("Yönetici Olmadığınız İçin Bu İşlemi Yapamazsınız.",Yonetici.this);
                            }
                            else if(message_.equals("kullaniciVar")){
                                Kamera.showDialog("Böyle bir kullanıcı zaten bulunuyor.",Yonetici.this);
                            }
                        }
                        else if(error_.equals("False")){
                            if(message_.equals("basarili")){
                                Kamera.showDialog("Ekleme İşlemi Başarıyla Gerçekleşti",Yonetici.this);
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable t, JSONObject e) {
                    t.printStackTrace();
                }
            });

        }catch (Exception e){
            e.printStackTrace();
        }

    }


    public void kayitSil(View view){
        try {
            deleteUser();
        }catch (ConnectException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        } catch (SocketTimeoutException e) {
            e.printStackTrace();
        }
    }

    public void deleteUser()  throws java.net.SocketTimeoutException, java.net.ConnectException, JSONException {
        String url="http://192.168.12.1:8080/api/kullaniciSil";
        try {
            AsyncHttpClient client = new AsyncHttpClient();
            client.setTimeout(5000);
            RequestParams postParams = new RequestParams();

            postParams.put("sicilNo", sicilNo.getText());
            postParams.put("token",userToken);
            //postParams.put("yöneticiMi",pozisyonID); error tcNo adi soyadi dogumTarihi sicil askerlik araniyorMu
            client.post(url,postParams,new JsonHttpResponseHandler(){
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject json){
                    try {
                            String error_ = json.getString("error");
                            String message_=json.getString("message");

                            if(error_.equals("True")){
                                if(message_.equals("tokenGecersiz")){
                                    Kamera.showDialog("Lütfen Uygulamayı Tekrar Başlatınız.",Yonetici.this);
                                }
                                else if(message_.equals("yoneticiDegil")){
                                    Kamera.showDialog("Yönetici Olmadığınız İçin Bu İşlemi Yapamazsınız.",Yonetici.this);
                                }
                                else if(message_.equals("kullaniciYok")){
                                    Kamera.showDialog("Böyle bir kullanıcı bulunmuyor",Yonetici.this);
                                }
                            }
                            else if(error_.equals("False")){
                                if(message_.equals("basarili")){
                                    Kamera.showDialog("Silme İşlemi Başarıyla Gerçekleşti",Yonetici.this);
                                }
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                }
                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable t, JSONObject e) {
                    t.printStackTrace();
                }
            });

        }catch (Exception e){
            e.printStackTrace();
        }

    }
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

}
