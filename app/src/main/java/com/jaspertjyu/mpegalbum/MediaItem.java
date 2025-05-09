package com.jaspertjyu.mpegalbum;

public class MediaItem {
    public static final int TYPE_IMAGE = 0;
    public static final int TYPE_VIDEO = 1;

    private final long id;
    private final String name;
    private final String path;
    private final int type;
    private String coverPath;

    public MediaItem(long id, String name, String path, int type) {
        this.id = id;
        this.name = name;
        this.path = path;
        this.type = type;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getPath() {
        return path;
    }

    public int getType() {
        return type;
    }

    public boolean isVideo() {
        return type == TYPE_VIDEO;
    }

    public boolean isImage() {
        return type == TYPE_IMAGE;
    }

    public String getCoverPath() {
        return coverPath;
    }

    public void setCoverPath(String coverPath) {
        this.coverPath = coverPath;
    }
}