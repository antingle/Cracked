package com.example.cracked;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ingredientListFragment extends Fragment {
    ListView listView;
    String ingredient = "";
    String websiteURL = "https://www.allrecipes.com/recipe/254970/fried-green-tomato-parmesan/";

    public ingredientListFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView ( LayoutInflater inflater , ViewGroup container, Bundle savedInstanceState){
        View fragmentRoot=inflater.inflate(R.layout.fragment_layout,container,false);
        listView = fragmentRoot.findViewById(R.id.ingredientListView);

        LinkedList<String> ingredients = new LinkedList<>();

        ExecutorService service = Executors.newSingleThreadExecutor();
        service.execute(new Runnable() {
            @Override
            public void run() {

                //PreExecute
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //Put loading bar
                    }
                });

                //doInBackground
                try {
                    Document document = Jsoup.connect(websiteURL).timeout(6000).get();
                    Elements ele = document.getElementsByClass("ingredients-item-name elementFont__body");  //should find attribute to get recipe
                    for(Element element : ele) {
                        ingredient = element.text();
                        ingredients.add(ingredient);
                        Log.d("YUH", ingredient);
                    }
                }
                catch (IOException e) {
                    e.printStackTrace();
                }

                //postExecute
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ArrayAdapter<String> adapter =  new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, ingredients);
                        listView.setAdapter(adapter);
                    }
                });
            }
        });

        return fragmentRoot;
    }
}
