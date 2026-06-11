package com.example.myapplication3;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private MediaPlayer mediaPlayer;
    private SeekBar seekBar;
    private ImageButton btnPlayPause, btnNext, btnPrev;
    private TextView tvCurrentTime, tvTotalDuration;

    private List<Integer> playlist = new ArrayList<>();
    private int currentIndex = 0;

    private Handler handler = new Handler();
    private boolean isTracking = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();

        scanRawFolder();

        if (!playlist.isEmpty()) {
            loadTrack(currentIndex, false);
        } else {
            Toast.makeText(this, "هیچ فایلی در پوشه raw پیدا نشد!", Toast.LENGTH_LONG).show();
        }

        setupListeners();
    }

    private void initViews() {
        seekBar = findViewById(R.id.seekBar);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnNext = findViewById(R.id.btnNext);
        btnPrev = findViewById(R.id.btnPrevious);
        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        tvTotalDuration = findViewById(R.id.tvTotalDuration);
    }

    private void scanRawFolder() {
        Field[] fields = R.raw.class.getFields();
        for (Field field : fields) {
            try {
                int resId = field.getInt(null);
                playlist.add(resId);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private void loadTrack(int index, boolean shouldPlay) {
        if (playlist.isEmpty()) return;

        releaseMediaPlayer();

        currentIndex = index;
        mediaPlayer = MediaPlayer.create(this, playlist.get(currentIndex));

        if (mediaPlayer != null) {
            seekBar.setMax(mediaPlayer.getDuration());
            tvTotalDuration.setText(createTimeLabel(mediaPlayer.getDuration()));
            seekBar.setProgress(0);
            tvCurrentTime.setText(createTimeLabel(0));

            mediaPlayer.setOnCompletionListener(mp -> nextTrack());

            if (shouldPlay) {
                mediaPlayer.start();
                btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
                startSeekBarUpdate();
            } else {
                btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
            }
        }
    }

    private void nextTrack() {
        if (playlist.isEmpty()) return;
        int nextIndex = (currentIndex + 1) % playlist.size();
        loadTrack(nextIndex, true);
    }

    private void prevTrack() {
        if (playlist.isEmpty()) return;
        int prevIndex = (currentIndex - 1 + playlist.size()) % playlist.size();
        loadTrack(prevIndex, true);
    }

    private void setupListeners() {
        btnPlayPause.setOnClickListener(v -> {
            if (mediaPlayer == null) return;

            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
            } else {
                mediaPlayer.start();
                btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
                startSeekBarUpdate();
            }
        });

        btnNext.setOnClickListener(v -> nextTrack());
        btnPrev.setOnClickListener(v -> prevTrack());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) {
                    tvCurrentTime.setText(createTimeLabel(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isTracking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isTracking = false;
                if (mediaPlayer != null) {
                    mediaPlayer.seekTo(seekBar.getProgress());
                }
            }
        });
    }

    private void startSeekBarUpdate() {
        handler.removeCallbacks(seekBarUpdater);
        handler.post(seekBarUpdater);
    }

    private Runnable seekBarUpdater = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                if (!isTracking) {
                    int currentPos = mediaPlayer.getCurrentPosition();
                    seekBar.setProgress(currentPos);
                    tvCurrentTime.setText(createTimeLabel(currentPos));
                }
                handler.postDelayed(this, 1000);
            }
        }
    };

    private String createTimeLabel(int duration) {
        int minutes = (duration / 1000) / 60;
        int seconds = (duration / 1000) % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private void releaseMediaPlayer() {
        handler.removeCallbacks(seekBarUpdater);
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseMediaPlayer();
        handler.removeCallbacksAndMessages(null);
    }
}