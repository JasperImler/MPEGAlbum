package com.jaspertjyu.mpegalbum;

import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.interfaces.IVLCVout;

import java.util.ArrayList;
import java.util.Locale;

import android.util.Log;

import com.bumptech.glide.Glide;

import android.content.Context;
import android.os.VibrationEffect;
import android.os.Vibrator;

public class VideoPlayerActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_PATH = "video_path";

    private ImageView imageView;
    private androidx.appcompat.widget.Toolbar toolbar;
    private SurfaceView surfaceView;

    private Handler handler;
    private Runnable updateSeekBarRunnable;
    private boolean isControlsVisible = true;
    private boolean isPlaying = false;
    private LibVLC libVLC;
    private MediaPlayer mediaPlayer;
    private Media media;
    private int duration = 0;

    private String imgPath;
    private String videoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        imgPath = getIntent().getStringExtra(EXTRA_IMAGE_PATH);
        Log.d("VideoPlayerActivity", "Image path: " + imgPath);
        videoPath = convertImagePathToVideoPath(imgPath);
        if (imgPath == null) {
            Log.e("VideoPlayerActivity", "Video path is null, finishing activity.");
            finish();
            return;
        }

        imageView = findViewById(R.id.imageView);
        surfaceView = findViewById(R.id.videoView);
        toolbar = findViewById(R.id.toolbar);

        handler = new Handler();

        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                setupVLCPlayer(videoPath);
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                if (mediaPlayer != null) {
                    IVLCVout vlcVout = mediaPlayer.getVLCVout();
                    vlcVout.setWindowSize(width, height);

                    // 获取视频的原始尺寸
                    Media.VideoTrack videoTrack = mediaPlayer.getCurrentVideoTrack();
                    if (videoTrack != null) {
                        float videoWidth = videoTrack.width;
                        float videoHeight = videoTrack.height;

                        // 计算适合屏幕的缩放比例
                        float scale;
                        if (width * videoHeight > height * videoWidth) {
                            scale = height / videoHeight;
                        } else {
                            scale = width / videoWidth;
                        }

                        // 计算缩放后的尺寸
                        int scaledWidth = (int) (videoWidth * scale);
                        int scaledHeight = (int) (videoHeight * scale);

                        // 设置SurfaceView的尺寸
                        holder.setFixedSize(scaledWidth, scaledHeight);
                    }
                }
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                releasePlayer();
            }
        });

        setupImageView();
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    private void showImageView() {
        surfaceView.setVisibility(View.INVISIBLE);
        imageView.setVisibility(View.VISIBLE);
    }

    private void showVideoView() {
        surfaceView.setVisibility(View.VISIBLE);
        imageView.setVisibility(View.INVISIBLE);
    }

    private String convertImagePathToVideoPath(String imgPath) {
        return imgPath.replace("_IMG.jpg", ".mp4");
    }

    private void setupImageView() {
        Glide.with(this).load(imgPath).into(imageView);
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
         imageView.setOnTouchListener((v, event) -> {
             switch (event.getAction()) {
                 case MotionEvent.ACTION_DOWN:
                     vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.EFFECT_TICK));
                     showVideoView();
                     mediaPlayer.play();
                     break;
                 case MotionEvent.ACTION_UP:
                     mediaPlayer.pause();
                     showImageView();
                     break;
             }
             return true;
         });
        showImageView();
    }

    private void setupVLCPlayer(String videoPath) {
        try {
            ArrayList<String> options = new ArrayList<>();
            options.add("--no-audio");
            options.add("--no-video-title-show");
            options.add("--no-stats");
            options.add("--no-snapshot-preview");

            libVLC = new LibVLC(this, options);
            mediaPlayer = new MediaPlayer(libVLC);

            IVLCVout vlcVout = mediaPlayer.getVLCVout();
            vlcVout.setVideoView(surfaceView);

            // 获取屏幕尺寸
            android.util.DisplayMetrics displayMetrics = new android.util.DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            int screenWidth = displayMetrics.widthPixels;
            int screenHeight = displayMetrics.heightPixels;

            // 设置视频输出尺寸
            vlcVout.setWindowSize(screenWidth, screenHeight);
            surfaceView.getHolder().setFixedSize(screenWidth, screenHeight);

            vlcVout.attachViews();

            media = new Media(libVLC, videoPath);
            media.setHWDecoderEnabled(false,false);
            mediaPlayer.setMedia(media);

            mediaPlayer.setEventListener(event -> {
                switch(event.type) {
                    case MediaPlayer.Event.Opening:
                        Log.d("VideoPlayerActivity", "Media Opening");
                        break;
                    case MediaPlayer.Event.Playing:
                        Log.d("VideoPlayerActivity", "Media Playing");
                        duration = (int) mediaPlayer.getLength();
                        isPlaying = true;
                        break;
                    case MediaPlayer.Event.EndReached:
                        Log.d("VideoPlayerActivity", "Media EndReached");
                        isPlaying = false;
                        break;
                    case MediaPlayer.Event.EncounteredError:
                        Log.e("VideoPlayerActivity", "Media EncounteredError");
                        Toast.makeText(VideoPlayerActivity.this, "视频播放出错", Toast.LENGTH_SHORT).show();
                        finish();
                        break;
                }
            });
        } catch (Exception e) {
            Log.e("VideoPlayerActivity", "Error setting up VLCPlayer", e);
            Toast.makeText(this, "视频播放出错", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("VideoPlayerActivity", "Activity started.");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("VideoPlayerActivity", "Activity paused.");
        if (mediaPlayer != null && isPlaying) {
            mediaPlayer.pause();
            isPlaying = false;
        }
        handler.removeCallbacks(updateSeekBarRunnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("VideoPlayerActivity", "Activity resumed.");
        if (mediaPlayer != null && isPlaying) {
            mediaPlayer.pause();
            isPlaying = false;
        }
        handler.removeCallbacks(updateSeekBarRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("VideoPlayerActivity", "Activity destroyed.");
        releasePlayer();
        handler.removeCallbacks(updateSeekBarRunnable);
    }

    private void releasePlayer() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.getVLCVout().detachViews();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (media != null) {
            media.release();
            media = null;
        }
        if (libVLC != null) {
            libVLC.release();
            libVLC = null;
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}