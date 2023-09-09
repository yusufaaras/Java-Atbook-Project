package com.eyusufaras.artbookproject;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.viewmodel.ViewModelFactoryDsl;
import androidx.recyclerview.widget.LinearLayoutManager;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.Bundle;
import android.text.Layout;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import com.eyusufaras.artbookproject.databinding.ActivityMainBinding;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    ArrayList<Art> artArrayList;
    ArtAdapter artAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        ConstraintLayout view = binding.getRoot();
        setContentView(view);

        artArrayList=new ArrayList<Art>();

        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        artAdapter=new ArtAdapter(artArrayList);
        binding.recyclerView.setAdapter(artAdapter);

        getdata();

    }

    private void getdata(){   //ilk sayfada eklenenleri göstermek için
        try{
            SQLiteDatabase sqLiteDatabase=this.openOrCreateDatabase("arts", MODE_PRIVATE,null);

            Cursor cursor= sqLiteDatabase.rawQuery("SELECT*FROM arts",null);
            int nameIx=cursor.getColumnIndex("artname");
            int idIx=cursor.getColumnIndex("id");

            while(cursor.moveToNext()){
                String name=cursor.getString(nameIx);
                int id=cursor.getInt(idIx);
                Art art=new Art(name,id);
                artArrayList.add(art);
            }
            artAdapter.notifyDataSetChanged();
            cursor.close();

        }catch(Exception e){
            e.printStackTrace();
        }
    }


    @Override        //menu yü activity e bağlama
    public boolean onCreateOptionsMenu(Menu menu) { //bağlama işlemi

        MenuInflater menuInflater=getMenuInflater();
        menuInflater.inflate(R.menu.art_menu,menu);

        return super.onCreateOptionsMenu(menu);
    }

    //menuye tıklanınca ne olacak
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if(item.getItemId()==R.id.add_art){
            Intent intent=new Intent(this,ArtActivity.class);    //tıklanınca artActivityi açmak için
            intent.putExtra("info","new");
            startActivity(intent);   //artActivity başlasın diye
        }

        return super.onOptionsItemSelected(item);
    }
}