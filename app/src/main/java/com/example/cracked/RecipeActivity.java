package com.example.cracked;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.squareup.picasso.Picasso;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RecipeActivity extends AppCompatActivity implements View.OnClickListener{

    TextView titleTextView;
    ImageView imageView;
    LinearLayout lLayout;
    Toolbar toolbar;
    Button scaleButton;
    Button conversionButton;
    Button editButton;
    ArrayList<String> ingredients;

    // test recipes
    // https://www.allrecipes.com/recipe/254970/fried-green-tomato-parmesan/
    // https://www.allrecipes.com/recipe/214500/sausage-peppers-onions-and-potato-bake/
    String websiteURL = "";
    Recipe recipe;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe);

        toolbar = findViewById(R.id.toolbarView);
        titleTextView = findViewById(R.id.titleTextView);
        imageView = findViewById(R.id.imageView);
        lLayout = findViewById(R.id.recipeLinearLayout);
        scaleButton = findViewById(R.id.scaleButton);
        conversionButton = findViewById(R.id.conversionButton);
        editButton = findViewById(R.id.editButton);

        scaleButton.setOnClickListener(this);
        conversionButton.setOnClickListener(this);
        editButton.setOnClickListener(this);

        toolbar.setTitle("Recipe");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);


        recipe = new Recipe();

        // grab website url
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            websiteURL = extras.getString("url");

            // if websiteURL is empty, that means an imported recipe is being loaded instead
            if (websiteURL == null && extras.getString("title") != null) {
                recipe.title = extras.getString("title");
                recipe.imageURL = extras.getString("imageURL");
                recipe.ingredients = extras.getStringArrayList("ingredients");
                recipe.directions = extras.getStringArrayList("directions");
            }
        }

        // only run if websiteURL was provided
        if (websiteURL != null && websiteURL != "") {
            ExecutorService service = Executors.newSingleThreadExecutor();
            service.execute(new Runnable() {
                @Override
                public void run() {

                    //PreExecute
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //Put loading bar
                        }
                    });

                    //doInBackground
                    try {
                        Document document = Jsoup.connect(websiteURL).timeout(6000).get();

                        ingredients = new ArrayList<>();
                        ArrayList<String> directions = new ArrayList<>();

                        // find recipe ingredients
                        Elements elements = document.getElementsByClass("ingredients-item-name elementFont__body");  //should find attribute to get recipe
                        for (Element element : elements) {
                            String ingredient = element.text();
                            ingredients.add(ingredient);
                            Log.d("YUH", ingredient);
                        }

                        // find recipe title
                        elements = document.getElementsByClass("headline heading-content elementFont__display");
                        for (Element element : elements) {
                            recipe.title = element.text();
                        }

                        // find recipe directions
                        elements = document.getElementsByClass("section-body elementFont__body--paragraphWithin elementFont__body--linkWithin");
                        for (Element element : elements) {
                            directions.add(element.text());
                        }

                        // find the recipe image url
                        elements = document.getElementsByTag("meta");
                        for (Element element : elements) {
                            if (element.attr("property").equals("og:image")) {
                                recipe.imageURL = element.attr("content");
                            }
                        }

                        recipe.ingredients = ingredients;
                        recipe.directions = directions;
                        AddRecipeToDatabase();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    //postExecute
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            setUpUI();
                        }
                    });
                }
            });

            // setup UI from an already imported recipe
        } else {
            setUpUI();
        }

    }

    // menu items
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_add_to_cart) {
            AddToShoppingCart();
        } else if (id == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(
            Menu menu) {
        getMenuInflater().inflate(R.menu.menu_recipe, menu);
        return super.onCreateOptionsMenu(menu);
    }

    // add the recipe to firebase
    private void AddRecipeToDatabase() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> docData = new HashMap<>();
        docData.put("title", recipe.title);
        docData.put("ingredients", recipe.ingredients);
        docData.put("directions", recipe.directions);
        docData.put("imageURL", recipe.imageURL);
        docData.put("timestamp", new Date());
        db.collection("recipes")
                .add(docData)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        Toast.makeText(getApplicationContext(),
                                recipe.title + " has been imported",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getApplicationContext(),
                                "Uh oh! Something went wrong, the recipe could not be added :(",
                                Toast.LENGTH_SHORT).show();
                    }
                });

    }

    // add the recipe ingredients to the shopping cart on firebase
    private void AddToShoppingCart() {
        // Add each ingredient to shopping cart
        for (String ingredient : recipe.ingredients) {
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            Map<String, Object> docData = new HashMap<>();
            docData.put("name", ingredient);
            docData.put("isChecked", false);
            docData.put("timestamp", new Date());
            db.collection("shoppingCart")
                    .add(docData)
                    .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                        @Override
                        public void onSuccess(DocumentReference documentReference) {
                            Toast.makeText(getApplicationContext(),
                                    "Ingredients to " + recipe.title + " have been added to the shopping cart",
                                    Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(getApplicationContext(),
                                    "Uh oh! Something went wrong, the recipe could not be added to the shopping cart:(",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private void setUpUI() {
        titleTextView.setText(this.recipe.title);
        Picasso.get().load(this.recipe.imageURL).into(imageView);

        // ingredients header
        final TextView ingredientsHeader = new TextView(RecipeActivity.this);
        ingredientsHeader.setText("Ingredients");
        ingredientsHeader.setPadding(0, 60, 0, 0);
        ingredientsHeader.setTextSize(18);
        lLayout.addView(ingredientsHeader);

        // ingredients list
        for (int i = 0; i < recipe.ingredients.size(); i++) {
            // create a new textview
            final TextView rowTextView = new TextView(RecipeActivity.this);

            // set some properties of rowTextView or something
            rowTextView.setText(recipe.ingredients.get(i));
            rowTextView.setPadding(0, 10, 0, 10);
            rowTextView.setTextSize(16);
            rowTextView.setTextColor(Color.BLACK);

            // add the textview to the linearlayout
            lLayout.addView(rowTextView);
        }

        final TextView directionsHeader = new TextView(RecipeActivity.this);
        directionsHeader.setText("Directions");
        directionsHeader.setPadding(0, 60, 0, 0);
        directionsHeader.setTextSize(18);
        lLayout.addView(directionsHeader);

        // directions list
        for (int i = 0; i < recipe.directions.size(); i++) {
            // create a new textview
            final TextView rowTextView = new TextView(RecipeActivity.this);

            // set some properties of rowTextView or something
            rowTextView.setText(i + 1 + ". " + recipe.directions.get(i));
            rowTextView.setPadding(0, 10, 0, 10);
            rowTextView.setTextSize(16);
            rowTextView.setTextColor(Color.BLACK);

            // add the textview to the linearlayout
            lLayout.addView(rowTextView);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.editButton: {

            }
            case R.id.conversionButton: {
//                for (int i = 0; i < ingredients.size(); ++i) {
//                    String temp = ingredients.indexOf(i);
//                }
            }
            case R.id.scaleButton: {

            }

        }
    }
}