package com.example.cracked;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RecipeActivity extends AppCompatActivity {

    TextView titleTextView;

    // test recipes
    // https://www.allrecipes.com/recipe/254970/fried-green-tomato-parmesan/
    // https://www.allrecipes.com/recipe/214500/sausage-peppers-onions-and-potato-bake/
    String websiteURL = "";
    Recipe newRecipe;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true); // helps to add the back button

        titleTextView = (TextView) findViewById(R.id.titleTextView);
        LinearLayout lLayout = (LinearLayout) findViewById(R.id.recipeLinearLayout);

        ArrayList<String> ingredients = new ArrayList<>();
        ArrayList<String> directions = new ArrayList<>();
        newRecipe = new Recipe();

        // grab website url
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            websiteURL = extras.getString("url");

            // FOR EASY TESTING DON'T ENTER A URL -- REMOVE LATER!!!!!!!!!
            if (websiteURL.equals("")) {
                websiteURL = "https://www.allrecipes.com/recipe/254970/fried-green-tomato-parmesan/";
            }
        }

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

                    // find recipe ingredients
                    Elements elements = document.getElementsByClass("ingredients-item-name elementFont__body");  //should find attribute to get recipe
                    for(Element element : elements) {
                        String ingredient = element.text();
                        ingredients.add(ingredient);
                        Log.d("YUH", ingredient);
                    }

                    // find recipe title
                    elements = document.getElementsByClass("headline heading-content elementFont__display");
                    for(Element element : elements) {
                        newRecipe.title = element.text();
                    }

                    // find recipe directions
                    elements = document.getElementsByClass("section-body elementFont__body--paragraphWithin elementFont__body--linkWithin");
                    for(Element element : elements) {
                        directions.add(element.text());
                    }

                    newRecipe.ingredients = ingredients;
                    newRecipe.directions = directions;
                }
                catch (IOException e) {
                    e.printStackTrace();
                }

                //postExecute
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        titleTextView.setText(newRecipe.title);

                        // ingredients header
                        final TextView ingredientsHeader = new TextView(RecipeActivity.this);
                        ingredientsHeader.setText("Ingredients");
                        ingredientsHeader.setPadding(0, 60,0, 0);
                        ingredientsHeader.setTextSize(18);
                        lLayout.addView(ingredientsHeader);

                        // ingredients list
                        for (int i = 0; i < ingredients.size(); i++) {
                            // create a new textview
                            final TextView rowTextView = new TextView(RecipeActivity.this);

                            // set some properties of rowTextView or something
                            rowTextView.setText(ingredients.get(i));
                            rowTextView.setPadding(0, 10, 0, 10);
                            rowTextView.setTextSize(16);
                            rowTextView.setTextColor(Color.BLACK);

                            // add the textview to the linearlayout
                            lLayout.addView(rowTextView);
                        }

                        final TextView directionsHeader = new TextView(RecipeActivity.this);
                        directionsHeader.setText("Directions");
                        directionsHeader.setPadding(0, 60,0, 0);
                        directionsHeader.setTextSize(18);
                        lLayout.addView(directionsHeader);

                        // directions list
                        for (int i = 0; i < directions.size(); i++) {
                            // create a new textview
                            final TextView rowTextView = new TextView(RecipeActivity.this);

                            // set some properties of rowTextView or something
                            rowTextView.setText(directions.get(i));
                            rowTextView.setPadding(0, 10, 0, 10);
                            rowTextView.setTextSize(16);
                            rowTextView.setTextColor(Color.BLACK);

                            // add the textview to the linearlayout
                            lLayout.addView(rowTextView);
                        }
                    }
                });
            }
        });

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
}