package com.example.cracked;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;

public class EditRecipeActivity extends AppCompatActivity {

    Toolbar toolbar;
    Recipe recipe;
    ImageView imageView;
    EditText editTextRecipeTitle;
    EditText editTextIngredients;
    EditText editTextDirections;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_recipe);

        imageView = findViewById(R.id.imageView);
        editTextRecipeTitle = findViewById(R.id.editTextRecipeTitle);
        editTextIngredients = findViewById(R.id.editTextIngredients);
        editTextDirections = findViewById(R.id.editTextDirections);
        toolbar = findViewById(R.id.toolbarView);
        toolbar.setTitle("Edit Recipe");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        // retrieve recipe to edit from intent
        recipe = new Recipe();
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            recipe.id = extras.getString("id");
            recipe.title = extras.getString("title");
            recipe.imageURL = extras.getString("imageURL");
            recipe.ingredients = extras.getStringArrayList("ingredients");
            recipe.directions = extras.getStringArrayList("directions");
        }

        setUpUI();
    }

    // menu items
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_done) {
            recipe.title = editTextRecipeTitle.getText().toString();
            recipe.ingredients = parseBulletList(editTextIngredients.getText().toString());
            recipe.directions = parseBulletList(editTextDirections.getText().toString());

            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("recipes").document(recipe.id)
                    .update("title", recipe.title,
                            "timestamp", new Date(),
                            "ingredients", recipe.ingredients,
                            "directions", recipe.directions)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Toast.makeText(getApplicationContext(),
                                    recipe.title + " has been updated",
                                    Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(getApplicationContext(),
                                    "Error updating recipe",
                                    Toast.LENGTH_SHORT).show();
                            Log.e("EditRecipe", "Error updating recipe", e);
                        }
                    });
        } else if (id == android.R.id.home) {
            // confirm if user wants to discard their changes
            showCancelDialog();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(
            Menu menu) {
        getMenuInflater().inflate(R.menu.menu_edit, menu);
        return super.onCreateOptionsMenu(menu);
    }

    protected void setUpUI() {
        Picasso.get().load(this.recipe.imageURL).into(imageView);
        // setup view from recipe
        editTextRecipeTitle.setText(recipe.title);

        // ingredients
        String ingredientsString = "";
        for (int i = 0; i < recipe.ingredients.size(); ++i) {
            String direction = "\n◎ " + recipe.ingredients.get(i);
            if (i == 0) {
                direction = "◎ " + recipe.ingredients.get(i);
            }
            ingredientsString += direction;
        }
        editTextIngredients.setText(ingredientsString);

        // directions
        String directionsString = "";
        for (int i = 0; i < recipe.directions.size(); ++i) {
            String direction = "\n◎ " + recipe.directions.get(i);
            if (i == 0) {
                direction = "◎ " + recipe.directions.get(i);
            }
            directionsString += direction;
        }
        editTextDirections.setText(directionsString);

        editTextIngredients.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable e) {
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
                if (lengthAfter > lengthBefore) {
                    if (text.toString().length() == 1) {
                        text = "◎ " + text;
                        editTextIngredients.setText(text);
                        editTextIngredients.setSelection(editTextIngredients.getText().length());
                    }
                    if (text.toString().endsWith("\n")) {
                        text = text.toString().replace("\n", "\n◎ ");
                        text = text.toString().replace("◎ ◎", "◎");
                        editTextIngredients.setText(text);
                        editTextIngredients.setSelection(editTextIngredients.getText().length());
                    }
                }
            }
        });

        editTextDirections.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable e) {
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) {
            }

            @Override
            public void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
                if (lengthAfter > lengthBefore) {
                    if (text.toString().length() == 1) {
                        text = "◎ " + text;
                        editTextDirections.setText(text);
                        editTextDirections.setSelection(editTextDirections.getText().length());
                    }
                    if (text.toString().endsWith("\n")) {
                        text = text.toString().replace("\n", "\n◎ ");
                        text = text.toString().replace("◎ ◎", "◎");
                        editTextDirections.setText(text);
                        editTextDirections.setSelection(editTextDirections.getText().length());
                    }
                }
            }
        });
    }

    // parses the bullet list from the edit text to a string array list for the database
    protected ArrayList<String> parseBulletList(String string) {
        ArrayList<String> list = new ArrayList<>();
        BufferedReader bufReader = new BufferedReader(new StringReader(string));

        try {

            String line = null;
            while ((line = bufReader.readLine()) != null) {
                String listItem = line.substring(1).trim();
                Log.d("STINRGLINE", listItem);
                if (!listItem.equals(""))
                    list.add(listItem);
            }
        } catch (IOException e) {
        }


        return list;
    }

    protected void showCancelDialog() {
        // Use the Builder class for convenient dialog construction
        new AlertDialog.Builder(EditRecipeActivity.this)
                .setTitle("Discard changes")
                .setMessage("Are you sure you want to discard the changes to this recipe?")
                .setPositiveButton("Discard", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}