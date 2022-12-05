package com.kingchan.buythedip;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import androidx.annotation.NonNull;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class UpdatePostActivity extends AppCompatActivity {

    private EditText editCaption;
    private Button editButton;

    private String uCaption, uId;
    private String uPostImage;
    private CircleImageView circleImageView;
    private TextView mProfileName;
    private Uri postImageUri = null;
    // Post Image
    private ImageView postPicture;

    //private FirebaseFirestore db;

    // Crop images
    private Uri mImageUri = null;

    // Firebase Firestore
    private FirebaseAuth auth;
    private StorageReference storageReference;
    private FirebaseFirestore firestore;
    private String userId;

    private final long lastClickTime = 1000;

    // Store Spinner
    private Spinner storeSpinner;
    String[] store = {"Walmart", "Superstore", "No frill"};
    private String selectedStore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_update_post);

        // Store Spinner
        storeSpinner = findViewById(R.id.store_spinner);

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

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(UpdatePostActivity.this, android.R.layout.simple_spinner_item, store);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        storeSpinner.setAdapter(adapter);

        storeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedStore = parent.getItemAtPosition(position).toString();
                Toast.makeText(UpdatePostActivity.this, selectedStore, Toast.LENGTH_SHORT).show();

            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

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

        // Trigger CropImage when clicking the picture
        postPicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Picking image from the photo gallery
                CropImage.activity()
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .setAspectRatio(3,2)
                        .setMinCropResultSize(512,512)
                        .start(UpdatePostActivity.this);
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
                // preventing double, using threshold of 1000 ms
                if (SystemClock.elapsedRealtime() - lastClickTime < 1000){
                    return;
                }

                // Get the changed edited text
                String caption = editCaption.getText().toString();
                selectedStore = storeSpinner.getSelectedItem().toString();
                //postImageUri = Uri.parse(uPostImage);

                if (postImageUri != null){
                    // Use storage reference to make the post reference
                    StorageReference postRef = storageReference.child("post_images").child(FieldValue.serverTimestamp().toString() + ".jpg");

                    // Put the postImageUri to the post reference
                    postRef.putFile(postImageUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                            if(task.isSuccessful()){
                                postRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>(){
                                    @Override
                                    public void onSuccess(Uri uri){
                                        // making a Hash Map called postMap, adding all information there
                                        HashMap<String , Object> postMap = new HashMap<>();
                                        postMap.put("image" , uri.toString());
                                        postMap.put("user" , userId);
                                        postMap.put("caption" , caption);
                                        postMap.put("store", selectedStore);
                                        postMap.put("time" , FieldValue.serverTimestamp());

                                        // Put the postMap to the firestore.collection.add method, to the Posts table
                                        firestore.collection("Posts").document(uId).update(postMap).addOnCompleteListener(new OnCompleteListener<Void>() {
                                            @Override
                                            public void onComplete(@NonNull Task<Void> task) {
                                                // If complete and successful
                                                if (task.isSuccessful()){
                                                    // Show successful text
                                                    Toast.makeText(UpdatePostActivity.this, "Post Updated Successfully !!", Toast.LENGTH_SHORT).show();
                                                    // Go back to MainActivity
                                                    startActivity(new Intent(UpdatePostActivity.this , MainActivity.class));
                                                    // finish all storage
                                                    finish();
                                                }else{
                                                    Toast.makeText(UpdatePostActivity.this, task.getException().toString() , Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        });

                                    }
                                });
                            }
                        }
                    });
                }
            }
        });
    }

    // Set the postImageUri and the postPicture
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE){
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK){

                postImageUri = result.getUri();
                postPicture.setImageURI(postImageUri);
            }else if(resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE){
                Toast.makeText(this, result.getError().toString(), Toast.LENGTH_SHORT).show();
            }
        }
    }
}