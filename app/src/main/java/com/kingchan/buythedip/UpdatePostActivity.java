package com.kingchan.buythedip;

import androidx.appcompat.app.AppCompatActivity;

import androidx.annotation.NonNull;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import de.hdodenhof.circleimageview.CircleImageView;

public class UpdatePostActivity extends AppCompatActivity {

    private EditText editCaption;
    private Button editButton;

    private String uCaption, uId;
    private String uPostImage;
    private CircleImageView circleImageView;
    private TextView mProfileName;

    private FirebaseFirestore db;

    // Crop images
    private Uri mImageUri = null;

    // Firebase Firestore
    private FirebaseAuth auth;
    private StorageReference storageReference;
    private FirebaseFirestore firestore;
    private String userId;

    // Post Image
    private ImageView postPicture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_post);

        db = FirebaseFirestore.getInstance();

        // Firestore
        storageReference = FirebaseStorage.getInstance().getReference();
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        userId = auth.getCurrentUser().getUid();

        circleImageView = findViewById(R.id.profile_pic);
        mProfileName = findViewById(R.id.username_tv);

        editButton = findViewById(R.id.edit_post_btn);
        editCaption = findViewById(R.id.caption_text);
        postPicture = findViewById(R.id.user_post);

        // Retrieve the profile data
        firestore.collection("Users").document(userId).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()){
                    if (task.getResult().exists()){
                        String name = task.getResult().getString("name");
                        String imageUrl = task.getResult().getString("image");
                        mProfileName.setText(name);
                        mImageUri = Uri.parse(imageUrl);

                        Glide.with(UpdatePostActivity.this).load(imageUrl).into(circleImageView);
                    }
                }
            }
        });

        // Get data from the bundles
        Bundle bundle = getIntent().getExtras();
        if (bundle != null){
            editButton.setText("Update");
             uCaption = bundle.getString("uCaption");
             uPostImage = bundle.getString("uPostImage");
             uId = bundle.getString("uId");

             // set the uCaption text to editCaption, so that it can see and edit
             editCaption.setText(uCaption);
             Glide.with(UpdatePostActivity.this).load(Uri.parse(uPostImage)).into(postPicture);
        }else{
            editButton.setText("Save");
        }

        editButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                String caption = editCaption.getText().toString();

                Bundle tmp_bundle = getIntent().getExtras();
                if(tmp_bundle != null){
                    String id = uId;
                    updateToFireStore(id, caption);
                }
            }
        });
    }

    // Update to firestore core function
    private void updateToFireStore(String id , String caption){

        db.collection("Posts").document(id).update("caption" , caption)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()){
                            Toast.makeText(UpdatePostActivity.this, "Data Updated!!", Toast.LENGTH_SHORT).show();
                            startActivity(new Intent());
                        }else{
                            Toast.makeText(UpdatePostActivity.this, "Error : " + task.getException().getMessage() , Toast.LENGTH_SHORT).show();
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(UpdatePostActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}