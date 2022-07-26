package com.example.cracked;

import java.net.URL;
import java.util.ArrayList;

public class Recipe {
    String title;
    String imageURL;
    ArrayList<String> ingredients;
    ArrayList<String> directions;

    public Recipe() {

    }

    public Recipe(String title, String imageURL, ArrayList<String> ingredients, ArrayList<String> directions) {
        this.title = title;
        this.imageURL = imageURL;
        this.ingredients = ingredients;
        this.directions = directions;
    }
}
