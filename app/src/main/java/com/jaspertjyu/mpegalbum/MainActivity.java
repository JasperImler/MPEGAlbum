package com.jaspertjyu.mpegalbum;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import android.os.Environment;

public class MainActivity extends AppCompatActivity implements MediaAdapter.OnItemClickListener {

    private static final int REQUEST_PERMISSIONS = 1001;
    private RecyclerView recyclerView;
    private MediaAdapter mediaAdapter;
    private List<MediaItem> mediaItems;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));

        mediaItems = new ArrayList<>();
        mediaAdapter = new MediaAdapter(this, mediaItems, this);
        recyclerView.setAdapter(mediaAdapter);

        checkPermissionsAndLoadMedia();
    }

    private void checkPermissionsAndLoadMedia() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO},
                        REQUEST_PERMISSIONS);
            } else {
                loadMediaFromStorage();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_PERMISSIONS);
            } else {
                loadMediaFromStorage();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                loadMediaFromStorage();
            } else {
                Toast.makeText(this, R.string.permission_required, Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void loadMediaFromStorage() {
        mediaItems.clear();
        // 加载指定路径的视频（Pictures/LivePhoto-Android目录）
        loadSpecificPathImages();
        mediaAdapter.notifyDataSetChanged();

        if (mediaItems.isEmpty()) {
            Toast.makeText(this, R.string.no_media_found, Toast.LENGTH_SHORT).show();
        }
    }

    private void loadImages() {
        String[] projection = {MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null, null, null);

        if (cursor != null) {
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
            int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
            int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

            while (cursor.moveToNext()) {
                long id = cursor.getLong(idColumn);
                String name = cursor.getString(nameColumn);
                String data = cursor.getString(dataColumn);

                MediaItem mediaItem = new MediaItem(id, name, data, MediaItem.TYPE_IMAGE);
                mediaItems.add(mediaItem);
            }
            cursor.close();
        }
    }

    private void loadVideos() {
        String[] projection = {MediaStore.Video.Media._ID, MediaStore.Video.Media.DISPLAY_NAME, MediaStore.Video.Media.DATA, MediaStore.Video.Media.MIME_TYPE};
        Cursor cursor = getContentResolver().query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, projection, null, null, null);

        if (cursor != null) {
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID);
            int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME);
            int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
            int mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE);

            while (cursor.moveToNext()) {
                long id = cursor.getLong(idColumn);
                String name = cursor.getString(nameColumn);
                String data = cursor.getString(dataColumn);
                String mimeType = cursor.getString(mimeTypeColumn);

                // Check if it's an MPEG video
                if (mimeType.contains("mpeg") || name.toLowerCase().endsWith(".mpg") || name.toLowerCase().endsWith(".mpeg")) {
                    MediaItem mediaItem = new MediaItem(id, name, data, MediaItem.TYPE_VIDEO);
                    mediaItems.add(mediaItem);
                }
            }
            cursor.close();
        }
    }

    public static void isDirExitAndCreate(String path)
	{
		File file = new File(path);

		Log.i("FileUtil", "判断目录是否存在: " + path);
		//如果不存在 或者 不是一个目录 就创建一个
		if(!file.exists() || (file.exists() && !file.isDirectory()))
		{
			Log.i("FileUtil", "目录不存在，准备创建");
			boolean res = file.mkdir();
			if (!res) {
				Log.i("FileUtil", "目录创建失败");
			}else {
				Log.i("FileUtil", "目录创建成功");
			}
		}
	}

    private void loadSpecificPathVideos() {
        // 获取sdk根目录
        File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        String appPath =  picturesDir + File.separator + "LivePhoto-Android";
        isDirExitAndCreate(appPath);
        File targetDir = new File(appPath);

        if (targetDir.exists() && targetDir.isDirectory()) {
            // 筛选MPEG视频文件
            File[] videoFiles = targetDir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    String lowerCaseName = name.toLowerCase();
                    return lowerCaseName.endsWith(".mp4");
                }
            });
            if (videoFiles != null && videoFiles.length > 0) {
                for (File videoFile : videoFiles) {
                    String videoName = videoFile.getName();
                    String videoPath = videoFile.getAbsolutePath();
                    
                    // 获取对应的jpg文件路径
                    String jpgName = videoName.substring(0, videoName.lastIndexOf("."))+ "_IMG" + ".jpg";
                    File jpgFile = new File(targetDir, jpgName);
                    
                    if (jpgFile.exists()) {
                        // 使用文件路径的哈希值作为ID
                        long id = videoPath.hashCode();
                        MediaItem mediaItem = new MediaItem(id, videoName, videoPath, MediaItem.TYPE_VIDEO);
                        mediaItem.setCoverPath(jpgFile.getAbsolutePath());
                        mediaItems.add(mediaItem);
                    }
                }
            }
        }
        Collections.reverse(mediaItems);
    }
    
    private void loadSpecificPathImages() {
        // 获取sdk根目录
        File picturesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        String appPath = picturesDir + File.separator + "LivePhoto-Android";
        isDirExitAndCreate(appPath);
        File targetDir = new File(appPath);
    
        if (targetDir.exists() && targetDir.isDirectory()) {
            // 筛选JPG图片文件
            File[] imageFiles = targetDir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    String lowerCaseName = name.toLowerCase();
                    return lowerCaseName.endsWith(".jpg");
                }
            });
            if (imageFiles != null && imageFiles.length > 0) {
                for (File imageFile : imageFiles) {
                    String imageName = imageFile.getName();
                    String imagePath = imageFile.getAbsolutePath();
    
                    // 使用文件路径的哈希值作为ID
                    long id = imagePath.hashCode();
                    MediaItem mediaItem = new MediaItem(id, imageName, imagePath, MediaItem.TYPE_IMAGE);
                    mediaItems.add(mediaItem);
                }
            }
        }
        Collections.reverse(mediaItems);
    }
    
    @Override
    public void onItemClick(MediaItem mediaItem) {
        if (mediaItem.getType() == MediaItem.TYPE_IMAGE) {
            Intent intent = new Intent(this, VideoPlayerActivity.class);
            intent.putExtra(VideoPlayerActivity.EXTRA_IMAGE_PATH, mediaItem.getPath());
            startActivity(intent);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_refresh) {
            loadMediaFromStorage();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}