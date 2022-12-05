package com.kingchan.buythedip.Adapter;

import android.annotation.SuppressLint;
import android.app.Activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.kingchan.buythedip.CommentsActivity;
import com.kingchan.buythedip.MainActivity;
import com.kingchan.buythedip.Model.Post;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.hdodenhof.circleimageview.CircleImageView;

import com.kingchan.buythedip.Model.Users;
import com.kingchan.buythedip.R;
import com.kingchan.buythedip.UpdatePostActivity;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private List<Post> mList;
    private Activity context;

    // Retrieve Posts - 8, User pic and User Name
    private FirebaseFirestore firestore;
    private FirebaseAuth auth;

    // Retrieving User - 12
    private List<Users> usersList;

    // Retrieving User - 12, add userList
    public PostAdapter(Activity context , List<Post> mList, List<Users> usersList){
        this.mList = mList;
        this.context = context;
        this.usersList = usersList;
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(context).inflate(R.layout.each_post , parent , false);
        // Retrieve Posts -8, User pic and User Name
        firestore = FirebaseFirestore.getInstance();
        // Like Feature - 9
        auth = FirebaseAuth.getInstance();
        return new PostViewHolder(v);
    }

    // Retrieve Posts -8
    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, @SuppressLint("RecyclerView") int position) {
        Post post = mList.get(position);
        holder.setPostPic(post.getImage());
        holder.setPostCaption(post.getCaption());
        holder.setStoreInfo(post.getStore());

        long milliseconds = post.getTime().getTime();
        String date  = DateFormat.format("MM/dd/yyyy" , new Date(milliseconds)).toString();
        holder.setPostDate(date);

        // Refracting Code - 12, improving performance
        String username = usersList.get(position).getName();
        String image = usersList.get(position).getImage();

        holder.setProfilePic(image);
        holder.setPostUsername(username);

        //likebtn
        String postId = post.PostId;
        String currentUserId = auth.getCurrentUser().getUid();
        holder.likePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                firestore.collection("Posts/" + postId + "/Likes").document(currentUserId).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                        if (!task.getResult().exists()){
                            Map<String , Object> likesMap = new HashMap<>();
                            likesMap.put("timestamp" , FieldValue.serverTimestamp());
                            firestore.collection("Posts/" + postId + "/Likes").document(currentUserId).set(likesMap);
                        }else{
                            firestore.collection("Posts/" + postId + "/Likes").document(currentUserId).delete();
                        }
                    }
                });
            }
        });

        // Set clickable caption
        holder.postCaption.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Creates an Intent that will load a map of San Francisco
                Uri gmmIntentUri = Uri.parse("geo:37.7749,-122.4194");
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                context.startActivity(mapIntent);
            }
        });

        //like color change
        firestore.collection("Posts/" + postId + "/Likes").document(currentUserId).addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error == null){
                    if (value.exists()){
                        holder.likePic.setImageDrawable(context.getDrawable(R.drawable.after_liked));
                    }else{
                        holder.likePic.setImageDrawable(context.getDrawable(R.drawable.before_liked));
                    }
                }
            }
        });

        //likes count - 10
        firestore.collection("Posts/" + postId + "/Likes").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot value, @Nullable FirebaseFirestoreException error) {
                if (error == null){
                    if (!value.isEmpty()){
                        // show number of likes
                        int count = value.size();
                        holder.setPostLikes(count);
                    }else{
                        holder.setPostLikes(0);
                    }
                }
            }
        });

        //comments implementation
        holder.commentsPic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent commentIntent = new Intent(context , CommentsActivity.class);
                commentIntent.putExtra("postid" , postId);
                context.startActivity(commentIntent);
            }
        });

        // Delete - 13
        if (currentUserId.equals(post.getUser())){
            // Show delete button
            holder.deleteBtn.setVisibility(View.VISIBLE);
            // Set it clickable
            holder.deleteBtn.setClickable(true);
            // Set on click event
            holder.deleteBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Prompt alert box
                    AlertDialog.Builder alert = new AlertDialog.Builder(context);
                    // Set the alert box
                    alert.setTitle("Delete")
                            .setMessage("Are You Sure ?")
                            .setNegativeButton("No" , null)
                            .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                                // When click yes
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    // Delete the comments
                                    firestore.collection("Posts/" + postId + "/Comments").get()
                                            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                @Override
                                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                    for (QueryDocumentSnapshot snapshot : task.getResult()){
                                                        firestore.collection("Posts/" + postId + "/Comments").document(snapshot.getId()).delete();
                                                    }
                                                }
                                            });
                                    // Delete the likes
                                    firestore.collection("Posts/" + postId + "/Likes").get()
                                            .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                                                @Override
                                                public void onComplete(@NonNull Task<QuerySnapshot> task) {
                                                    for (QueryDocumentSnapshot snapshot : task.getResult()){
                                                        firestore.collection("Posts/" + postId + "/Likes").document(snapshot.getId()).delete();
                                                    }
                                                }
                                            });
                                    // Delete the post
                                    firestore.collection("Posts").document(postId).delete();
                                    // Remove the position of the post list
                                    mList.remove(position);
                                    // Trigger it to the adapter
                                    notifyDataSetChanged();
                                }
                            });
                    alert.show();
                }
            });
        }
    }

    // Edit function
    public void updateData(int position){
        // Model item = mList.get(position);
        Post post = mList.get(position);

        Bundle bundle = new Bundle();
        bundle.putString("uId", post.PostId);
        bundle.putString("uCaption", post.getCaption());
        bundle.putString("uPostImage", post.getImage());
        Intent intent = new Intent(context, UpdatePostActivity.class);
        intent.putExtras(bundle);
        context.startActivity(intent);
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    // For Reading or View Post in Main Activity
    public class PostViewHolder extends RecyclerView.ViewHolder{
        ImageView postPic , commentsPic , likePic;
        CircleImageView profilePic ;
        TextView postUsername , postDate , postCaption , postLikes, storeSelected;
        // Delete - 13
        ImageButton deleteBtn;
        // Retrieve Posts -8
        View mView;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            // Retrieve Posts -8
            mView = itemView;
            // Like Feature - 9
            likePic = mView.findViewById(R.id.like_btn);
            // CommentsPic - 10
            commentsPic = mView.findViewById(R.id.comments_post);
            // Delete - 13
            deleteBtn= mView.findViewById(R.id.delete_btn);

        }

        // Like Feature - 9
        public void setPostLikes(int count){
            postLikes = mView.findViewById(R.id.like_count_tv);

            // Like Count - 10
            postLikes.setText(count + " Likes");
        }

        // Retrieve Posts -8 start
        public void setPostPic(String urlPost){
            postPic = mView.findViewById(R.id.user_post);
            Glide.with(context).load(urlPost).into(postPic);
        }
        public void setProfilePic(String urlProfile){
            profilePic = mView.findViewById(R.id.profile_pic);
            Glide.with(context).load(urlProfile).into(profilePic);
        }
        public void setPostUsername(String username){
            postUsername = mView.findViewById(R.id.username_tv);
            postUsername.setText(username);
        }
        public void setPostDate(String date){
            postDate = mView.findViewById(R.id.date_tv);
            postDate.setText(date);
        }
        public void setPostCaption(String caption){
            postCaption = mView.findViewById(R.id.caption_tv);
            postCaption.setText(caption);
        }
        public void setStoreInfo(String store){
            storeSelected = mView.findViewById(R.id.store_tv);
            storeSelected.setText(store);
        }
        // Retrieve Posts -8 end
    }
}
