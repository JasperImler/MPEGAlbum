package com.jaspertjyu.mpegalbum;

import android.os.Bundle;
import android.os.Handler;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageButton;
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

public class VideoPlayerActivity extends AppCompatActivity {

    public static final String EXTRA_VIDEO_PATH = "video_path";

    private SurfaceView surfaceView;
    private ImageButton playPauseButton;
    private SeekBar seekBar;
    private TextView durationTextView;
    private View controlsLayout;

    private Handler handler;
    private Runnable updateSeekBarRunnable;
    private boolean isControlsVisible = true;
    private boolean isPlaying = false;

    private LibVLC libVLC;
    private MediaPlayer mediaPlayer;
    private Media media;
    private int duration = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        String videoPath = getIntent().getStringExtra(EXTRA_VIDEO_PATH);
        Log.d("VideoPlayerActivity", "Video path: " + videoPath);
        if (videoPath == null) {
            Log.e("VideoPlayerActivity", "Video path is null, finishing activity.");
            finish();
            return;
        }

        surfaceView = findViewById(R.id.videoView);
        playPauseButton = findViewById(R.id.playPauseButton);
        seekBar = findViewById(R.id.seekBar);
        durationTextView = findViewById(R.id.durationTextView);
        controlsLayout = findViewById(R.id.controlsLayout);

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

        setupControls();
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
                        seekBar.setMax(duration);
                        updateDurationText(0, duration);
                        isPlaying = true;
                        startSeekBarUpdate();
                        break;
                    case MediaPlayer.Event.EndReached:
                        Log.d("VideoPlayerActivity", "Media EndReached");
                        isPlaying = false;
                        runOnUiThread(() -> {
                            playPauseButton.setImageResource(android.R.drawable.ic_media_play);
                        });
                        break;
                    case MediaPlayer.Event.EncounteredError:
                        Log.e("VideoPlayerActivity", "Media EncounteredError");
                        Toast.makeText(VideoPlayerActivity.this, "视频播放出错", Toast.LENGTH_SHORT).show();
                        finish();
                        break;
                }
            });

            mediaPlayer.play();
            hideControlsDelayed();

        } catch (Exception e) {
            Log.e("VideoPlayerActivity", "Error setting up VLCPlayer", e);
            Toast.makeText(this, "视频播放出错", Toast.LENGTH_SHORT).show();
            finish();
        }

        surfaceView.setOnClickListener(v -> toggleControls());
    }

    private void setupControls() {
        playPauseButton.setOnClickListener(v -> togglePlayPause());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    long seekPosition = (long) progress * 1000;
                    mediaPlayer.setTime(seekPosition);
                    updateDurationText(progress, duration);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                handler.removeCallbacks(updateSeekBarRunnable);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (isPlaying) {
                    startSeekBarUpdate();
                }
            }
        });
    }

    private void togglePlayPause() {
        if (isPlaying) {
            Log.d("VideoPlayerActivity", "Pausing video.");
            mediaPlayer.pause();
            playPauseButton.setImageResource(android.R.drawable.ic_media_play);
            handler.removeCallbacks(updateSeekBarRunnable);
        } else {
            Log.d("VideoPlayerActivity", "Starting video.");
            mediaPlayer.play();
            playPauseButton.setImageResource(android.R.drawable.ic_media_pause);
            startSeekBarUpdate();
        }
        isPlaying = !isPlaying;
    }

    private void startSeekBarUpdate() {
        updateSeekBarRunnable = new Runnable() {
            @Override
            public void run() {
                if (isPlaying) {
                    long currentPosition = mediaPlayer.getTime();
                    seekBar.setProgress((int) currentPosition);
                    updateDurationText((int) currentPosition, duration);
                    handler.postDelayed(this, 1000);
                }
            }
        };
        handler.post(updateSeekBarRunnable);
    }

    private void updateDurationText(int currentPosition, int duration) {
        String current = formatTime(currentPosition);
        String total = formatTime(duration);
        durationTextView.setText(String.format("%s/%s", current, total));
    }

    private String formatTime(int timeMs) {
        int seconds = timeMs / 1000;
        int minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    private void toggleControls() {
        if (isControlsVisible) {
            hideControls();
        } else {
            showControls();
            hideControlsDelayed();
        }
    }

    private void showControls() {
        controlsLayout.setVisibility(View.GONE);
        isControlsVisible = true;
    }

    private void hideControls() {
        controlsLayout.setVisibility(View.GONE);
        isControlsVisible = false;
    }

    private void hideControlsDelayed() {
        handler.removeCallbacks(hideControlsRunnable);
        handler.postDelayed(hideControlsRunnable, 3000);
    }

    private final Runnable hideControlsRunnable = this::hideControls;

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("VideoPlayerActivity", "Activity paused.");
        if (mediaPlayer != null && isPlaying) {
            mediaPlayer.pause();
            isPlaying = false;
            playPauseButton.setImageResource(android.R.drawable.ic_media_play);
        }
        handler.removeCallbacks(updateSeekBarRunnable);
        handler.removeCallbacks(hideControlsRunnable);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("VideoPlayerActivity", "Activity destroyed.");
        releasePlayer();
        handler.removeCallbacks(updateSeekBarRunnable);
        handler.removeCallbacks(hideControlsRunnable);
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
}