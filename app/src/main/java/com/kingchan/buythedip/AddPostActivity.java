package com.kingchan.buythedip;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.HashMap;

public class AddPostActivity extends AppCompatActivity {

    private Button mAddPostBtn;
    private EditText mCaptionText;
    private ImageView mPostImage;
    private ProgressBar mProgressBar;
    private Uri postImageUri = null;
    private StorageReference storageReference;
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;
    private String currentUserId;
    private Toolbar postToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_post);

        // Retrieve the components of the layout
        mAddPostBtn = findViewById(R.id.save_post_btn);
        mCaptionText = findViewById(R.id.caption_text);
        mPostImage = findViewById(R.id.post_image);
        mProgressBar = findViewById(R.id.post_progressBar);
        mProgressBar.setVisibility(View.INVISIBLE);
        postToolbar = findViewById(R.id.addpost_toolbar);


        getSupportActionBar().setTitle("Add Post");

        // Initialize the firestore, Authentication, and use the authen to retrieve user ID
        storageReference = FirebaseStorage.getInstance().getReference();
        firestore = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        currentUserId = auth.getCurrentUser().getUid();

        // When click the image, trigger Image Cropper
        mPostImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Picking image from the photo gallery
                CropImage.activity()
                        .setGuidelines(CropImageView.Guidelines.ON)
                        .setAspectRatio(3,2)
                        .setMinCropResultSize(512,512)
                        .start(AddPostActivity.this);
            }
        });

        // Setup when the Add Post button on click
        mAddPostBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Show Progress Bar
                mProgressBar.setVisibility(View.VISIBLE);
                // Set the Caption Text as a variable
                String caption = mCaptionText.getText().toString();

                // If caption is not empty and image is not null
                if (!caption.isEmpty() && postImageUri !=null){
                    // Use storage reference to make the post reference
                    StorageReference postRef = storageReference.child("post_images").child(FieldValue.serverTimestamp().toString() + ".jpg");

                    // Put the postImageUri to the post reference
                    postRef.putFile(postImageUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                        // If it is completed
                        @Override
                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                            // If it is successful
                            if (task.isSuccessful()){

                                postRef.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                    @Override
                                    public void onSuccess(Uri uri) {
                                        // making a Hash Map called postMap, adding all information there
                                        HashMap<String , Object> postMap = new HashMap<>();
                                        postMap.put("image" , uri.toString());
                                        postMap.put("user" , currentUserId);
                                        postMap.put("caption" , caption);
                                        postMap.put("time" , FieldValue.serverTimestamp());

                                        // Put the postMap to the firestore.collection.add method, to the Posts table
                                        firestore.collection("Posts").add(postMap).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
                                            @Override
                                            public void onComplete(@NonNull Task<DocumentReference> task) {
                                                // If complete and successful
                                                if (task.isSuccessful()){
                                                    // Hide progress bar
                                                    mProgressBar.setVisibility(View.INVISIBLE);
                                                    // Show successful text
                                                    Toast.makeText(AddPostActivity.this, "Post Added Successfully !!", Toast.LENGTH_SHORT).show();
                                                    // Go back to MainActivity
                                                    startActivity(new Intent(AddPostActivity.this , MainActivity.class));
                                                    // finish all storage
                                                    finish();
                                                }else{
                                                    mProgressBar.setVisibility(View.INVISIBLE);
                                                    Toast.makeText(AddPostActivity.this, task.getException().toString() , Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        });
                                    }
                                });

                            }else{
                                mProgressBar.setVisibility(View.INVISIBLE);
                                Toast.makeText(AddPostActivity.this, task.getException().getMessage() , Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }else{
                    mProgressBar.setVisibility(View.INVISIBLE);
                    Toast.makeText(AddPostActivity.this, "Please Add Image and Write Your caption", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE){
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK){

                postImageUri = result.getUri();
                mPostImage.setImageURI(postImageUri);
            }else if(resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE){
                Toast.makeText(this, result.getError().toString(), Toast.LENGTH_SHORT).show();
            }
        }
    }
}