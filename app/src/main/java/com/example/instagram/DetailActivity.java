package com.example.instagram;

import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.instagram.model.Comment;
import com.example.instagram.model.Post;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import jp.wasabeef.glide.transformations.CropSquareTransformation;

import static com.bumptech.glide.request.RequestOptions.bitmapTransform;

public class DetailActivity extends AppCompatActivity {
    @BindView(R.id.ivPostImage) ImageView ivPostImage;
    @BindView(R.id.tvDescription) TextView tvDescription;
    @BindView(R.id.tvName) TextView tvName;
    @BindView(R.id.ivProfileImage) ImageView ivProfileImage;
    @BindView(R.id.tvCreatedAt) TextView tvCreatedAt;
    @BindView(R.id.tvNumLikes) TextView tvNumLikes;
    @BindView(R.id.ivHeart) ImageView ivHeart;
    @BindView(R.id.rvComments) RecyclerView rvComments;
    @BindView(R.id.etCompose) EditText etCompose;
    @BindView(R.id.tvComment) TextView tvComment;

    Post post;
    private boolean isLikedByUser = false;
    ArrayList<Comment> comments;
    CommentAdapter adapter;

    private final String TAG = "DetailActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        ButterKnife.bind(this);

        String postId = getIntent().getStringExtra("postId");

        final Post.Query postsQuery = new Post.Query();

        try {
            post = postsQuery.withUser().get(postId);
        } catch (ParseException e) {
            Log.d(TAG, "Post not found");
            e.printStackTrace();
            finish();
        }


        isLikedByUser = post.isLikedBy(ParseUser.getCurrentUser().getUsername());

        ivHeart.setSelected(isLikedByUser);

        tvDescription.setText(post.getDescription());
        tvName.setText(post.getUser().getUsername());
        Integer numLikes = post.getLikes();
        if (numLikes == 1) {
            tvNumLikes.setText(String.format("%s like", numLikes));
        } else {
            tvNumLikes.setText(String.format("%s likes", numLikes));
        }
        Date rawDate = post.getCreatedAt();

        tvCreatedAt.setText(String.format("Created at %s on %s", getCreationTime(rawDate), getCreationDate(rawDate)));
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int width = metrics.widthPixels;
        Glide.with(this)
                .load(post.getImage().getUrl())
                .apply(bitmapTransform(new CropSquareTransformation()))
                .apply(new RequestOptions().override(width, width))
                .into(ivPostImage);

        ParseUser user = post.getUser();
        ParseFile profilePic = user.getParseFile("profileImage");
        if (profilePic != null) {
            Glide.with(this)
                    .load(profilePic.getUrl())
                    .apply(RequestOptions.circleCropTransform())
                    .into(ivProfileImage);
        } else {
            Glide.with(this)
                    .load(this.getResources().getIdentifier("ic_user_profile_filled", "drawable", this.getPackageName()))
                    .apply(RequestOptions.circleCropTransform())
                    .into(ivProfileImage);
        }

        comments = new ArrayList<>();
        adapter = new CommentAdapter(comments);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        rvComments.setLayoutManager(linearLayoutManager);
        rvComments.setAdapter(adapter);

        getComments();
    }

    @OnClick(R.id.ivHeart)
    void like() {
        if (!isLikedByUser) {
            ivHeart.setSelected(true);
            int numLikes = Integer.parseInt(tvNumLikes.getText().toString().split(" ")[0]);
            isLikedByUser = true;
            post.setLikes(numLikes + 1);
            post.add(ParseUser.getCurrentUser().getUsername());
            post.saveInBackground();
            if (numLikes == 0) {
                tvNumLikes.setText(String.format("%s like", numLikes + 1));
            } else {
                tvNumLikes.setText(String.format("%s likes", numLikes + 1));
            }
        } else {
            ivHeart.setSelected(false);
            int numLikes = Integer.parseInt(tvNumLikes.getText().toString().split(" ")[0]);
            isLikedByUser = false;
            post.setLikes(numLikes - 1);
            post.remove(ParseUser.getCurrentUser().getUsername());
            post.saveInBackground();
            if (numLikes == 2) {
                tvNumLikes.setText(String.format("%s like", numLikes - 1));
            } else {
                tvNumLikes.setText(String.format("%s likes", numLikes - 1));
            }
        }
    }

    @OnClick(R.id.tvComment)
    void createComment() {
        String content = etCompose.getText().toString();
        newComment(content);
        etCompose.setText("");
    }

    public void getComments() {
        final Comment.Query commentQuery = new Comment.Query();
        commentQuery.getComments(post);

        commentQuery.findInBackground((objects, e) -> {
            if (e == null) {
                Log.d(TAG, objects.toString());
                Log.d(TAG, "" + objects.size());
                adapter.clear();
                adapter.addAll(objects);
            } else {
                e.printStackTrace();
            }
        });
    }

    public void newComment(String content) {
        final Comment newComment = new Comment();
        newComment.setContent(content);
        newComment.setPost(post);
        newComment.setUser(ParseUser.getCurrentUser());

        newComment.saveInBackground(e -> {
            if (e == null) {
                Log.d(TAG, "Create comment success!");
                comments.add(newComment);
                adapter.notifyItemInserted(comments.size() - 1);

            } else {
                Log.e(TAG, "Comment not created");
                e.printStackTrace();
            }
        });
    }

    private String getCreationDate(Date rawDate) {
        SimpleDateFormat simpleDate = new SimpleDateFormat("MM/dd", Locale.US);
        String strDate = simpleDate.format(rawDate);
        return strDate;
    }

    private String getCreationTime(Date rawDate) {
        SimpleDateFormat simpleDate = new SimpleDateFormat("hh:mm", Locale.US);
        String strDate = simpleDate.format(rawDate);
        return strDate;
    }
}
