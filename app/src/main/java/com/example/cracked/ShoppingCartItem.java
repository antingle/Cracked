package com.example.cracked;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;

public class ShoppingCartItem {
    String id;
    String name;
    Boolean isChecked;

    public ShoppingCartItem(String id, String name, Boolean isChecked) {
        this.id = id;
        this.name = name;
        this.isChecked = isChecked;
    }

    public String toString() {
        return name;
    }

    public void setChecked(Boolean checked) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("shoppingCart").document(id)
                .update("isChecked", checked)
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w("ShoppingCartItem", "Error updating document", e);
                    }
                });
        isChecked = checked;
    }

}
