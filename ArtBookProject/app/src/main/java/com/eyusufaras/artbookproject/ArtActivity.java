package com.eyusufaras.artbookproject;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Toast;

import com.eyusufaras.artbookproject.databinding.ActivityArtBinding;
import com.google.android.material.snackbar.Snackbar;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

public class ArtActivity extends AppCompatActivity {

    ActivityResultLauncher<Intent> activityResultLauncher;   //galeriye girmek için
    ActivityResultLauncher<String> permissionLauncher;     //izin istemek için
    Bitmap selectedImage;
    private ActivityArtBinding binding;
    SQLiteDatabase database;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityArtBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        registerLauncher();

        database = this.openOrCreateDatabase("Arts", MODE_PRIVATE, null);

        Intent intent = getIntent();
        String info = intent.getStringExtra("info");
        if(info.equals("new")){   //new art
            binding.nameText.setText("");  //yeni ekleneceği için boş oldugundan emin olmak için
            binding.ArtistText.setText("");
            binding.YearText.setText("");
            binding.button.setVisibility(View.VISIBLE);  //buton görünür
            binding.imageView.setImageResource(R.drawable.selectimage);

        }else{   //id den onu getir
            int artId=intent.getIntExtra("artId",0);
            binding.button.setVisibility(view.INVISIBLE);  //buton görünmez

            try {
                Cursor cursor=database.rawQuery("SELECT*FROM arts WHERE id=?",new String[] {String.valueOf(artId)});
                int artNameIx=cursor.getColumnIndex("artname");
                int painterNameIx=cursor.getColumnIndex("paintername");
                int yearIx=cursor.getColumnIndex("year");
                int imageIx=cursor.getColumnIndex("image");

                while(cursor.moveToNext()){
                    binding.nameText.setText(cursor.getString(artNameIx));
                    binding.ArtistText.setText(cursor.getString(painterNameIx));
                    binding.YearText.setText(cursor.getString(yearIx));

                    byte[] bytes=cursor.getBlob(imageIx);
                    Bitmap bitmap= BitmapFactory.decodeByteArray(bytes,0,bytes.length);
                    binding.imageView.setImageBitmap(bitmap);
                }
                cursor.close();


            }catch (Exception e){
                e.printStackTrace();
            }
        }

    }

    public void save(View view){
        String name=binding.nameText.getText().toString();
        String artistName=binding.ArtistText.getText().toString();
        String year=binding.YearText.toString();

        Bitmap smallImage=makeSmallerImage(selectedImage,300);

        //image byte çevrilir kayıt eddebilmek için
        ByteArrayOutputStream outputStream=new ByteArrayOutputStream();
        smallImage.compress(Bitmap.CompressFormat.PNG,50,outputStream);
        byte[] byteArray=outputStream.toByteArray();

        //kayıt işlemi
        try {
            database.execSQL("CREATE TABLE IF NOT EXISTS arts(id INTEGER PRIMARY KEY, artname VARCHAR,paintername VARCHAR,year VARCHAR,İMAGE BLOB)"); //image blob olur

            String sqlString="İNSERT İNTO arts(artname,paintername,year,image) VALUES(?,?,?,?)";
            SQLiteStatement sqLiteStatement=database.compileStatement(sqlString);
            sqLiteStatement.bindString(1,name);
            sqLiteStatement.bindString(2,artistName);
            sqLiteStatement.bindString(3,year);
            sqLiteStatement.bindBlob(4,byteArray);
            sqLiteStatement.execute();

        }catch (Exception e){
            e.printStackTrace();
        }
        //finish();  //kullanılırsa mainActivity e döner olur fakat başka kullanım da var
        Intent intent = new Intent(ArtActivity.this,MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);    //başka açık activitylerin hepsini kapatır
        startActivity(intent);

    }

    public Bitmap makeSmallerImage(Bitmap image,int maximumSize){  //fotoğrafı küçültmek için (örneğin yataysa dikeye göre düzenlemek için )

        int width= image.getWidth();
        int height=image.getHeight();

        float bitmapRatio=(float) width/(float) height;   //kenarları böleriz 0 altında mı üstündemi?

        if(bitmapRatio>1){   //o zaman görsel yatay

            width =maximumSize;
            height=(int)(width/bitmapRatio);  //köşeden orantılı küçültmek için

        }else{  //dikey
            height=maximumSize;
            width=(int)(width*bitmapRatio);

        }
        return image.createScaledBitmap(image,width,height,true);
    }

    public void selectImage(View view){
        //galeriye erişmek için izin isteme
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){  //erişim izni yoksa verilmemişte
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,Manifest.permission.READ_EXTERNAL_STORAGE)){  //izin verilmeyince açıklama yağmak zorundamıyım
                //izin isterken detaylı bilgi
                Snackbar.make(view,"permission needed for gallery",Snackbar.LENGTH_INDEFINITE).setAction("Give permission", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);

                    }
                }).show();

            }else{  //zorunda değilsek direk izin isticez
                permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE);
            }
        }else{  //izin verilmişse galeriye git

            Intent intentToGallery=new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);  //galeriye git resim seç
            activityResultLauncher.launch(intentToGallery);

        }
    }

    private void registerLauncher(){

        activityResultLauncher=registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {

                if(result.getResultCode()==RESULT_OK){  //kullanıcı resim seçmiş mi
                    Intent intentFromResult= result.getData();
                    if(intentFromResult!=null){  //resim boş değilse
                        Uri imageData=intentFromResult.getData();
                        //binding.imageView.setImageURI(imageData);  //her zaman doğru olmaz

                        try {    //uygulamanın çökmesini engller try catch
                            if(Build.VERSION.SDK_INT>=28) {     //versiyon 28 ve üzerindeyse bu şekilde çalışır altı için farklı (bu methot 28 altı için hata vermiyosa kullanılabilir yeni geldi)
                                ImageDecoder.Source source = ImageDecoder.createSource(ArtActivity.this.getContentResolver(), imageData);
                                selectedImage = ImageDecoder.decodeBitmap(source);
                                binding.imageView.setImageBitmap(selectedImage);
                            }else{    //bu kullanım kalkıyor ama eğer 28 üstünde hata vermiyosas kullanılabilir
                                selectedImage=MediaStore.Images.Media.getBitmap(ArtActivity.this.getContentResolver(),imageData);
                                binding.imageView.setImageBitmap(selectedImage);
                            }
                        }catch (Exception e){
                            e.printStackTrace();  //logchat te gözükür hatalar

                        }
                    }
                }
            }
        });

        permissionLauncher=registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
            @Override
            public void onActivityResult(Boolean result) {
                if(result){   //result==true izin verilmişse
                    Intent intentToGallery=new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    activityResultLauncher.launch(intentToGallery);
                }
                else{
                    Toast.makeText(ArtActivity.this,"izin gereklli",Toast.LENGTH_LONG).show();
                }
            }
        });

    }

}