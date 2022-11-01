package com.kingchan.buythedip.Model;

import java.util.Date;

public class Post extends PostId {

    // Need to match AddPostActivity - PostMap
    private String image , user , caption;
    private Date time;

    public String getImage() {
        return image;
    }

    public String getUser() {
        return user;
    }

    public String getCaption() {
        return caption;
    }

    public Date getTime() {
        return time;
    }
}
