package com.yttv.app;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;
import androidx.media3.ui.PlayerView;

import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.VideoStream;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlayerActivity extends Activity {
    private ExoPlayer player;
    private PlayerView view;
    private TextView msg;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(0xFF000000);
        view = new PlayerView(this);
        msg = new TextView(this);
        msg.setTextColor(0xFFFFFFFF);
        msg.setTextSize(24);
        msg.setGravity(Gravity.CENTER);
        msg.setText("جاري التحميل...");
        root.addView(view, new FrameLayout.LayoutParams(-1, -1));
        root.addView(msg, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);

        final String url = getIntent().getStringExtra("url");
        new Thread(() -> {
            try {
                StreamInfo info = StreamInfo.getInfo(url);
                String best = null;
                int bestH = 0;
                for (VideoStream v : info.getVideoStreams()) {
                    int h = height(v.getResolution());
                    if (h <= 720 && h >= bestH) {
                        bestH = h;
                        best = v.getContent();
                    }
                }
                final boolean hls = best == null;
                if (hls) best = info.getHlsUrl();
                if (best == null || best.isEmpty()) throw new Exception("ما لقيت رابط تشغيل");
                final String finalUrl = best;
                runOnUiThread(() -> play(finalUrl, hls));
            } catch (Exception e) {
                runOnUiThread(() -> msg.setText("خطأ: " + e));
            }
        }).start();
    }

    private int height(String res) {
        if (res == null) return 0;
        Matcher m = Pattern.compile("^(\\d+)").matcher(res);
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }

    private void play(String url, boolean hls) {
        DefaultHttpDataSource.Factory f = new DefaultHttpDataSource.Factory()
                .setUserAgent(DownloaderImpl.UA)
                .setAllowCrossProtocolRedirects(true);
        player = new ExoPlayer.Builder(this)
                .setMediaSourceFactory(new DefaultMediaSourceFactory(f))
                .build();
        view.setPlayer(player);
        MediaItem.Builder mi = new MediaItem.Builder().setUri(url);
        if (hls) mi.setMimeType(MimeTypes.APPLICATION_M3U8);
        player.setMediaItem(mi.build());
        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_READY) msg.setVisibility(View.GONE);
            }
            @Override
            public void onPlayerError(PlaybackException e) {
                msg.setVisibility(View.VISIBLE);
                msg.setText("خطأ تشغيل: " + e.getErrorCodeName());
            }
        });
        player.prepare();
        player.play();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
        if (player != null && e.getAction() == KeyEvent.ACTION_DOWN) {
            switch (e.getKeyCode()) {
                case KeyEvent.KEYCODE_DPAD_CENTER:
                case KeyEvent.KEYCODE_ENTER:
                    player.setPlayWhenReady(!player.getPlayWhenReady());
                    return true;
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    player.seekTo(player.getCurrentPosition() + 10000);
                    return true;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    player.seekTo(Math.max(0, player.getCurrentPosition() - 10000));
                    return true;
            }
        }
        return super.dispatchKeyEvent(e);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) player.release();
    }
}
