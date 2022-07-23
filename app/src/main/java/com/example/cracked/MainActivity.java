package com.example.cracked;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.app.ActionBar;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.material.navigation.NavigationView;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    Button importButton;
    EditText editTextRecipeURL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        importButton = (Button) findViewById(R.id.importButton);
        editTextRecipeURL = (EditText) findViewById(R.id.editTextRecipeURL);

        importButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Intent intent = new Intent(MainActivity.this, RecipeActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putString("url", editTextRecipeURL.getText().toString());
                    intent.putExtras(bundle);
                    startActivity(intent);
                }
        });
    }
}
