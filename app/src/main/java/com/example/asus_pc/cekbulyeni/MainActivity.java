package com.example.asus_pc.cekbulyeni;

import android.content.Context;
import android.content.Intent;
import android.preference.PreferenceActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import com.loopj.android.http.*;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.ConnectException;
import java.net.SocketTimeoutException;

import cz.msebera.android.httpclient.Header;

public class MainActivity extends AppCompatActivity {


    EditText sicil;
    EditText sifre;
    RadioButton yonetici,kullanici;
    Button login;
    TextView uyari;
    int pozisyonID;
    String userToken;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        sicil=(EditText)findViewById(R.id.input_sicil);
        sifre=(EditText)findViewById(R.id.input_password);
        uyari=(TextView)findViewById(R.id.uyari);
        login=(Button)findViewById(R.id.btn_login);
        alanKontrol();
        login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String pozisyon="";
                try {
                    userLogin();
                } catch (ConnectException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (SocketTimeoutException e) {
                    e.printStackTrace();
                }
                girisKontrol(pozisyon);

            }
        });

    }

    public void userLogin()  throws java.net.SocketTimeoutException, java.net.ConnectException, JSONException {
        String sicilNo_=sicil.getText().toString();
        String sifre_=sifre.getText().toString();
        String url="http://192.168.12.1:8080/api/login";
        try {

            AsyncHttpClient client = new AsyncHttpClient();
            client.setTimeout(5000);
            RequestParams postParams = new RequestParams();
            postParams.put("sicilNo", sicilNo_);
            postParams.put("parola",sifre_);
            //postParams.put("yöneticiMi",pozisyonID);
            client.post(url,postParams,new JsonHttpResponseHandler(){
                @Override
               public void onSuccess(int statusCode, Header[] headers, JSONObject json){
                   try {
                       String error_ = json.getString("error");
                       if(error_.equals("True")){
                            Toast.makeText(getApplicationContext(),"Giriş Başarısız", Toast.LENGTH_LONG).show();
                        }
                        else{
                            String yoneticiMi=json.getString("yoneticiMi");
                           userToken = json.getString("token");
                           if(yoneticiMi.equals("True"))
                           {
                               Intent inte=new Intent(MainActivity.this,Yonetici.class);
                               inte.putExtra("token",userToken);
                               startActivity(inte);
                           }
                           else{
                               Intent inte=new Intent(MainActivity.this,Kamera.class);
                               inte.putExtra("token",userToken);
                               startActivity(inte);
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

    private  void nullAlanKontrol(){
        uyari.setText("");
        String sicilNo=sicil.getText().toString();
        String pass=sifre.getText().toString();
        if(sicilNo.equals("")) uyari.setText("Sicil no alanı boş olamaz!");

        if(pass.equals("")) uyari.setText("Şifre alanı boş olamaz!");
        if(sicilNo.equals("")&&pass.equals("")) uyari.setText("Sicil no ve şifre alanları boş olamaz!");

    }




    private  void girisKontrol(String pozisyon){
        int sicilNo;
        String pass;
          nullAlanKontrol();

         switch (pozisyon){


             case "Kullanıcı":
                    // Kullanici Girişi
                sicilNo=0;
                    if(!sicil.getText().toString().equals(""))
                    {
                         sicilNo=Integer.parseInt(sicil.getText().toString());
                    }
                   pass  = sifre.getText().toString();



           //         Intent intent = new Intent(this,Yonetici.class);
                  //  startActivity(intent);
                  if(sicilNo==111&&pass.equals("test")){
                    Intent intent = new Intent(this,Kullanici.class);
                    intent.putExtra("sicilNo",sicilNo);
                    intent.putExtra("pass",pass);
                    startActivity(intent);

                    }
                    else {

                     if (!(sicilNo==0||pass.equals("")))
                         uyari.setText("Sicil no veya şifre hatalı!");
                  }


                    break;


                case  "Yönetici":
                    // Yönetici girişi

                    sicilNo=0;
                    if(!sicil.getText().toString().equals(""))
                    {
                        sicilNo=Integer.parseInt(sicil.getText().toString());
                    }
                    pass  = sifre.getText().toString();



                    //         Intent intent = new Intent(this,Yonetici.class);
                    //  startActivity(intent);
                    if(sicilNo==222||pass.equals("test")){
                        Intent intent = new Intent(this,Yonetici.class);
                        intent.putExtra("sicilNo",sicilNo);
                        intent.putExtra("pass",pass);
                        startActivity(intent);

                    }
                    else {
                        if (!(sicilNo==0||pass.equals("")))
                            uyari.setText("Sicil no veya şifre hatalı!");

                    }

                    break;
            }



    }
    private  void alanKontrol(){

        sicil.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String alanDurumu=uyari.getText().toString();
                if(alanDurumu.equals("Sicil no ve şifre alanları boş olamaz!")) uyari.setText("Şifre alanı boş olamaz!");
                if(alanDurumu.equals("Sicil no alanı boş olamaz!")) uyari.setText("");
            }
        });

        sifre.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String alanDurumu=uyari.getText().toString();
                if(alanDurumu.equals("Sicil no ve şifre alanları boş olamaz!")) uyari.setText("Sicil no alanı boş olamaz!");
                if(alanDurumu.equals("Şifre alanı boş olamaz!")) uyari.setText("");
            }
        });




    }


}
