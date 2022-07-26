package com.example.cracked;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;

import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
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
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    Button importButton;
    EditText editTextRecipeURL;
    public RecyclerView recyclerView;
    ArrayList<Recipe> recipeList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        importButton = (Button) findViewById(R.id.importButton);
        editTextRecipeURL = (EditText) findViewById(R.id.editTextRecipeURL);
        recyclerView = (RecyclerView) findViewById(R.id.recyclerView);

        importButton.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    String importURL = editTextRecipeURL.getText().toString();
                    if (importURL == "") {
                        Toast.makeText(getApplicationContext(), "Please enter a URL", Toast.LENGTH_SHORT).show();
                        return;
                    } else if (!urlIsValid(importURL)) {
                        Toast.makeText(getApplicationContext(), "Please enter a valid URL", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    editTextRecipeURL.setText(""); // clear the edit text
                    Intent intent = new Intent(MainActivity.this, RecipeActivity.class);
                    Bundle bundle = new Bundle();
                    bundle.putString("url", importURL);
                    intent.putExtras(bundle);
                    startActivity(intent);
                }
        });

        // listen for firebase updates
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("recipes")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Toast.makeText(getApplicationContext(), "Cannot reach Firebase", Toast.LENGTH_LONG).show();
                            return;
                        }

                        recipeList = new ArrayList<Recipe>();
                        for (QueryDocumentSnapshot doc : value) {
                            Recipe recipe = new Recipe(
                                    doc.getString("title"),
                                    doc.getString("imageURL"),
                                    (ArrayList<String>) doc.getData().get("ingredients"),
                                    (ArrayList<String>) doc.getData().get("directions"));
                            recipeList.add(recipe);
                        }
                        setupRecyclerView();
                    }
                });
    }

    protected void setupRecyclerView() {
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(getApplicationContext());
        recyclerView.setLayoutManager(llm);
        RVAdapter adapter = new RVAdapter(recipeList);
        recyclerView.setAdapter(adapter);
    }

    // test if url is valid
    public static boolean urlIsValid(String url)
    {
        /* Try creating a valid URL */
        try {
            new URL(url).toURI();
            return true;
        }

        // If there was an Exception
        // while creating URL object
        catch (Exception e) {
            return false;
        }
    }
}
