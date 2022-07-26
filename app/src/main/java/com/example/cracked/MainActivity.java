package com.example.cracked;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import java.net.URL;
import java.util.ArrayList;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

public class MainActivity extends AppCompatActivity {

    Button importButton;
    EditText editTextRecipeURL;
    RecyclerView recyclerView;
    Toolbar toolbar;

    ArrayList<Recipe> recipeList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbarView);
        importButton = findViewById(R.id.importButton);
        editTextRecipeURL = findViewById(R.id.editTextRecipeURL);
        recyclerView = findViewById(R.id.recyclerView);

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

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
                            Recipe recipe = new Recipe(doc.getId(),
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

    @Override
    public boolean onCreateOptionsMenu(
            Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_cart) {
            Intent intent = new Intent(MainActivity.this, ShoppingCartActivity.class);
            startActivity(intent);
        } else if (id == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
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
