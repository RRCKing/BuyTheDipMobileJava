package com.kingchan.buythedip;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.kingchan.buythedip.Adapter.PostAdapter;
import com.kingchan.buythedip.Model.Post;

// Retrieve Posts - 8
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.kingchan.buythedip.Model.Users;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // Set the firebase and firestore variables
    private FirebaseAuth firebaseAuth;
    private Toolbar mainToolbar;
    private FirebaseFirestore firestore;
    private RecyclerView mRecyclerView;
    private FloatingActionButton fab;

    // Retrieve Posts - 8
    private PostAdapter adapter;
    private List<Post> list;
    // Retrieve Posts - 8
    private Query query;
    private ListenerRegistration listenerRegistration;
    // Retrieving User - 12
    private List<Users> usersList;

    // Search
    private SearchView searchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        mainToolbar = findViewById(R.id.main_toolbar);

        // Add Post Activity - 5
        mRecyclerView = findViewById(R.id.recyclerView);

        // Post Layout - 7
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(MainActivity.this));

        // Retrieve Posts - 8
        list = new ArrayList<>();
        // Retrieving User
        usersList = new ArrayList<>();
        adapter = new PostAdapter(MainActivity.this , list, usersList);
        mRecyclerView.setAdapter(adapter);

        fab = findViewById(R.id.floatingActionButton);
        //setSupportActionBar(mainToolbar);
        getSupportActionBar().setTitle("Buy The Dip");

        // Add Post - 6
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this , AddPostActivity.class));
            }
        });

        // Retrieve Posts - 8
        if (firebaseAuth.getCurrentUser() != null){

            mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                    super.onScrolled(recyclerView, dx, dy);
                    Boolean isBottom = !mRecyclerView.canScrollVertically(1);
                    if (isBottom)
                        Toast.makeText(MainActivity.this, "Reached Bottom", Toast.LENGTH_SHORT).show();
                }
            });
            query = firestore.collection("Posts").orderBy("time" , Query.Direction.DESCENDING);

            // Search Function
            searchView = findViewById(R.id.searchView);
            // some device will make the hint text as edit
            searchView.clearFocus();

            // Search
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String searchText) {
                    //list.clear();
                    retrieveData(searchText.toLowerCase());
                    return true;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    retrieveData(newText.toLowerCase());
                    return false;
                }
            });

            retrieveData("");
        }

        // Swipe to Edit
        ItemTouchHelper touchHelper = new ItemTouchHelper(new TouchHelper(adapter));
        touchHelper.attachToRecyclerView(mRecyclerView);
    }

    // Get the user
    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null){
            startActivity(new Intent(MainActivity.this , SignInActivity.class));
            finish();
        }else{
            String currentUserId = firebaseAuth.getCurrentUser().getUid();

            firestore.collection("Users").document(currentUserId).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                    if (task.isSuccessful()){
                        if (!task.getResult().exists()){
                            startActivity(new Intent(MainActivity.this , SetUpActivity.class));
                            finish();
                        }
                    }
                }
            });
        }
    }

    // Get the menu for Sign out
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu , menu);
        return true;
    }

    // Set when option selected e.g. Profile or Sign Out
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.profile_menu){
            startActivity(new Intent(MainActivity.this , SetUpActivity.class));
        }else if(item.getItemId() == R.id.sign_out_menu){
            firebaseAuth.signOut();
            startActivity(new Intent(MainActivity.this , SignInActivity.class));
            finish();
        }
        return true;
    }

    // Retrieve Data
    private void retrieveData(String text) {
        // The SnapshortListener gives us all the data inside that post
        listenerRegistration = query.addSnapshotListener(MainActivity.this, new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                list.clear();
                for (DocumentChange doc : value.getDocumentChanges()){
                    if (doc.getType() == DocumentChange.Type.ADDED){

                        String postId = doc.getDocument().getId();
                        Post post = doc.getDocument().toObject(Post.class).withId(postId);
                        String postUserId = doc.getDocument().getString("user");

                        if (!text.isEmpty() && post.getCaption().toLowerCase().contains(text)){
                            loadPosts(postId, post, postUserId);
                        }else if (text.isEmpty()){
                            loadPosts(postId, post, postUserId);
                        }
                    }else{
                        adapter.notifyDataSetChanged();
                    }
                }
                listenerRegistration.remove();
            }
        });
    }

    private void loadPosts(String postId, Post post, String postUserId){

        firestore.collection("Users").document(postUserId).get()
                .addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (task.isSuccessful()){
                            Users users = task.getResult().toObject(Users.class);
                            usersList.add(users);
                            list.add(post);

                            // Set the data to the adapter
                            adapter.notifyDataSetChanged();
                        }else{
                            Toast.makeText(MainActivity.this, task.getException().getMessage() , Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}