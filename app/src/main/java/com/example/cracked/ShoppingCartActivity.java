package com.example.cracked;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;

public class ShoppingCartActivity extends AppCompatActivity {

    Toolbar toolbar;
    ListView shoppingCartListView;
    ArrayAdapter<ShoppingCartItem> adapter;

    ArrayList<ShoppingCartItem> shoppingCartItems;
    FirebaseFirestore db;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shopping_cart);

        // toolbar
        toolbar = findViewById(R.id.toolbarView);
        toolbar.setTitle("Shopping Cart");
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowHomeEnabled(true);

        // listview
        shoppingCartListView = findViewById(R.id.shoppingCartListView);
        shoppingCartListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        shoppingCartListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                CheckedTextView v = (CheckedTextView) view;
                boolean currentCheck = v.isChecked();
                shoppingCartItems.get(position).setChecked(currentCheck);
            }
        });
        shoppingCartItems = new ArrayList<ShoppingCartItem>();


        // listen for firebase updates
        db = FirebaseFirestore.getInstance();
        db.collection("shoppingCart")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot value,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Toast.makeText(getApplicationContext(), "Cannot reach Firebase", Toast.LENGTH_LONG).show();
                            return;
                        }

                        for (DocumentChange dc : value.getDocumentChanges()) {
                            QueryDocumentSnapshot doc = dc.getDocument();
                            switch (dc.getType()) {
                                case ADDED:
                                    ShoppingCartItem shoppingCartItem = new ShoppingCartItem(
                                            doc.getId(),
                                            doc.getString("name"),
                                            doc.getBoolean("isChecked"));
                                    shoppingCartItems.add(shoppingCartItem);
                                    break;
                            }
                        }
                        setShoppingCartItems();
                    }
                });
    }

    protected void setShoppingCartItems() {
        adapter = new ArrayAdapter<ShoppingCartItem>(this, android.R.layout.simple_list_item_multiple_choice, shoppingCartItems);
        shoppingCartListView.setAdapter(adapter);
        for (int i = 0; i < shoppingCartItems.size(); i++) {
            shoppingCartListView.setItemChecked(i, shoppingCartItems.get(i).isChecked);
        }
    }

    protected void clearCartItems(Boolean allItems) {
        for (ShoppingCartItem item : shoppingCartItems) {
            if (allItems || item.isChecked) {
                db.collection("shoppingCart").document(item.id)
                        .delete()
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                shoppingCartItems.remove(item);
                                shoppingCartListView.invalidate(); // reset adapter after clearing items
                                setShoppingCartItems();
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w("ShoppingCartItem", "Error deleting document", e);
                            }
                        });
            }
        }
    }

    // menu items
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_clear) {
            clearCartItems(false);
            Toast.makeText(getApplicationContext(), "Only checked items have been removed", Toast.LENGTH_SHORT).show();
        } else if (id == R.id.action_clear_all) {
            clearCartItems(true);
            Toast.makeText(getApplicationContext(), "All items have been removed", Toast.LENGTH_SHORT).show();
        } else if (id == android.R.id.home) {
            finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(
            Menu menu) {
        getMenuInflater().inflate(R.menu.menu_cart, menu);
        return super.onCreateOptionsMenu(menu);
    }
}
