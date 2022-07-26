package com.example.cracked;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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

public class RecipeActivity extends AppCompatActivity {

    TextView titleTextView;
    ImageView imageView;
    LinearLayout lLayout;

    // test recipes
    // https://www.allrecipes.com/recipe/254970/fried-green-tomato-parmesan/
    // https://www.allrecipes.com/recipe/214500/sausage-peppers-onions-and-potato-bake/
    String websiteURL = "";
    Recipe recipe;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); // helps to add the back button

        titleTextView = (TextView) findViewById(R.id.titleTextView);
        imageView = (ImageView) findViewById(R.id.imageView);
        lLayout = (LinearLayout) findViewById(R.id.recipeLinearLayout);

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

                        ArrayList<String> ingredients = new ArrayList<>();
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
                        AddRecipeToDatabase(recipe);
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

    // add the back button
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    // add the recipe to firebase
    private void AddRecipeToDatabase(Recipe recipe) {
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
                                Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getApplicationContext(),
                                "Uh oh! Something went wrong, the recipe could not be added :(",
                                Toast.LENGTH_LONG).show();
                    }
                });

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
                rowTextView.setText(recipe.directions.get(i));
                rowTextView.setPadding(0, 10, 0, 10);
                rowTextView.setTextSize(16);
                rowTextView.setTextColor(Color.BLACK);

                // add the textview to the linearlayout
                lLayout.addView(rowTextView);

        }
    }
}