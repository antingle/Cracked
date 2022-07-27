package com.example.cracked;

import static java.lang.Character.isDigit;
import static java.lang.Character.isWhitespace;

import android.content.Intent;
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
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
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

public class RecipeActivity extends AppCompatActivity implements View.OnClickListener {

    TextView titleTextView;
    ImageView imageView;
    LinearLayout lLayout;
    Toolbar toolbar;
    Button scaleButton;
    Button conversionButton;
    Button editButton;
    ArrayList<String> ingredients;
    ArrayList<String> metricIngredients;
    Boolean isMetric = false;

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
        metricIngredients = new ArrayList<>();
        ingredients = new ArrayList<>();

        // grab website url
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            websiteURL = extras.getString("url");

            // if websiteURL is empty, that means an imported recipe is being loaded instead
            if (websiteURL == null && extras.getString("title") != null) {
                recipe.id = extras.getString("id");
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                db.collection("recipes")
                        .document(recipe.id)
                        .addSnapshotListener(new EventListener<DocumentSnapshot>() {
                            @Override
                            public void onEvent(@Nullable DocumentSnapshot snapshot,
                                                @Nullable FirebaseFirestoreException e) {
                                if (e != null) {
                                    Log.w("RecipeActivity", "Listen failed.", e);
                                    return;
                                }

                                String source = snapshot != null && snapshot.getMetadata().hasPendingWrites()
                                        ? "Local" : "Server";

                                if (snapshot != null && snapshot.exists()) {
                                    recipe.title = snapshot.getString("title");
                                    recipe.imageURL = snapshot.getString("imageURL");
                                    recipe.ingredients = (ArrayList<String>) snapshot.get("ingredients");
                                    recipe.directions = (ArrayList<String>) snapshot.get("directions");
                                    setUpUI();
                                } else {
                                    Log.d("RecipeActivity", source + " data: null");
                                }
                            }
                        });
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
        lLayout.removeAllViews();

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
                // start Edit Activity
                Intent intent = new Intent(getApplicationContext(), EditRecipeActivity.class);
                Bundle bundle = new Bundle();
                bundle.putString("id", recipe.id);
                bundle.putString("title", recipe.title);
                bundle.putString("imageURL", recipe.imageURL);
                bundle.putStringArrayList("ingredients", recipe.ingredients);
                bundle.putStringArrayList("directions", recipe.directions);
                intent.putExtras(bundle);
                startActivity(intent);
                break;
            }
            case R.id.conversionButton: {
                imperialToMetric();
                setUpUI();
                break;
            }
            case R.id.scaleButton: {
                String buttonText = (String) scaleButton.getText();

                if (buttonText.equals("1X")) {
                    scaleButton.setText("2X");
                    for (int i = 0; i < recipe.ingredients.size(); ++i) {
                        if ((!isDigit(recipe.ingredients.get(i).charAt(0))) && !isFraction(recipe.ingredients.get(i).charAt(0))) {
                            continue;
                        }
                        StringBuilder tempVal = new StringBuilder();
                        StringBuilder tempIngredients = new StringBuilder(recipe.ingredients.get(i));
                        double tempValDub = 0;
                        if (isFraction(recipe.ingredients.get(i).charAt(0))) {
                            for (int k = 0; k < recipe.ingredients.get(i).length(); ++k) {
                                if (!isWhitespace(recipe.ingredients.get(i).charAt(k))) {
                                    tempVal.append(recipe.ingredients.get(i).charAt(k));
                                } else {
                                    tempIngredients.delete(0, k);
                                    recipe.ingredients.set(i, tempIngredients.toString());
                                    break;
                                }
                            }
                            tempValDub = calcFraction(tempVal.toString().charAt(0));
                        } else {
                            for (int k = 0; k < recipe.ingredients.get(i).length(); ++k) {
                                if (!isWhitespace(recipe.ingredients.get(i).charAt(k))) {
                                    tempVal.append(recipe.ingredients.get(i).charAt(k));
                                } else {
                                    tempIngredients.delete(0, k);
                                    recipe.ingredients.set(i, tempIngredients.toString());
                                    break;
                                }
                            }
                            tempValDub = Double.parseDouble(tempVal.toString());
                        }
                        tempValDub *= 2;
                        if (tempValDub == Math.round(tempValDub)) {
                            int intVal = (int) tempValDub;
                            recipe.ingredients.set(i, String.valueOf(intVal) + recipe.ingredients.get(i));

                        } else {
                            recipe.ingredients.set(i, String.valueOf(tempValDub) + recipe.ingredients.get(i));
                        }
                    }
                } else if (buttonText.equals("2X")) {
                    scaleButton.setText("3X");
                    for (int i = 0; i < recipe.ingredients.size(); ++i) {
                        if ((!isDigit(recipe.ingredients.get(i).charAt(0))) && !isFraction(recipe.ingredients.get(i).charAt(0))) {
                            continue;
                        }
                        StringBuilder tempVal = new StringBuilder();
                        StringBuilder tempIngredients = new StringBuilder(recipe.ingredients.get(i));
                        double tempValDub = 0;
                        if (isFraction(recipe.ingredients.get(i).charAt(0))) {
                            for (int k = 0; k < recipe.ingredients.get(i).length(); ++k) {
                                if (!isWhitespace(recipe.ingredients.get(i).charAt(k))) {
                                    tempVal.append(recipe.ingredients.get(i).charAt(k));
                                } else {
                                    tempIngredients.delete(0, k);
                                    recipe.ingredients.set(i, tempIngredients.toString());
                                    break;
                                }
                            }
                            tempValDub = calcFraction(tempVal.toString().charAt(0));
                        } else {
                            for (int k = 0; k < recipe.ingredients.get(i).length(); ++k) {
                                if (!isWhitespace(recipe.ingredients.get(i).charAt(k))) {
                                    tempVal.append(recipe.ingredients.get(i).charAt(k));
                                } else {
                                    tempIngredients.delete(0, k);
                                    recipe.ingredients.set(i, tempIngredients.toString());
                                    break;
                                }
                            }
                            tempValDub = Double.parseDouble(tempVal.toString());
                        }
                        tempValDub *= 1.5;
                        if (tempValDub == Math.round(tempValDub)) {
                            int intVal = (int) tempValDub;
                            recipe.ingredients.set(i, String.valueOf(intVal) + recipe.ingredients.get(i));

                        } else {
                            recipe.ingredients.set(i, String.valueOf(tempValDub) + recipe.ingredients.get(i));
                        }
                    }
                } else if (buttonText.equals("3X")) {
                    scaleButton.setText("1X");
                    for (int i = 0; i < recipe.ingredients.size(); ++i) {
                        if ((!isDigit(recipe.ingredients.get(i).charAt(0))) && !isFraction(recipe.ingredients.get(i).charAt(0))) {
                            continue;
                        }
                        StringBuilder tempVal = new StringBuilder();
                        StringBuilder tempIngredients = new StringBuilder(recipe.ingredients.get(i));
                        double tempValDub = 0;
                        if (isFraction(recipe.ingredients.get(i).charAt(0))) {
                            for (int k = 0; k < recipe.ingredients.get(i).length(); ++k) {
                                if (!isWhitespace(recipe.ingredients.get(i).charAt(k))) {
                                    tempVal.append(recipe.ingredients.get(i).charAt(k));
                                } else {
                                    tempIngredients.delete(0, k);
                                    recipe.ingredients.set(i, tempIngredients.toString());
                                    break;
                                }
                            }
                            tempValDub = calcFraction(tempVal.toString().charAt(0));
                        } else {
                            for (int k = 0; k < recipe.ingredients.get(i).length(); ++k) {
                                if (!isWhitespace(recipe.ingredients.get(i).charAt(k))) {
                                    tempVal.append(recipe.ingredients.get(i).charAt(k));
                                } else {
                                    tempIngredients.delete(0, k);
                                    recipe.ingredients.set(i, tempIngredients.toString());
                                    break;
                                }
                            }
                            tempValDub = Double.parseDouble(tempVal.toString());
                        }
                        tempValDub /= 3;
                        if (tempValDub == Math.round(tempValDub)) {
                            int intVal = (int) tempValDub;
                            recipe.ingredients.set(i, String.valueOf(intVal) + recipe.ingredients.get(i));

                        } else {
                            recipe.ingredients.set(i, String.valueOf(tempValDub) + recipe.ingredients.get(i));
                        }
                    }
                }
                setUpUI();
                break;
            }
        }
    }

    public double calcFraction(char input) {
        switch (input) {
            case '¼': {
                return 0.25;
            }
            case '⅓': {
                return 0.33;
            }
            case '½': {
                return 0.5;
            }
            case '⅔': {
                return 0.66;
            }
            case '¾': {
                return .75;
            }
            default: {
                return 0;
            }
        }
    }

    public boolean isFraction(char input) {
        switch (input) {
            case '¼':
            case '⅓':
            case '½':
            case '⅔':
            case '¾': {
                return true;
            }
            default: {
                return false;
            }
        }
    }

    public void imperialToMetric() {
        isMetric = !isMetric;
        if (!metricIngredients.isEmpty())
        {
            if (isMetric) {
                conversionButton.setText("Metric");
                recipe.ingredients = metricIngredients;
            } else {
                conversionButton.setText("Imperial");
                recipe.ingredients = ingredients;
            }
            setUpUI();
            return;
        }

        conversionButton.setText("Metric");
        for (String ingredient : recipe.ingredients) {
            metricIngredients.add(ingredient);
            ingredients.add(ingredient);
        }

        for (int i = 0; i < recipe.ingredients.size(); ++i) {
            int whitespace = 0;
            StringBuilder num = new StringBuilder();
            StringBuilder temp = new StringBuilder();
            temp.append(recipe.ingredients.get(i));

            if ((!isDigit(recipe.ingredients.get(i).charAt(0))) && !isFraction(recipe.ingredients.get(i).charAt(0))) {
                continue;
            }
            StringBuilder tempVal = new StringBuilder();
            StringBuilder tempIngredients = new StringBuilder(recipe.ingredients.get(i));
            double tempValDub = 0;
            if (isFraction(recipe.ingredients.get(i).charAt(0))) {
                for (int k = 0; k < recipe.ingredients.get(i).length(); ++k) {
                    if (!isWhitespace(recipe.ingredients.get(i).charAt(k))) {
                        tempVal.append(recipe.ingredients.get(i).charAt(k));
                    } else {
                        tempIngredients.delete(0, k);
                        recipe.ingredients.set(i, tempIngredients.toString());
                        break;
                    }
                }
                tempValDub = calcFraction(tempVal.toString().charAt(0));
            }
                else{
                    for (int k = 0; k < recipe.ingredients.get(i).length(); ++k) {
                        if (isWhitespace(recipe.ingredients.get(i).charAt(k))) {
                            ++whitespace;
                        }
                        if (whitespace == 0) {
                            num.append(recipe.ingredients.get(i).charAt(k));
                            temp.delete(0, k);
                        }
                    }
                recipe.ingredients.set(i, temp.toString());
                String Number = num.toString();
               tempValDub = Double.parseDouble(Number);
                }



            if ((recipe.ingredients.get(i).contains("cups"))){
                tempValDub = tempValDub * 128;
                metricIngredients.set(i, tempValDub + (recipe.ingredients.get(i).replaceAll("cups", "grams")));
            }
            else if ((recipe.ingredients.get(i).contains("tablespoons"))){
                tempValDub = tempValDub * 14.3;
                metricIngredients.set(i, tempValDub + (recipe.ingredients.get(i).replaceAll("tablespoons", "grams")));
            }
            else if ((recipe.ingredients.get(i).contains("teaspoons"))) {
                tempValDub = tempValDub * 5.69;
                metricIngredients.set(i, tempValDub + (recipe.ingredients.get(i).replaceAll("teaspoons", "grams")));
            }
            else if ((recipe.ingredients.get(i).contains("cup"))){
                tempValDub = tempValDub * 128;
                metricIngredients.set(i, tempValDub + (recipe.ingredients.get(i).replaceAll("cup", "grams")));
            }
            else if ((recipe.ingredients.get(i).contains("tablespoon"))){
                tempValDub = tempValDub * 14.3;
                metricIngredients.set(i, tempValDub + (recipe.ingredients.get(i).replaceAll("tablespoon", "grams")));
            }
            else if ((recipe.ingredients.get(i).contains("teaspoon"))){
                tempValDub = tempValDub * 5.69;
                metricIngredients.set(i, tempValDub + (recipe.ingredients.get(i).replaceAll("teaspoon", "grams")));
            }
        }
        recipe.ingredients = metricIngredients;
        setUpUI();

    }


}