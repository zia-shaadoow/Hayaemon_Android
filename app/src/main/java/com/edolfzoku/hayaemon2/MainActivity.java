/*
 * MainActivity
 *
 * Copyright (c) 2018 Ryota Yamauchi. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.edolfzoku.hayaemon2;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.FileProvider;
import android.support.v4.view.ViewPager;
import android.support.v4.view.accessibility.AccessibilityEventCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.method.ScrollingMovementMethod;
import android.util.TypedValue;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.android.vending.billing.IInAppBillingService;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.un4seen.bass.BASS;
import com.un4seen.bass.BASS_FX;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.un4seen.bass.BASS_AAC.BASS_CONFIG_AAC_MP4;

public class MainActivity extends AppCompatActivity implements View.OnClickListener,
        View.OnLongClickListener, View.OnTouchListener, DrawerLayout.DrawerListener
{
    public PlaylistFragment playlistFragment;
    public LoopFragment loopFragment;
    public ControlFragment controlFragment;
    public EqualizerFragment equalizerFragment;
    public EffectFragment effectFragment;
    static int sFxVol;
    static int sStream;
    static int sRecord;
    private DrawerLayout mDrawerLayout;
    private boolean mLoopA, mLoopB;
    private double mLoopAPos, mLoopBPos;
    private long mByteLength;
    private double mLength;
    private HoldableViewPager mViewPager;
    private int mSync;
    private IInAppBillingService mService;
    private ServiceConnection mServiceConn;
    private boolean mShowUpdateLog;
    private boolean mPlayNextByBPos;
    private boolean mWaitEnd = false;
    private BroadcastReceiver mReceiver;
    private boolean mBound = false;
    private float mDensity;

    private AdView mAdView;
    private LinearLayout mLinearControl;
    private SeekBar mSeekCurPos;
    private ImageView mImgViewDown;
    private TabLayout mTabLayout;
    private View mViewSep1, mViewSep2;
    private TextView mTextCurPos, mTextRemain, mTextTitle, mTextArtist, mTextRecordingTime;
    private AnimationButton mBtnRewind, mBtnPlay, mBtnForward, mBtnShuffle, mBtnRepeat, mBtnRecord, mBtnPlayInPlayingBar, mBtnForwardInPlayingBar, mBtnRewindInPlayingBar, mBtnMoreInPlayingBar, mBtnShuffleInPlayingBar, mBtnRepeatInPlayingBar, mBtnCloseInPlayingBar, mBtnStopRecording, mBtnArtworkInPlayingBar;
    private RelativeLayout mRelativeRecording, mRelativeSave, mRelativeLock, mRelativeAddSong, mRelativeItem, mRelativeReport, mRelativeReview, mRelativeHideAds, mRelativeInfo, mRelativePlayingWithShadow, mRelativePlaying;

    private GestureDetector mGestureDetector;
    private int mLastY = 0;

    public IInAppBillingService getService() { return mService; }
    public void setPlayNextByBPos(boolean playNextByBPos) { mPlayNextByBPos = playNextByBPos; }
    public boolean isPlayNextByBPos() { return mPlayNextByBPos; }
    public void setWaitEnd(boolean waitEnd) { mWaitEnd = waitEnd; }
    public HoldableViewPager getViewPager() { return mViewPager; }
    public float getDensity() { return mDensity; }
    public SeekBar getSeekCurPos() { return mSeekCurPos; }
    public TextView getTextCurPos() { return mTextCurPos; }
    public TextView getTextRemain() { return mTextRemain; }
    public void setLoopA(boolean loopA) { mLoopA = loopA; }
    public boolean isLoopA() { return mLoopA; }
    public void setLoopB(boolean loopB) { mLoopB = loopB; }
    public boolean isLoopB() { return mLoopB; }
    public double getLoopAPos() { return mLoopAPos; }
    public void setLoopAPos(double loopAPos) { mLoopAPos = loopAPos; }
    public double getLoopBPos() { return mLoopBPos; }
    public void setLoopBPos(double loopBPos) { mLoopBPos = loopBPos; }
    public void setLength(double length) { mLength = length; }
    public double getLength() { return mLength; }
    public void setByteLength(long byteLength) { mByteLength = byteLength; }
    public long getByteLength() { return mByteLength; }
    public AnimationButton getBtnPlay() { return mBtnPlay; }
    public AnimationButton getBtnPlayInPlayingBar() { return mBtnPlayInPlayingBar; }
    public AnimationButton getBtnArtworkInPlayingBar() { return mBtnArtworkInPlayingBar; }
    public AnimationButton getBtnShuffle() { return mBtnShuffle; }
    public AnimationButton getBtnRepeat() { return mBtnRepeat; }
    public AnimationButton getBtnRecord() { return mBtnRecord; }
    public RelativeLayout getRelativePlayingWithShadow() { return mRelativePlayingWithShadow; }
    public RelativeLayout getRelativeRecording() { return mRelativeRecording; }
    public View getViewSep1() { return mViewSep1; }
    public TextView getTextRecordingTime() { return mTextRecordingTime; }
    public AnimationButton getBtnStopRecording() { return mBtnStopRecording; }

    public MainActivity() { }

    static class FileProcsParams {
        AssetFileDescriptor assetFileDescriptor = null;
        FileChannel fileChannel = null;
        InputStream inputStream = null;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        mDensity = getResources().getDisplayMetrics().density;

        MainActivity.sStream = 0;

        setContentView(R.layout.activity_main);

        mBtnShuffle = findViewById(R.id.btnShuffle);
        mBtnRepeat = findViewById(R.id.btnRepeat);
        mBtnRecord = findViewById(R.id.btnRecord);
        mDrawerLayout = findViewById(R.id.drawer_layout);
        mBtnPlay = findViewById(R.id.btnPlay);
        mBtnPlayInPlayingBar = findViewById(R.id.btnPlayInPlayingBar);
        mBtnForward = findViewById(R.id.btnForward);
        mBtnForwardInPlayingBar = findViewById(R.id.btnForwardInPlayingBar);
        mBtnRewind = findViewById(R.id.btnRewind);
        mBtnRewindInPlayingBar = findViewById(R.id.btnRewindInPlayingBar);
        mBtnCloseInPlayingBar = findViewById(R.id.btnCloseInPlayingBar);
        mBtnShuffleInPlayingBar = findViewById(R.id.btnShuffleInPlayingBar);
        mBtnRepeatInPlayingBar = findViewById(R.id.btnRepeatInPlayingBar);
        mBtnMoreInPlayingBar = findViewById(R.id.btnMoreInPlayingBar);
        mRelativePlaying = findViewById(R.id.relativePlaying);
        mSeekCurPos = findViewById(R.id.seekCurPos);
        mImgViewDown = findViewById(R.id.imgViewDown);
        mBtnArtworkInPlayingBar = findViewById(R.id.btnArtworkInPlayingBar);
        mRelativePlayingWithShadow = findViewById(R.id.relativePlayingWithShadow);
        mTextCurPos = findViewById(R.id.textCurPos);
        mTextRemain = findViewById(R.id.textRemain);
        mTabLayout = findViewById(R.id.tabs);
        mViewSep1 = findViewById(R.id.viewSep1);
        mViewSep2 = findViewById(R.id.viewSep2);
        mTextTitle = findViewById(R.id.textTitleInPlayingBar);
        mTextArtist = findViewById(R.id.textArtistInPlayingBar);
        mRelativeRecording = findViewById(R.id.relativeRecording);
        mLinearControl = findViewById(R.id.linearControl);
        mRelativeSave = findViewById(R.id.relativeSave);
        mRelativeLock = findViewById(R.id.relativeLock);
        mRelativeAddSong = findViewById(R.id.relativeAddSong);
        mRelativeItem = findViewById(R.id.relativeItem);
        mRelativeReport = findViewById(R.id.relativeReport);
        mRelativeReview = findViewById(R.id.relativeReview);
        mRelativeHideAds = findViewById(R.id.relativeHideAds);
        mRelativeInfo = findViewById(R.id.relativeInfo);
        mTextRecordingTime = findViewById(R.id.textRecordingTime);
        mBtnStopRecording = findViewById(R.id.btnStopRecording);
        AnimationButton btnSetting = findViewById(R.id.btnSetting);

        initialize(savedInstanceState);
        loadData();

        Intent intent = getIntent();
        if(intent != null && intent.getType() != null) {
            if(intent.getType().contains("audio/")) {
                if(Build.VERSION.SDK_INT < 16)
                {
                    Uri uri = copyFile(intent.getData());
                    playlistFragment.addSong(this, uri);
                }
                else
                {
                    if(intent.getClipData() == null)
                    {
                        Uri uri = copyFile(intent.getData());
                        playlistFragment.addSong(this, uri);
                    }
                    else
                    {
                        for(int i = 0; i < intent.getClipData().getItemCount(); i++)
                        {
                            Uri uri = copyFile(intent.getClipData().getItemAt(i).getUri());
                            playlistFragment.addSong(this, uri);
                        }
                    }
                }
                if(playlistFragment.getSongsAdapter() != null)
                    playlistFragment.getSongsAdapter().notifyDataSetChanged();
                SharedPreferences preferences = getSharedPreferences("SaveData", Activity.MODE_PRIVATE);
                Gson gson = new Gson();
                preferences.edit().putString("arPlaylists", gson.toJson(playlistFragment.getPlaylists())).apply();
                preferences.edit().putString("arEffects", gson.toJson(playlistFragment.getEffects())).apply();
                preferences.edit().putString("arLyrics", gson.toJson(playlistFragment.getLyrics())).apply();
                preferences.edit().putString("arPlaylistNames", gson.toJson(playlistFragment.getPlaylistNames())).apply();
            }
        }

        mServiceConn = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                mService = IInAppBillingService.Stub.asInterface(service);
                mBound = true;
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                mService = null;
                mBound = false;
            }
        };
        Intent serviceIntent =
                new Intent("com.android.vending.billing.InAppBillingService.BIND");
        serviceIntent.setPackage("com.android.vending");
        if (!mBound) {
            bindService(serviceIntent, mServiceConn, Context.BIND_AUTO_CREATE);
            mBound = true;
        }

        mBtnShuffle.setOnClickListener(this);
        mBtnRepeat.setOnClickListener(this);

        mDrawerLayout.addDrawerListener(this);
        mRelativeSave.setOnTouchListener(this);
        mRelativeSave.setOnClickListener(this);
        mRelativeLock.setOnTouchListener(this);
        mRelativeLock.setOnClickListener(this);
        mRelativeAddSong.setOnTouchListener(this);
        mRelativeAddSong.setOnClickListener(this);
        mRelativeItem.setOnTouchListener(this);
        mRelativeItem.setOnClickListener(this);
        mRelativeReport.setOnTouchListener(this);
        mRelativeReport.setOnClickListener(this);
        mRelativeReview.setOnTouchListener(this);
        mRelativeReview.setOnClickListener(this);
        mRelativeHideAds.setOnTouchListener(this);
        mRelativeHideAds.setOnClickListener(this);
        mRelativeInfo.setOnTouchListener(this);
        mRelativeInfo.setOnClickListener(this);
        btnSetting.setOnClickListener(this);

        mBtnPlayInPlayingBar.setOnClickListener(this);
        mBtnForwardInPlayingBar.setOnClickListener(this);
        mBtnForwardInPlayingBar.setOnLongClickListener(this);
        mBtnForwardInPlayingBar.setOnTouchListener(this);
        mBtnRewindInPlayingBar.setOnClickListener(this);
        mBtnRewindInPlayingBar.setOnLongClickListener(this);
        mBtnRewindInPlayingBar.setOnTouchListener(this);
        mBtnCloseInPlayingBar.setOnClickListener(this);
        mBtnShuffleInPlayingBar.setOnClickListener(this);
        mBtnRepeatInPlayingBar.setOnClickListener(this);
        mBtnMoreInPlayingBar.setOnClickListener(this);
        mSeekCurPos.getProgressDrawable().setColorFilter(Color.parseColor("#A0A0A0"), PorterDuff.Mode.SRC_IN);
        mSeekCurPos.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                loopFragment.setCurPos((double)seekBar.getProgress());
            }
        });
        mImgViewDown.setOnClickListener(this);
        mImgViewDown.setOnTouchListener(this);
        mRelativePlaying.setOnClickListener(this);
        mGestureDetector = new GestureDetector(this, new SingleTapConfirm());
        mRelativePlaying.setOnTouchListener(this);

        mBtnArtworkInPlayingBar.setAnimation(false);
        mBtnArtworkInPlayingBar.setClickable(false);

        mDrawerLayout.setScrimColor(Color.argb(102, 0, 0, 0));

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            MainActivity.setSystemBarTheme(this, false);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public static void setSystemBarTheme(final Activity pActivity, final boolean pIsDark) {
        final int lFlags = pActivity.getWindow().getDecorView().getSystemUiVisibility();
        pActivity.getWindow().getDecorView().setSystemUiVisibility(pIsDark ? (lFlags & ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR) : (lFlags | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR));
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);

        if(playlistFragment.isAdded())
            getSupportFragmentManager().putFragment(savedInstanceState, "playlistFragment", playlistFragment);
        if(loopFragment.isAdded())
            getSupportFragmentManager().putFragment(savedInstanceState, "loopFragment", loopFragment);
        if(controlFragment.isAdded())
            getSupportFragmentManager().putFragment(savedInstanceState, "controlFragment", controlFragment);
        if(equalizerFragment.isAdded())
            getSupportFragmentManager().putFragment(savedInstanceState, "equalizerFragment", equalizerFragment);
        if(effectFragment.isAdded())
            getSupportFragmentManager().putFragment(savedInstanceState, "effectFragment", effectFragment);
    }

    private class SingleTapConfirm extends GestureDetector.SimpleOnGestureListener
    {
        @Override
        public boolean onSingleTapUp(MotionEvent event)
        {
            return true;
        }
    }

    private void advanceAnimation(View view, String strTarget, int nFrom, int nTo, float fProgress)
    {
        RelativeLayout.LayoutParams param = (RelativeLayout.LayoutParams)view.getLayoutParams();
        switch (strTarget) {
            case "height":
                param.height = (int) (nFrom + (nTo - nFrom) * fProgress);
                break;
            case "width":
                param.width = (int) (nFrom + (nTo - nFrom) * fProgress);
                break;
            case "leftMargin":
                param.leftMargin = (int) (nFrom + (nTo - nFrom) * fProgress);
                break;
            case "topMargin":
                param.topMargin = (int) (nFrom + (nTo - nFrom) * fProgress);
                break;
            case "rightMargin":
                param.rightMargin = (int) (nFrom + (nTo - nFrom) * fProgress);
                break;
            case "bottomMargin":
                param.bottomMargin = (int) (nFrom + (nTo - nFrom) * fProgress);
                break;
        }
    }

    private int getStatusBarHeight(){
        final Rect rect = new Rect();
        getWindow().getDecorView().getWindowVisibleDisplayFrame(rect);
        return rect.top;
    }

    @Override
    public void onDrawerOpened(@NonNull View drawerView)
    {
    }

    @Override
    public void onDrawerClosed(@NonNull View drawerView)
    {
    }

    @Override
    public void onDrawerSlide(@NonNull View drawerView, float slideOffset)
    {
    }

    @Override
    public void onDrawerStateChanged(int newState)
    {
        if(newState == DrawerLayout.STATE_IDLE)
        {
            mRelativeSave.setBackgroundColor(Color.argb(255, 255, 255, 255));
            mRelativeLock.setBackgroundColor(Color.argb(255, 255, 255, 255));
            mRelativeAddSong.setBackgroundColor(Color.argb(255, 255, 255, 255));
            mRelativeHideAds.setBackgroundColor(Color.argb(255, 255, 255, 255));
            mRelativeItem.setBackgroundColor(Color.argb(255, 255, 255, 255));
            mRelativeReport.setBackgroundColor(Color.argb(255, 255, 255, 255));
            mRelativeReview.setBackgroundColor(Color.argb(255, 255, 255, 255));
            mRelativeInfo.setBackgroundColor(Color.argb(255, 255, 255, 255));
        }
    }

    public Uri copyFile(Uri uri)
    {
        int i = 0;
        String strPath;
        File file;
        while(true) {
            strPath = getFilesDir() + "/copied" + String.format(Locale.getDefault(), "%d", i);
            file = new File(strPath);
            if(!file.exists()) break;
            i++;
        }
        try {
            InputStream in;
            if(uri.getScheme() != null && uri.getScheme().equals("content"))
                in = getContentResolver().openInputStream(uri);
            else
                in = new FileInputStream(uri.toString());
            FileOutputStream out = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int len;
            if(in != null) {
                while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
                in.close();
            }
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return Uri.parse(strPath);
    }

    public Uri copyTempFile(Uri uri)
    {
        int i = 0;
        String strPath;
        File file;
        while(true) {
            strPath = getExternalCacheDir() + "/copied" + String.format(Locale.getDefault(), "%d", i);
            file = new File(strPath);
            if(!file.exists()) break;
            i++;
        }
        try {
            InputStream in;
            if(uri.getScheme() != null && uri.getScheme().equals("content"))
                in = getContentResolver().openInputStream(uri);
            else
                in = new FileInputStream(uri.toString());
            FileOutputStream out = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int len;
            if(in != null) {
                while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
                in.close();
            }
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return Uri.parse(strPath);
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if(mReceiver == null) {
            mReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (intent.getAction() == null) return;
                    if (intent.getAction().equals("action_rewind")) {
                        playlistFragment.onRewindBtnClick();
                        return;
                    }
                    if (intent.getAction().equals("action_playpause")) {
                        playlistFragment.onPlayBtnClick();
                        return;
                    }
                    if (intent.getAction().equals("action_forward")) {
                        playlistFragment.onForwardBtnClick();
                        return;
                    } else if (intent.getAction().equals(AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
                        if (BASS.BASS_ChannelIsActive(sStream) == BASS.BASS_ACTIVE_PLAYING)
                            playlistFragment.pause();
                        return;
                    }
                    try {
                        Bundle ownedItems = mService.getPurchases(3, getPackageName(), "inapp", null);
                        if (ownedItems != null && ownedItems.getInt("RESPONSE_CODE") == 0) {
                            ArrayList<String> ownedSkus =
                                    ownedItems.getStringArrayList("INAPP_PURCHASE_ITEM_LIST");
                            ArrayList<String> purchaseDataList =
                                    ownedItems.getStringArrayList("INAPP_PURCHASE_DATA_LIST");
                            if (purchaseDataList != null && ownedSkus != null) {
                                for (int i = 0; i < purchaseDataList.size(); i++) {
                                    String sku = ownedSkus.get(i);

                                    if (sku.equals("hideads")) {
                                        hideAds();
                                    }
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        finish();
                    }
                }
            };
            registerReceiver(mReceiver, new IntentFilter("action_rewind"));
            registerReceiver(mReceiver, new IntentFilter("action_playpause"));
            registerReceiver(mReceiver, new IntentFilter("action_forward"));
            registerReceiver(mReceiver, new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
            registerReceiver(mReceiver, new IntentFilter("com.android.vending.billing.PURCHASES_UPDATED"));
        }

        try {
            Bundle ownedItems = mService.getPurchases(3, getPackageName(), "inapp", null);
            if(ownedItems != null && ownedItems.getInt("RESPONSE_CODE") == 0) {
                ArrayList<String> ownedSkus =
                        ownedItems.getStringArrayList("INAPP_PURCHASE_ITEM_LIST");
                ArrayList<String> purchaseDataList =
                        ownedItems.getStringArrayList("INAPP_PURCHASE_DATA_LIST");
                if(purchaseDataList != null && ownedSkus != null) {
                    for (int i = 0; i < purchaseDataList.size(); i++) {
                        String sku = ownedSkus.get(i);

                        if (sku.equals("hideads"))
                            hideAds();
                    }
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(sStream == 0 && mReceiver != null) {
            try {
                unregisterReceiver(mReceiver);
            }
            catch(Exception e) {
                e.printStackTrace();
            }
            mReceiver = null;
        }
    }

    public void startNotification()
    {
        if(playlistFragment.getPlayingPlaylist() < 0 && playlistFragment.getPlaylists().size() <= playlistFragment.getPlayingPlaylist())
            return;
        ArrayList<SongItem> arSongs = playlistFragment.getPlaylists().get(playlistFragment.getPlayingPlaylist());
        if(playlistFragment.getPlaying() < 0 && arSongs.size() <= playlistFragment.getPlaying())
            return;
        int playing = playlistFragment.getPlaying();
        if(playing < 0 || arSongs.size() <= playing) return;
        SongItem item = arSongs.get(playing);
        Intent intent = new Intent(this, ForegroundService.class);
        intent.putExtra("strTitle", item.getTitle());
        intent.putExtra("strArtist", item.getArtist());
        intent.putExtra("strPathArtwork", item.getPathArtwork());
        intent.putExtra("strPath", item.getPath());
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent);
        else startService(intent);
    }

    public void stopNotification()
    {
        Intent intent = new Intent(this, ForegroundService.class);
        stopService(intent);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        if(mShowUpdateLog) {
            mShowUpdateLog = false;

            LayoutInflater inflater = getLayoutInflater();
            final View layout = inflater.inflate(R.layout.updatelogdialog,
                    (ViewGroup)findViewById(R.id.layout_root));
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            TextView textUpdatelogTitle = layout.findViewById(R.id.textUpdatelogTitle);
            try {
                textUpdatelogTitle.setText(String.format(Locale.getDefault(), "ハヤえもんAndroid版ver.%sに\nアップデートされました！", getApplicationContext().getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), 0).versionName));
            }
            catch(PackageManager.NameNotFoundException e) {
                textUpdatelogTitle.setText("ハヤえもんAndroid版が\nアップデートされました！");
            }
            builder.setView(layout);

            TextView textViewBlog = layout.findViewById(R.id.textViewBlog);
            String strBlog = "この内容は<a href=\"http://hayaemon.jp/blog/\">開発者ブログ</a>から";
            CharSequence blogChar = Html.fromHtml(strBlog);
            textViewBlog.setText(blogChar);
            MovementMethod mMethod = LinkMovementMethod.getInstance();
            textViewBlog.setMovementMethod(mMethod);
            textViewBlog.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));

            TextView textViewArticle = layout.findViewById(R.id.textViewArticle);
            String strArticle = "<a href=\"http://hayaemon.jp/blog/archives/7261\">→該当記事へ</a>";
            CharSequence blogChar2 = Html.fromHtml(strArticle);
            textViewArticle.setText(blogChar2);
            textViewArticle.setMovementMethod(mMethod);

            TextView textView = layout.findViewById(R.id.textView);
            textView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            textView.setMovementMethod(ScrollingMovementMethod.getInstance());
            textView.setText(readChangeLog());

            final AlertDialog alertDialog = builder.create();
            alertDialog.setOnShowListener(new DialogInterface.OnShowListener()
            {
                @Override
                public void onShow(DialogInterface arg0)
                {
                    if(alertDialog.getWindow() != null) {
                        alertDialog.getWindow().getDecorView().getBackground().setColorFilter(Color.parseColor("#00000000"), PorterDuff.Mode.SRC_IN);
                        WindowManager.LayoutParams lp = alertDialog.getWindow().getAttributes();
                        lp.dimAmount = 0.4f;
                        alertDialog.getWindow().setAttributes(lp);
                    }
                }
            });
            alertDialog.show();
            Button btnClose = layout.findViewById(R.id.btnClose);
            btnClose.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Switch switchNextHidden = layout.findViewById(R.id.switchNextHidden);
                    boolean bChecked = switchNextHidden.isChecked();
                    SharedPreferences preferences = getSharedPreferences("SaveData", Activity.MODE_PRIVATE);
                    preferences.edit().putBoolean("hideupdatelognext", bChecked).apply();
                    alertDialog.dismiss();
                }
            });
            Button btnShare = layout.findViewById(R.id.btnShare);
            btnShare.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent sendIntent = new Intent();
                    sendIntent.setAction(Intent.ACTION_SEND);
                    try {
                        sendIntent.putExtra(Intent.EXTRA_TEXT, "ハヤえもんAndroid版ver." + getApplicationContext().getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), 0).versionName + "にアップデートしました！ https://bit.ly/2D3jY89");
                    }
                    catch(PackageManager.NameNotFoundException e) {
                        sendIntent.putExtra(Intent.EXTRA_TEXT, "ハヤえもんAndroid版をアップデートしました！ https://bit.ly/2D3jY89");
                    }
                    sendIntent.setType("*/*");
                    File file = getScreenshot(layout.getRootView());
                    Uri uri = FileProvider.getUriForFile(getApplicationContext(), "com.edolfzoku.hayaemon2", file);
                    int flag;
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flag = PackageManager.MATCH_ALL;
                    else flag = PackageManager.MATCH_DEFAULT_ONLY;
                    List<ResolveInfo> resInfoList = getApplicationContext().getPackageManager().queryIntentActivities(sendIntent, flag);
                    for (ResolveInfo resolveInfo : resInfoList) {
                        String packageName = resolveInfo.activityInfo.packageName;
                        getApplicationContext().grantUriPermission(packageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    }
                    sendIntent.putExtra(Intent.EXTRA_STREAM, uri);
                    startActivity(sendIntent);

                    file.deleteOnExit();
                }
            });
        }
    }

    private File getScreenshot(View view) {
        view.setDrawingCacheEnabled(true);

        // Viewのキャッシュを取得
        Bitmap cache = view.getDrawingCache();
        Bitmap screenShot = Bitmap.createBitmap(cache);
        view.setDrawingCacheEnabled(false);

        File file = new File(getExternalCacheDir() + "/export/capture.jpeg");
        if(!file.getParentFile().mkdir()) System.out.println("ディレクトリが作成できませんでした");
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file, false);
            // 画像のフォーマットと画質と出力先を指定して保存
            screenShot.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ie) {
                    ie.printStackTrace();
                }
            }
        }
        return file;
    }

    private void loadData()
    {
        SharedPreferences preferences = getSharedPreferences("SaveData", Activity.MODE_PRIVATE);
        Gson gson = new Gson();
        ArrayList<ArrayList<SongItem>> arPlaylists = gson.fromJson(preferences.getString("arPlaylists",""), new TypeToken<ArrayList<ArrayList<SongItem>>>(){}.getType());
        ArrayList<ArrayList<EffectSaver>> arEffects = gson.fromJson(preferences.getString("arEffects",""), new TypeToken<ArrayList<ArrayList<EffectSaver>>>(){}.getType());
        ArrayList<ArrayList<String>> arLyrics = gson.fromJson(preferences.getString("arLyrics",""), new TypeToken<ArrayList<ArrayList<String>>>(){}.getType());
        ArrayList<String> arPlaylistNames = gson.fromJson(preferences.getString("arPlaylistNames",""), new TypeToken<ArrayList<String>>(){}.getType());
        List<String> arSongsPath = gson.fromJson(preferences.getString("arSongsPath",""), new TypeToken<List<String>>(){}.getType());
        if(arPlaylists != null && arPlaylistNames != null) {
            for(int i = 0; i < arPlaylists.size(); i++) {
                playlistFragment.setPlaylists(arPlaylists);
                playlistFragment.setPlaylistNames(arPlaylistNames);
            }
            if(arEffects != null && arPlaylists.size() == arEffects.size())
                playlistFragment.setEffects(arEffects);
            else {
                arEffects = playlistFragment.getEffects();
                for(int i = 0; i < arPlaylists.size(); i++) {
                    ArrayList<EffectSaver> arEffectSavers = new ArrayList<>();
                    ArrayList<SongItem> arSongs = arPlaylists.get(i);
                    for(int j = 0; j < arSongs.size(); j++) {
                        EffectSaver saver = new EffectSaver();
                        arEffectSavers.add(saver);
                    }
                    arEffects.add(arEffectSavers);
                }
            }
            if(arLyrics != null && arPlaylists.size() == arLyrics.size())
                playlistFragment.setLyrics(arLyrics);
            else {
                arLyrics = playlistFragment.getLyrics();
                for(int i = 0; i < arPlaylists.size(); i++) {
                    ArrayList<String> arTempLyrics = new ArrayList<>();
                    ArrayList<SongItem> arSongs = arPlaylists.get(i);
                    for(int j = 0; j < arSongs.size(); j++) {
                        arTempLyrics.add(null);
                    }
                    arLyrics.add(arTempLyrics);
                }
            }
        }
        else if(arSongsPath != null) {
            playlistFragment.addPlaylist(String.format(Locale.getDefault(), "%s 1", getString(R.string.playlist)));
            playlistFragment.addPlaylist(String.format(Locale.getDefault(), "%s 2", getString(R.string.playlist)));
            playlistFragment.addPlaylist(String.format(Locale.getDefault(), "%s 3", getString(R.string.playlist)));
            playlistFragment.selectPlaylist(0);
            for(int i = 0; i < arSongsPath.size(); i++) {
                playlistFragment.addSong(this, Uri.parse(arSongsPath.get(i)));
            }
        }
        else {
            playlistFragment.addPlaylist(String.format(Locale.getDefault(), "%s 1", getString(R.string.playlist)));
            playlistFragment.addPlaylist(String.format(Locale.getDefault(), "%s 2", getString(R.string.playlist)));
            playlistFragment.addPlaylist(String.format(Locale.getDefault(), "%s 3", getString(R.string.playlist)));
            playlistFragment.selectPlaylist(0);
        }

        String strVersionName = preferences.getString("versionname", null);
        boolean bHideUpdateLogNext = preferences.getBoolean("hideupdatelognext", false);
        String strCurrentVersionName;
        try {
            strCurrentVersionName = getApplicationContext().getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), 0).versionName;
        }
        catch(PackageManager.NameNotFoundException e) {
            strCurrentVersionName = strVersionName;
        }
        if(!bHideUpdateLogNext && Locale.getDefault().equals(Locale.JAPAN))
        {
            if(strVersionName != null && !strCurrentVersionName.equals(strVersionName))
                mShowUpdateLog = true;
        }
        if(strVersionName == null) {
            preferences.edit().putBoolean("bPinkCamperDisplayed", true).apply();
            preferences.edit().putBoolean("bBlueCamperDisplayed", true).apply();
            preferences.edit().putBoolean("bOrangeCamperDisplayed", true).apply();
        }
        preferences.edit().putString("versionname", strCurrentVersionName).apply();

        mPlayNextByBPos = preferences.getBoolean("bPlayNextByBPos", false);
        boolean bSnap = preferences.getBoolean("bSnap", false);
        controlFragment.setSnap(bSnap);
        controlFragment.setMinSpeed(preferences.getInt("nMinSpeed", 10));
        controlFragment.setMaxSpeed(preferences.getInt("nMaxSpeed", 400));
        controlFragment.setMinPitch(preferences.getInt("nMinPitch", -12));
        controlFragment.setMaxPitch(preferences.getInt("nMaxPitch", 12));

        boolean bHideAds = preferences.getBoolean("hideads", false);
        if(bHideAds) hideAds();
        else
        {
            try {
                Bundle ownedItems = mService.getPurchases(3, getPackageName(), "inapp", null);
                if(ownedItems.getInt("RESPONSE_CODE") == 0)
                {
                    ArrayList<String> ownedSkus =
                            ownedItems.getStringArrayList("INAPP_PURCHASE_ITEM_LIST");
                    ArrayList<String> purchaseDataList =
                            ownedItems.getStringArrayList("INAPP_PURCHASE_DATA_LIST");
                    if(purchaseDataList != null && ownedSkus != null) {
                        for (int i = 0; i < purchaseDataList.size(); i++) {
                            String sku = ownedSkus.get(i);

                            if (sku.equals("hideads"))
                                hideAds();
                        }
                    }
                }
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }

        int nShuffle = preferences.getInt("shufflemode", 0);
        if(nShuffle == 1)
        {
            mBtnShuffle.setContentDescription(getString(R.string.shuffleOn));
            mBtnShuffle.setImageResource(R.drawable.ic_bar_button_mode_shuffle_on);
            mBtnShuffleInPlayingBar.setContentDescription(getString(R.string.shuffleOn));
            mBtnShuffleInPlayingBar.setImageResource(R.drawable.ic_playing_large_mode_shuffle_on);
        }
        else if(nShuffle == 2)
        {
            mBtnShuffle.setContentDescription(getString(R.string.singleOn));
            mBtnShuffle.setImageResource(R.drawable.ic_bar_button_mode_single_on);
            mBtnShuffleInPlayingBar.setContentDescription(getString(R.string.singleOn));
            mBtnShuffleInPlayingBar.setImageResource(R.drawable.ic_playing_large_mode_single_on);
        }
        else
        {
            mBtnShuffle.setContentDescription(getString(R.string.shuffleOff));
            mBtnShuffle.setImageResource(R.drawable.ic_bar_button_mode_shuffle);
            mBtnShuffleInPlayingBar.setContentDescription(getString(R.string.shuffleOff));
            mBtnShuffleInPlayingBar.setImageResource(R.drawable.ic_playing_large_mode_shuffle);
        }

        int nRepeat = preferences.getInt("repeatmode", 0);
        if(nRepeat == 1)
        {
            mBtnRepeat.setContentDescription(getString(R.string.repeatAllOn));
            mBtnRepeat.setImageResource(R.drawable.ic_bar_button_mode_repeat_all_on);
            mBtnRepeatInPlayingBar.setContentDescription(getString(R.string.repeatAllOn));
            mBtnRepeatInPlayingBar.setImageResource(R.drawable.ic_playing_large_mode_repeat_all_on);
        }
        else if(nRepeat == 2)
        {
            mBtnRepeat.setContentDescription(getString(R.string.repeatSingleOn));
            mBtnRepeat.setImageResource(R.drawable.ic_bar_button_mode_repeat_single_on);
            mBtnRepeatInPlayingBar.setContentDescription(getString(R.string.repeatSingleOn));
            mBtnRepeatInPlayingBar.setImageResource(R.drawable.ic_playing_large_mode_repeat_one_on);
        }
        else
        {
            mBtnRepeat.setContentDescription(getString(R.string.repeatOff));
            mBtnRepeat.setImageResource(R.drawable.ic_bar_button_mode_repeat);
            mBtnRepeatInPlayingBar.setContentDescription(getString(R.string.repeatOff));
            mBtnRepeatInPlayingBar.setImageResource(R.drawable.ic_playing_large_mode_repeat_all);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        if (requestCode == 1)
        {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                playlistFragment.startRecord();
            else
            {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.permitMicError);
                builder.setMessage(R.string.permitMicErrorDetail);
                builder.setPositiveButton("OK", null);
                final AlertDialog alertDialog = builder.create();
                alertDialog.setOnShowListener(new DialogInterface.OnShowListener()
                {
                    @Override
                    public void onShow(DialogInterface arg0)
                    {
                        if(alertDialog.getWindow() != null) {
                            WindowManager.LayoutParams lp = alertDialog.getWindow().getAttributes();
                            lp.dimAmount = 0.4f;
                            alertDialog.getWindow().setAttributes(lp);
                        }
                    }
                });
                alertDialog.show();
            }
        }
    }

    @Override
    public boolean onLongClick(View v)
    {
        if(v.getId() == R.id.btnRewind || v.getId() == R.id.btnRewindInPlayingBar)
        {
            if(sStream == 0) return false;
            int chan = BASS_FX.BASS_FX_TempoGetSource(sStream);
            if(effectFragment.isReverse())
                BASS.BASS_ChannelSetAttribute(chan, BASS_FX.BASS_ATTRIB_REVERSE_DIR, BASS_FX.BASS_FX_RVS_FORWARD);
            else
                BASS.BASS_ChannelSetAttribute(chan, BASS_FX.BASS_ATTRIB_REVERSE_DIR, BASS_FX.BASS_FX_RVS_REVERSE);
            BASS.BASS_ChannelSetAttribute(sStream, BASS_FX.BASS_ATTRIB_TEMPO, controlFragment.getSpeed() + 100);
            mBtnRewind.setColorFilter(new PorterDuffColorFilter(Color.parseColor("#FF007AFF"), PorterDuff.Mode.SRC_IN));
            mBtnRewindInPlayingBar.setColorFilter(new PorterDuffColorFilter(Color.parseColor("#FF007AFF"), PorterDuff.Mode.SRC_IN));
            return true;
        }
        else if(v.getId() == R.id.btnForward || v.getId() == R.id.btnForwardInPlayingBar)
        {
            if(sStream == 0) return false;
            BASS.BASS_ChannelSetAttribute(sStream, BASS_FX.BASS_ATTRIB_TEMPO, controlFragment.getSpeed() + 100);
            mBtnForward.setColorFilter(new PorterDuffColorFilter(Color.parseColor("#FF007AFF"), PorterDuff.Mode.SRC_IN));
            mBtnForwardInPlayingBar.setColorFilter(new PorterDuffColorFilter(Color.parseColor("#FF007AFF"), PorterDuff.Mode.SRC_IN));
            return true;
        }
        return false;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event)
    {
        if(v.getId() == R.id.relativePlaying || v.getId() == R.id.imgViewDown)
        {
            int nY = (int) event.getRawY();
            if (mGestureDetector.onTouchEvent(event)) return false;
            if(mSeekCurPos.getVisibility() != View.VISIBLE) {
                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    RelativeLayout.LayoutParams paramContainer = (RelativeLayout.LayoutParams) mViewPager.getLayoutParams();
                    RelativeLayout.LayoutParams paramRecording = (RelativeLayout.LayoutParams) mRelativeRecording.getLayoutParams();
                    if (MainActivity.sRecord != 0) {
                        paramContainer.addRule(RelativeLayout.ABOVE, R.id.relativeRecording);
                        paramContainer.bottomMargin = 0;
                        paramRecording.addRule(RelativeLayout.ABOVE, R.id.adView);
                        paramRecording.bottomMargin = (int) (60.0 * mDensity);
                    } else {
                        paramContainer.addRule(RelativeLayout.ABOVE, R.id.adView);
                        paramContainer.bottomMargin = (int) (60.0 * mDensity);
                    }
                    RelativeLayout.LayoutParams param = (RelativeLayout.LayoutParams) mRelativePlayingWithShadow.getLayoutParams();
                    int nHeight = param.height - (nY - mLastY);
                    int nMinHeight = (int) (82.0 * mDensity);
                    int nMaxHeight = (int) (142.0 * mDensity);
                    if (nHeight < nMinHeight) nHeight = nMinHeight;
                    else if (nHeight > nMaxHeight) nHeight = nMaxHeight;
                    param.height = nHeight;
                    mRelativePlayingWithShadow.setLayoutParams(param);
                }
                mLastY = nY;
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    RelativeLayout.LayoutParams param = (RelativeLayout.LayoutParams) mRelativePlayingWithShadow.getLayoutParams();
                    int nMinHeight = (int) (82.0 * mDensity);
                    if (param.height > nMinHeight) return false;
                    else {
                        RelativeLayout.LayoutParams paramContainer = (RelativeLayout.LayoutParams) mViewPager.getLayoutParams();
                        RelativeLayout.LayoutParams paramRecording = (RelativeLayout.LayoutParams) mRelativeRecording.getLayoutParams();
                        if (MainActivity.sRecord != 0) {
                            paramContainer.addRule(RelativeLayout.ABOVE, R.id.relativeRecording);
                            paramContainer.bottomMargin = 0;
                            paramRecording.addRule(RelativeLayout.ABOVE, R.id.relativePlayingWithShadow);
                            paramRecording.bottomMargin = (int) (-22 * mDensity);
                        } else {
                            paramContainer.addRule(RelativeLayout.ABOVE, R.id.relativePlayingWithShadow);
                            paramContainer.bottomMargin = (int) (-22 * mDensity);
                        }
                    }
                }
            }
            else {
                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    final int nCurrentHeight = getResources().getDisplayMetrics().heightPixels - mTabLayout.getHeight() - mLinearControl.getHeight() - getStatusBarHeight() + (int) (16.0 * mDensity);
                    final int nMaxHeight = getResources().getDisplayMetrics().heightPixels - mTabLayout.getHeight() - getStatusBarHeight() + (int) (22.0 * mDensity);
                    final int nMinHeight = (int) (82.0 * mDensity);
                    int nMinTranslationY = nCurrentHeight - nMaxHeight;
                    int nMaxTranslationY = nCurrentHeight - nMinHeight;
                    int nTranslationY = nY - mLastY;
                    if(nTranslationY < nMinTranslationY) nTranslationY = nMinTranslationY;
                    else if(nTranslationY > nMaxTranslationY) nTranslationY = nMaxTranslationY;
                    mRelativePlayingWithShadow.setTranslationY(nTranslationY);
                }
                if (event.getAction() == MotionEvent.ACTION_DOWN)
                    mLastY = (int)mRelativePlayingWithShadow.getTranslationY() + nY;
                if (event.getAction() == MotionEvent.ACTION_UP) {
                    if(mRelativePlayingWithShadow.getTranslationY() > (int) (100.0 * mDensity)) {
                        downViewPlaying(false);
                    }
                    else {
                        final int nTranslationYFrom = (int)mRelativePlayingWithShadow.getTranslationY();
                        final int nTranslationY = 0;

                        ValueAnimator anim = ValueAnimator.ofFloat(0.0f, 1.0f);
                        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                            @Override
                            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                                float fProgress = valueAnimator.getAnimatedFraction();
                                mRelativePlayingWithShadow.setTranslationY(nTranslationYFrom + (nTranslationY - nTranslationYFrom) * fProgress);
                            }
                        });
                        anim.setDuration(200).start();
                    }
                }
            }
            if(event.getAction() == MotionEvent.ACTION_MOVE || event.getAction() == MotionEvent.ACTION_UP)
                return true;
            return (v.getId() == R.id.relativePlaying && mSeekCurPos.getVisibility() == View.VISIBLE);
        }

        if(event.getAction() == MotionEvent.ACTION_UP)
        {
            mRelativeSave.setBackgroundColor(Color.argb(255, 255, 255, 255));
            mRelativeLock.setBackgroundColor(Color.argb(255, 255, 255, 255));
            mRelativeAddSong.setBackgroundColor(Color.argb(255, 255, 255, 255));
            mRelativeHideAds.setBackgroundColor(Color.argb(255, 255, 255, 255));
            mRelativeItem.setBackgroundColor(Color.argb(255, 255, 255, 255));
            mRelativeReport.setBackgroundColor(Color.argb(255, 255, 255, 255));
            mRelativeReview.setBackgroundColor(Color.argb(255, 255, 255, 255));
            mRelativeInfo.setBackgroundColor(Color.argb(255, 255, 255, 255));
            if(v.getId() == R.id.btnRewind || v.getId() == R.id.btnRewindInPlayingBar)
            {
                if(sStream == 0) return false;
                int chan = BASS_FX.BASS_FX_TempoGetSource(sStream);
                if(effectFragment.isReverse())
                    BASS.BASS_ChannelSetAttribute(chan, BASS_FX.BASS_ATTRIB_REVERSE_DIR, BASS_FX.BASS_FX_RVS_REVERSE);
                else
                    BASS.BASS_ChannelSetAttribute(chan, BASS_FX.BASS_ATTRIB_REVERSE_DIR, BASS_FX.BASS_FX_RVS_FORWARD);
                BASS.BASS_ChannelSetAttribute(sStream, BASS_FX.BASS_ATTRIB_TEMPO, controlFragment.getSpeed());
                mBtnRewind.clearColorFilter();
                mBtnRewindInPlayingBar.clearColorFilter();
            }
            else if(v.getId() == R.id.btnForward || v.getId() == R.id.btnForwardInPlayingBar)
            {
                if(sStream == 0) return false;
                BASS.BASS_ChannelSetAttribute(sStream, BASS_FX.BASS_ATTRIB_TEMPO, controlFragment.getSpeed());
                mBtnForward.clearColorFilter();
                mBtnForwardInPlayingBar.clearColorFilter();
            }
        }
        if(event.getAction() == MotionEvent.ACTION_DOWN)
        {
            if(v.getId() == R.id.relativeSave)
                mRelativeSave.setBackgroundColor(Color.argb(255, 229, 229, 229));
            if(v.getId() == R.id.relativeLock)
                mRelativeLock.setBackgroundColor(Color.argb(255, 229, 229, 229));
            if(v.getId() == R.id.relativeAddSong)
                mRelativeAddSong.setBackgroundColor(Color.argb(255, 229, 229, 229));
            if(v.getId() == R.id.relativeHideAds)
                mRelativeHideAds.setBackgroundColor(Color.argb(255, 229, 229, 229));
            if(v.getId() == R.id.relativeItem)
                mRelativeItem.setBackgroundColor(Color.argb(255, 229, 229, 229));
            if(v.getId() == R.id.relativeReport)
                mRelativeReport.setBackgroundColor(Color.argb(255, 229, 229, 229));
            if(v.getId() == R.id.relativeReview)
                mRelativeReview.setBackgroundColor(Color.argb(255, 229, 229, 229));
            if(v.getId() == R.id.relativeInfo)
                mRelativeInfo.setBackgroundColor(Color.argb(255, 229, 229, 229));
        }
        return false;
    }

    @Override
    public void onClick(View v)
    {
        if(v.getId() == R.id.btnMenu)
        {
            SharedPreferences preferences = getSharedPreferences("SaveData", Activity.MODE_PRIVATE);
            boolean bPinkCamperDisplayed = preferences.getBoolean("bPinkCamperDisplayed", false);
            boolean bBlueCamperDisplayed = preferences.getBoolean("bBlueCamperDisplayed", false);
            boolean bOrangeCamperDisplayed = preferences.getBoolean("bOrangeCamperDisplayed", false);
            int nCount = 0;
            if(!bPinkCamperDisplayed) nCount++;
            if(!bBlueCamperDisplayed) nCount++;
            if(!bOrangeCamperDisplayed) nCount++;

            findViewById(R.id.textPlaying).setVisibility(sStream == 0 ? View.GONE : View.VISIBLE);
            findViewById(R.id.relativePlayingInMenu).setVisibility(sStream == 0 ? View.GONE : View.VISIBLE);
            findViewById(R.id.relativeSave).setVisibility(sStream == 0 ? View.GONE : View.VISIBLE);
            findViewById(R.id.relativeLock).setVisibility(sStream == 0 ? View.GONE : View.VISIBLE);
            findViewById(R.id.dividerMenu).setVisibility(sStream == 0 ? View.GONE : View.VISIBLE);
            TextView textView = findViewById(R.id.textItemNew);
            textView.setVisibility(nCount == 0 ? View.GONE : View.VISIBLE);
            textView.setText(String.format(Locale.getDefault(), "%d", nCount));

            if(!isAdsVisible()) findViewById(R.id.relativeHideAds).setVisibility(View.GONE);
            if(sStream != 0) {
                playlistFragment.selectPlaylist(playlistFragment.getPlayingPlaylist());
                playlistFragment.setSelectedItem(playlistFragment.getPlaying());

                SongItem item = playlistFragment.getPlaylists().get(playlistFragment.getPlayingPlaylist()).get(playlistFragment.getPlaying());
                ImageView imgViewArtworkInMenu = findViewById(R.id.imgViewArtworkInMenu);
                Bitmap bitmap = null;
                if(item.getPathArtwork() != null && !item.getPathArtwork().equals("")) {
                    bitmap = BitmapFactory.decodeFile(item.getPathArtwork());
                }
                else {
                    MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                    try {
                        mmr.setDataSource(getApplicationContext(), Uri.parse(item.getPath()));
                        byte[] data = mmr.getEmbeddedPicture();
                        if (data != null) bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                    finally {
                        mmr.release();
                    }
                }
                if(bitmap != null) imgViewArtworkInMenu.setImageBitmap(bitmap);
                else imgViewArtworkInMenu.setImageResource(R.drawable.ic_playing_large_artwork);
                TextView textTitleInMenu = findViewById(R.id.textTitleInMenu);
                textTitleInMenu.setText(item.getTitle());
                TextView textArtistInMenu = findViewById(R.id.textArtistInMenu);
                if(item.getArtist() == null || item.getArtist().equals(""))
                {
                    textArtistInMenu.setTextColor(Color.argb(255, 147, 156, 160));
                    textArtistInMenu.setText(R.string.unknownArtist);
                }
                else
                {
                    textArtistInMenu.setTextColor(Color.argb(255, 102, 102, 102));
                    textArtistInMenu.setText(item.getArtist());
                }

                ArrayList<EffectSaver> arEffectSavers = playlistFragment.getEffects().get(playlistFragment.getPlayingPlaylist());
                EffectSaver saver = arEffectSavers.get(playlistFragment.getPlaying());
                ImageView imgLock = findViewById(R.id.imgLockInMenu);
                TextView textLock = findViewById(R.id.textLock);
                if(saver.isSave()) {
                    imgLock.setImageResource(R.drawable.ic_leftmenu_playing_unlock);
                    textLock.setText(R.string.cancelRestoreEffect);
                }
                else {
                    imgLock.setImageResource(R.drawable.ic_leftmenu_playing_lock);
                    textLock.setText(R.string.restoreEffect);
                }
            }
            mDrawerLayout.openDrawer(Gravity.START);
        }
        else if(v.getId() == R.id.btnShuffle || v.getId() == R.id.btnShuffleInPlayingBar)
        {
            if(mBtnShuffle.getContentDescription().toString().equals(getString(R.string.shuffleOff))) {
                mBtnShuffle.setContentDescription(getString(R.string.shuffleOn));
                mBtnShuffle.setImageResource(R.drawable.ic_bar_button_mode_shuffle_on);
                mBtnShuffleInPlayingBar.setContentDescription(getString(R.string.shuffleOn));
                mBtnShuffleInPlayingBar.setImageResource(R.drawable.ic_playing_large_mode_shuffle_on);
                sendAccessibilityEvent(getString(R.string.shuffleOn), v);
            }
            else if(mBtnShuffle.getContentDescription().toString().equals(getString(R.string.shuffleOn))) {
                mBtnShuffle.setContentDescription(getString(R.string.singleOn));
                mBtnShuffle.setImageResource(R.drawable.ic_bar_button_mode_single_on);
                mBtnShuffleInPlayingBar.setContentDescription(getString(R.string.singleOn));
                mBtnShuffleInPlayingBar.setImageResource(R.drawable.ic_playing_large_mode_single_on);
                sendAccessibilityEvent(getString(R.string.singleOn), v);
            }
            else {
                mBtnShuffle.setContentDescription(getString(R.string.shuffleOff));
                mBtnShuffle.setImageResource(R.drawable.ic_bar_button_mode_shuffle);
                mBtnShuffleInPlayingBar.setContentDescription(getString(R.string.shuffleOff));
                mBtnShuffleInPlayingBar.setImageResource(R.drawable.ic_playing_large_mode_shuffle);
                sendAccessibilityEvent(getString(R.string.shuffleOff), v);
            }
            playlistFragment.saveFiles(false, false, false, false, true);
        }
        else if(v.getId() == R.id.btnRepeat || v.getId() == R.id.btnRepeatInPlayingBar)
        {
            if(mBtnRepeat.getContentDescription().toString().equals(getString(R.string.repeatOff))) {
                mBtnRepeat.setContentDescription(getString(R.string.repeatAllOn));
                mBtnRepeat.setImageResource(R.drawable.ic_bar_button_mode_repeat_all_on);
                mBtnRepeatInPlayingBar.setContentDescription(getString(R.string.repeatAllOn));
                mBtnRepeatInPlayingBar.setImageResource(R.drawable.ic_playing_large_mode_repeat_all_on);
                sendAccessibilityEvent(getString(R.string.repeatAllOn), v);
            }
            else if(mBtnRepeat.getContentDescription().toString().equals(getString(R.string.repeatAllOn))) {
                mBtnRepeat.setContentDescription(getString(R.string.repeatSingleOn));
                mBtnRepeat.setImageResource(R.drawable.ic_bar_button_mode_repeat_single_on);
                mBtnRepeatInPlayingBar.setContentDescription(getString(R.string.repeatSingleOn));
                mBtnRepeatInPlayingBar.setImageResource(R.drawable.ic_playing_large_mode_repeat_one_on);
                sendAccessibilityEvent(getString(R.string.repeatSingleOn), v);
            }
            else {
                mBtnRepeat.setContentDescription(getString(R.string.repeatOff));
                mBtnRepeat.setImageResource(R.drawable.ic_bar_button_mode_repeat);
                mBtnRepeatInPlayingBar.setContentDescription(getString(R.string.repeatOff));
                mBtnRepeatInPlayingBar.setImageResource(R.drawable.ic_playing_large_mode_repeat_all);
                sendAccessibilityEvent(getString(R.string.repeatOff), v);
            }
            playlistFragment.saveFiles(false, false, false, false, true);
        }
        else if(v.getId() == R.id.relativeLock)
        {
            mDrawerLayout.closeDrawer(Gravity.START);

            ArrayList<EffectSaver> arEffectSavers = playlistFragment.getEffects().get(playlistFragment.getPlayingPlaylist());
            EffectSaver saver = arEffectSavers.get(playlistFragment.getPlaying());
            if(saver.isSave()) {
                saver.setSave(false);
                playlistFragment.getSongsAdapter().notifyItemChanged(playlistFragment.getPlaying());

                playlistFragment.saveFiles(false, true, false, false, false);
            }
            else {
                playlistFragment.setSavingEffect();
                playlistFragment.getSongsAdapter().notifyItemChanged(playlistFragment.getPlaying());
            }
        }
        else if(v.getId() == R.id.relativeSave)
        {
            mDrawerLayout.closeDrawer(Gravity.START);
            showSaveExportMenu();
        }
        else if(v.getId() == R.id.relativeAddSong)
        {
            mDrawerLayout.closeDrawer(Gravity.START);

            final BottomMenu menu = new BottomMenu(this);
            menu.setTitle(getString(R.string.addSong));
            menu.addMenu(getString(R.string.addFromLocal), R.drawable.ic_actionsheet_music, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    menu.dismiss();
                    open();
                }
            });
            if(Build.VERSION.SDK_INT >= 18) {
                menu.addMenu(getString(R.string.addFromVideo), R.drawable.ic_actionsheet_film, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        menu.dismiss();
                        openGallery();
                    }
                });
            }
            final Activity activity = this;
            menu.addMenu(getString(R.string.addURL), R.drawable.ic_actionsheet_globe, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    menu.dismiss();

                    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                    builder.setTitle(R.string.addURL);
                    LinearLayout linearLayout = new LinearLayout(activity);
                    linearLayout.setOrientation(LinearLayout.VERTICAL);
                    final EditText editURL = new EditText (activity);
                    editURL.setHint(R.string.URL);
                    editURL.setHintTextColor(Color.argb(255, 192, 192, 192));
                    editURL.setText("");
                    linearLayout.addView(editURL);
                    builder.setView(linearLayout);
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener()
                    {
                        public void onClick(DialogInterface dialog, int id)
                        {
                            playlistFragment.startAddURL(editURL.getText().toString());
                        }
                    });
                    builder.setNegativeButton(R.string.cancel, null);
                    final AlertDialog alertDialog = builder.create();
                    alertDialog.setOnShowListener(new DialogInterface.OnShowListener()
                    {
                        @Override
                        public void onShow(DialogInterface arg0)
                        {
                            if(alertDialog.getWindow() != null) {
                                WindowManager.LayoutParams lp = alertDialog.getWindow().getAttributes();
                                lp.dimAmount = 0.4f;
                                alertDialog.getWindow().setAttributes(lp);
                            }
                            editURL.requestFocus();
                            editURL.setSelection(editURL.getText().toString().length());
                            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                            if (null != imm) imm.showSoftInput(editURL, 0);
                        }
                    });
                    alertDialog.show();
                }
            });
            menu.setCancelMenu();
            menu.show();
        }
        else if(v.getId() == R.id.relativeHideAds)
        {
            try {
                Bundle buyIntentBundle = mService.getBuyIntent(3, getPackageName(), "hideads", "inapp", "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAkVvqgLyPSTyJKuyNw3Z0luaxCnOtbFwj65HGYmDS4KiyGaJNgFsLOc9wpmIQaQI+zrntxbufWXsT0gIh1/MRRmX2FgA0G6WDS0+w39ZsbgJRbXsxOzOOZaHbSo2NLOA29GXPo9FraFtNrOL9v4vLu7hxDPdfqoFNR80BUWwQqMBsiMNFqJ12sq1HzxHd2MIk/QooBZIB3EeM0QX5EYIsWcaKIAyzetuKjRGvO9Oi2a86dOBUfOFnHMMCvQ5+dldx5UkzmnhlbTm/KBWQCO3AqNy82NKxN9ND6GWVrlHuQGYX1FRiApMeXCmEvmwEyU2ArztpV8CfHyK2d0mM4bp0bwIDAQAB");
                int response = buyIntentBundle.getInt("RESPONSE_CODE");
                if(response == 0) {
                    PendingIntent pendingIntent = buyIntentBundle.getParcelable("BUY_INTENT");
                    if(pendingIntent != null)
                        startIntentSenderForResult(pendingIntent.getIntentSender(), 1001, new Intent(), 0, 0, 0);
                }
                else if(response == 7){
                    hideAds();
                }
            }
            catch(Exception e) {
                e.printStackTrace();
            }
            mDrawerLayout.closeDrawer(Gravity.START);
        }
        else if(v.getId() == R.id.relativeItem)
        {
            mDrawerLayout.closeDrawer(Gravity.START);
            openItem();
        }
        else if(v.getId() == R.id.relativeReport)
        {
            Uri uri = Uri.parse("https://twitter.com/ryota_yama");
            Intent i = new Intent(Intent.ACTION_VIEW,uri);
            startActivity(i);
            mDrawerLayout.closeDrawer(Gravity.START);
        }
        else if(v.getId() == R.id.relativeReview)
        {
            Uri uri = Uri.parse("https://play.google.com/store/apps/details?id=com.edolfzoku.hayaemon2&hl=ja");
            Intent i = new Intent(Intent.ACTION_VIEW,uri);
            startActivity(i);
            mDrawerLayout.closeDrawer(Gravity.START);
        }
        else if(v.getId() == R.id.relativeInfo)
        {
            mDrawerLayout.closeDrawer(Gravity.START);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            try {
                String strVersionName = getApplicationContext().getPackageManager().getPackageInfo(getApplicationContext().getPackageName(), 0).versionName;
                builder.setMessage(String.format(Locale.getDefault(), "%s: Android ver.%s", getString(R.string.version), strVersionName));
            }
            catch(PackageManager.NameNotFoundException e) {
                e.printStackTrace();
                return;
            }

            builder.setTitle(R.string.about);
            builder.setIcon(R.mipmap.ic_launcher);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                }
            });
            final AlertDialog alertDialog = builder.create();
            alertDialog.setOnShowListener(new DialogInterface.OnShowListener()
            {
                @Override
                public void onShow(DialogInterface arg0)
                {
                    if(alertDialog.getWindow() != null) {
                        WindowManager.LayoutParams lp = alertDialog.getWindow().getAttributes();
                        lp.dimAmount = 0.4f;
                        alertDialog.getWindow().setAttributes(lp);
                    }
                }
            });
            alertDialog.show();
        }
        else if(v.getId() == R.id.btnSetting)
        {
            mDrawerLayout.closeDrawer(Gravity.START);
            openSetting();
        }
        else if(v.getId() == R.id.btnPlayInPlayingBar)
            playlistFragment.onPlayBtnClick();
        else if(v.getId() == R.id.btnForwardInPlayingBar)
            playlistFragment.onForwardBtnClick();
        else if(v.getId() == R.id.btnRewindInPlayingBar)
            playlistFragment.onRewindBtnClick();
        else if(v.getId() == R.id.relativePlaying)
            upViewPlaying();
        else if(v.getId() == R.id.imgViewDown)
            downViewPlaying(false);
        else if(v.getId() == R.id.btnCloseInPlayingBar)
            playlistFragment.stop();
        else if(v.getId() == R.id.btnMoreInPlayingBar) {
            final BottomMenu menu = new BottomMenu(this);
            final int nPlaying = playlistFragment.getPlaying();
            playlistFragment.setSelectedItem(nPlaying);
            SongItem item = playlistFragment.getPlaylists().get(playlistFragment.getPlayingPlaylist()).get(nPlaying);
            menu.setTitle(item.getTitle());
            menu.addMenu(getString(R.string.saveExport), R.drawable.ic_actionsheet_save, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    menu.dismiss();
                    showSaveExportMenu();
                }
            });
            menu.addMenu(getString(R.string.changeTitleAndArtist), R.drawable.ic_actionsheet_edit, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    menu.dismiss();
                    playlistFragment.changeTitleAndArtist(nPlaying);
                }
            });
            menu.addMenu(getString(R.string.showLyrics), R.drawable.ic_actionsheet_file_text, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    menu.dismiss();
                    downViewPlaying(false);
                    playlistFragment.showLyrics();
                    mViewPager.setCurrentItem(0);
                }
            });
            ArrayList<EffectSaver> arEffectSavers = playlistFragment.getEffects().get(playlistFragment.getSelectedPlaylist());
            final EffectSaver saver = arEffectSavers.get(nPlaying);
            if(saver.isSave())
            {
                menu.addMenu(getString(R.string.cancelRestoreEffect), R.drawable.ic_actionsheet_unlock, new View.OnClickListener() {
                    @Override
                    public void onClick(View view)
                    {
                        saver.setSave(false);
                        playlistFragment.getSongsAdapter().notifyItemChanged(nPlaying);

                        playlistFragment.saveFiles(false, true, false, false, false);
                        menu.dismiss();
                    }
                });
            }
            else
            {
                menu.addMenu(getString(R.string.restoreEffect), R.drawable.ic_actionsheet_lock, new View.OnClickListener() {
                    @Override
                    public void onClick(View view)
                    {
                        playlistFragment.setSavingEffect();
                        playlistFragment.getSongsAdapter().notifyItemChanged(nPlaying);
                        menu.dismiss();
                    }
                });
            }
            menu.setCancelMenu();
            menu.show();
        }
        else if(v.getId() == R.id.btnArtworkInPlayingBar) {
            final int nPlaying = playlistFragment.getPlaying();
            playlistFragment.setSelectedItem(nPlaying);
            final SongItem item = playlistFragment.getPlaylists().get(playlistFragment.getPlayingPlaylist()).get(nPlaying);
            final BottomMenu menu = new BottomMenu(this);
            menu.setTitle(getString(R.string.changeArtwork));
            menu.addMenu(getString(R.string.setImage), R.drawable.ic_actionsheet_image, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    menu.dismiss();
                    if (Build.VERSION.SDK_INT < 19)
                    {
                        final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                        intent.setType("image/*");
                        playlistFragment.startActivityForResult(intent, 3);
                    }
                    else
                    {
                        final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                        intent.addCategory(Intent.CATEGORY_OPENABLE);
                        intent.setType("image/*");
                        playlistFragment.startActivityForResult(intent, 3);
                    }
                }
            });
            if(item.getPathArtwork() != null && !item.getPathArtwork().equals("")) {
                final MainActivity activity = this;
                menu.addDestructiveMenu(getString(R.string.resetArtwork), R.drawable.ic_actionsheet_initialize, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        menu.dismiss();
                        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                        builder.setTitle(R.string.resetArtwork);
                        builder.setMessage(R.string.askResetArtwork);
                        builder.setPositiveButton(getString(R.string.decideNot), null);
                        builder.setNegativeButton(getString(R.string.doReset), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                playlistFragment.resetArtwork();
                            }
                        });
                        final AlertDialog alertDialog = builder.create();
                        alertDialog.setOnShowListener(new DialogInterface.OnShowListener()
                        {
                            @Override
                            public void onShow(DialogInterface arg0)
                            {
                                if(alertDialog.getWindow() != null) {
                                    WindowManager.LayoutParams lp = alertDialog.getWindow().getAttributes();
                                    lp.dimAmount = 0.4f;
                                    alertDialog.getWindow().setAttributes(lp);
                                }
                                Button negativeButton = alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
                                negativeButton.setTextColor(Color.argb(255, 255, 0, 0));
                            }
                        });
                        alertDialog.show();
                    }
                });
            }
            menu.setCancelMenu();
            menu.show();
        }
    }

    private void upViewPlaying()
    {
        playlistFragment.selectPlaylist(playlistFragment.getPlayingPlaylist());
        mRelativePlaying.setOnClickListener(null);
        mBtnArtworkInPlayingBar.setOnClickListener(this);
        mBtnArtworkInPlayingBar.setAnimation(true);
        mBtnArtworkInPlayingBar.setClickable(true);
        final long lDuration = 400;
        int nScreenWidth = getResources().getDisplayMetrics().widthPixels;
        mRelativePlayingWithShadow.setBackgroundResource(R.drawable.playingview);
        RelativeLayout.LayoutParams paramContainer = (RelativeLayout.LayoutParams) mViewPager.getLayoutParams();
        RelativeLayout.LayoutParams paramRecording = (RelativeLayout.LayoutParams) mRelativeRecording.getLayoutParams();
        if (MainActivity.sRecord != 0) {
            paramContainer.addRule(RelativeLayout.ABOVE, R.id.relativeRecording);
            paramContainer.bottomMargin = 0;
            paramRecording.addRule(RelativeLayout.ABOVE, R.id.adView);
            paramRecording.bottomMargin = (int) (60.0 * mDensity);
        } else {
            paramContainer.addRule(RelativeLayout.ABOVE, R.id.adView);
            paramContainer.bottomMargin = (int) (60.0 * mDensity);
        }

        final RelativeLayout.LayoutParams paramTitle = (RelativeLayout.LayoutParams) mTextTitle.getLayoutParams();
        final RelativeLayout.LayoutParams paramArtist = (RelativeLayout.LayoutParams) mTextArtist.getLayoutParams();
        final RelativeLayout.LayoutParams paramBtnPlay = (RelativeLayout.LayoutParams) mBtnPlayInPlayingBar.getLayoutParams();
        final RelativeLayout.LayoutParams paramBtnForward = (RelativeLayout.LayoutParams) mBtnForwardInPlayingBar.getLayoutParams();

        mTextTitle.setGravity(Gravity.CENTER);
        mTextArtist.setGravity(Gravity.CENTER);
        paramTitle.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0);
        paramArtist.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 0);
        if(Build.VERSION.SDK_INT >= 17) {
            paramTitle.addRule(RelativeLayout.ALIGN_PARENT_END, 0);
            paramArtist.addRule(RelativeLayout.ALIGN_PARENT_END, 0);
        }

        final int nTranslationYFrom = (int)mRelativePlayingWithShadow.getTranslationY();
        final int nTranslationY = 0;
        final int nRelativePlayingHeightFrom = mRelativePlayingWithShadow.getHeight();
        final int nRelativePlayingHeight = getResources().getDisplayMetrics().heightPixels - mLinearControl.getHeight() - getStatusBarHeight() + (int) (16.0 * mDensity);
        final int nArtworkWidthFrom = mBtnArtworkInPlayingBar.getWidth();
        final int nArtworkWidth = nScreenWidth / 2;
        final int nArtworkMarginFrom = (int) (8.0 * mDensity);
        final int nArtworkLeftMargin = nScreenWidth / 2 - nArtworkWidth / 2;
        final int nArtworkTopMargin = (int) (64.0 * mDensity);
        final int nTitleTopMarginFrom = paramTitle.topMargin;
        final int nTitleLeftMarginFrom = paramTitle.leftMargin;
        final int nTitleRightMarginFrom = paramTitle.rightMargin;
        final int nTitleMargin = (int) (32.0 * mDensity);
        final int nTitleTopMargin = nArtworkTopMargin + nArtworkWidth + (int) (32.0 * mDensity) + (int) (24.0 * mDensity) + (int) (34.0 * mDensity);
        final int nArtistTopMarginFrom = paramArtist.topMargin;
        final int nArtistTopMargin = nTitleTopMargin + mTextTitle.getHeight() + (int) (4.0 * mDensity);
        final int nBtnPlayTopMargin = nArtistTopMargin + mTextArtist.getHeight() + (int) (20.0 * mDensity);
        final int nBtnPlayRightMarginFrom = paramBtnPlay.rightMargin;
        final int nBtnPlayRightMargin = nScreenWidth / 2 - mBtnPlayInPlayingBar.getWidth() / 2;
        final int nBtnForwardRightMarginFrom = paramBtnForward.rightMargin;
        final int nBtnForwardRightMargin = nBtnPlayRightMargin - mBtnForwardInPlayingBar.getWidth() - (int) (16.0 * mDensity);
        final float fTitleFontFrom = 13.0f;
        final float fTitleFont = 15.0f;
        Paint paint = new Paint();
        paint.setTextSize(mTextTitle.getTextSize());
        final int nTitleWidthFrom = (int) paint.measureText(mTextTitle.getText().toString());
        final int nTitleWidth = nScreenWidth;
        final float fArtistFontFrom = 10.0f;
        final float fArtistFont = 13.0f;
        paint.setTextSize(mTextArtist.getTextSize());
        final int nArtistWidthFrom = (int) paint.measureText(mTextArtist.getText().toString());
        final int nArtistWidth = nScreenWidth;
        final int nBtnHeightFrom = (int) (60.0 * mDensity);
        final int nBtnHeight = (int) (44.0 * mDensity);

        ValueAnimator anim = ValueAnimator.ofFloat(0.0f, 1.0f);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float fProgress = valueAnimator.getAnimatedFraction();
                advanceAnimation(mTabLayout, "bottomMargin", 0, -mTabLayout.getHeight(), fProgress);
                advanceAnimation(mViewSep2, "bottomMargin", 0, mTabLayout.getHeight(), fProgress);
                mRelativePlayingWithShadow.setTranslationY(nTranslationYFrom + (nTranslationY - nTranslationYFrom) * fProgress);
                advanceAnimation(mRelativePlayingWithShadow, "height", nRelativePlayingHeightFrom, nRelativePlayingHeight, fProgress);
                advanceAnimation(mRelativePlayingWithShadow, "bottomMargin", 0, -mTabLayout.getHeight(), fProgress);
                advanceAnimation(mRelativePlaying, "height", nRelativePlayingHeightFrom, nRelativePlayingHeight, fProgress);
                advanceAnimation(mBtnArtworkInPlayingBar, "width", nArtworkWidthFrom, nArtworkWidth, fProgress);
                advanceAnimation(mBtnArtworkInPlayingBar, "height", nArtworkWidthFrom, nArtworkWidth, fProgress);
                advanceAnimation(mBtnArtworkInPlayingBar, "leftMargin", nArtworkMarginFrom, nArtworkLeftMargin, fProgress);
                advanceAnimation(mBtnArtworkInPlayingBar, "topMargin", nArtworkMarginFrom, nArtworkTopMargin, fProgress);
                advanceAnimation(mTextTitle, "width", nTitleWidthFrom, nTitleWidth, fProgress);
                advanceAnimation(mTextTitle, "topMargin", nTitleTopMarginFrom, nTitleTopMargin, fProgress);
                advanceAnimation(mTextTitle, "leftMargin", nTitleLeftMarginFrom, nTitleMargin, fProgress);
                advanceAnimation(mTextTitle, "rightMargin", nTitleRightMarginFrom, nTitleMargin, fProgress);
                mTextTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, fTitleFontFrom + (fTitleFont - fTitleFontFrom) * fProgress);
                advanceAnimation(mTextArtist, "width", nArtistWidthFrom, nArtistWidth, fProgress);
                advanceAnimation(mTextArtist, "topMargin", nArtistTopMarginFrom, nArtistTopMargin, fProgress);
                advanceAnimation(mTextArtist, "leftMargin", nTitleLeftMarginFrom, nTitleMargin, fProgress);
                advanceAnimation(mTextArtist, "rightMargin", nTitleRightMarginFrom, nTitleMargin, fProgress);
                mTextArtist.setTextSize(TypedValue.COMPLEX_UNIT_DIP, fArtistFontFrom + (fArtistFont - fArtistFontFrom) * fProgress);
                advanceAnimation(mBtnPlayInPlayingBar, "topMargin", 0, nBtnPlayTopMargin, fProgress);
                advanceAnimation(mBtnForwardInPlayingBar, "topMargin", 0, nBtnPlayTopMargin, fProgress);
                advanceAnimation(mBtnPlayInPlayingBar, "rightMargin", nBtnPlayRightMarginFrom, nBtnPlayRightMargin, fProgress);
                advanceAnimation(mBtnPlayInPlayingBar, "height", nBtnHeightFrom, nBtnHeight, fProgress);
                advanceAnimation(mBtnForwardInPlayingBar, "height", nBtnHeightFrom, nBtnHeight, fProgress);
                advanceAnimation(mBtnRewindInPlayingBar, "height", nBtnHeightFrom, nBtnHeight, fProgress);
                advanceAnimation(mBtnForwardInPlayingBar, "rightMargin", nBtnForwardRightMarginFrom, nBtnForwardRightMargin, fProgress);
                mBtnMoreInPlayingBar.requestLayout();
                mBtnShuffleInPlayingBar.requestLayout();
                mBtnRepeatInPlayingBar.requestLayout();
            }
        });
        anim.setDuration(lDuration).start();

        mImgViewDown.setVisibility(View.VISIBLE);
        mSeekCurPos.setVisibility(View.VISIBLE);
        mTextCurPos.setVisibility(View.VISIBLE);
        mTextRemain.setVisibility(View.VISIBLE);
        mBtnRewindInPlayingBar.setVisibility(View.VISIBLE);
        mBtnMoreInPlayingBar.setVisibility(View.VISIBLE);
        mBtnShuffleInPlayingBar.setVisibility(View.VISIBLE);
        mBtnRepeatInPlayingBar.setVisibility(View.VISIBLE);

        if (BASS.BASS_ChannelIsActive(MainActivity.sStream) != BASS.BASS_ACTIVE_PLAYING)
            mBtnPlayInPlayingBar.setImageResource(R.drawable.ic_playing_large_play);
        else mBtnPlayInPlayingBar.setImageResource(R.drawable.ic_playing_large_pause);
        mBtnForwardInPlayingBar.setImageResource(R.drawable.ic_playing_large_forward);

        mImgViewDown.animate().alpha(1.0f).setDuration(lDuration);
        mSeekCurPos.animate().alpha(1.0f).setDuration(lDuration);
        mTextCurPos.animate().alpha(1.0f).setDuration(lDuration);
        mTextRemain.animate().alpha(1.0f).setDuration(lDuration);
        mBtnRewindInPlayingBar.animate().alpha(1.0f).setDuration(lDuration);
        mBtnMoreInPlayingBar.animate().alpha(1.0f).setDuration(lDuration);
        mBtnShuffleInPlayingBar.animate().alpha(1.0f).setDuration(lDuration);
        mBtnRepeatInPlayingBar.animate().alpha(1.0f).setDuration(lDuration);
        mBtnCloseInPlayingBar.animate().alpha(0.0f).setDuration(lDuration);
        mAdView.animate().translationY(mTabLayout.getHeight() + mAdView.getHeight()).setDuration(lDuration);
        mTabLayout.animate().translationY(mTabLayout.getHeight() + mAdView.getHeight()).setDuration(lDuration);
        mViewSep2.animate().translationY(mTabLayout.getHeight()).setDuration(lDuration);
    }

    public void downViewPlaying(final boolean bBottom)
    {
        final MainActivity activity = this;
        final long lDuration = 400;
        mRelativePlayingWithShadow.setBackgroundResource(R.drawable.topshadow);

        final RelativeLayout.LayoutParams paramArtwork = (RelativeLayout.LayoutParams) mBtnArtworkInPlayingBar.getLayoutParams();
        final RelativeLayout.LayoutParams paramTitle = (RelativeLayout.LayoutParams) mTextTitle.getLayoutParams();
        final RelativeLayout.LayoutParams paramArtist = (RelativeLayout.LayoutParams) mTextArtist.getLayoutParams();
        final RelativeLayout.LayoutParams paramBtnPlay = (RelativeLayout.LayoutParams) mBtnPlayInPlayingBar.getLayoutParams();
        final RelativeLayout.LayoutParams paramBtnForward = (RelativeLayout.LayoutParams) mBtnForwardInPlayingBar.getLayoutParams();

        final int nTranslationYFrom = (int)mRelativePlayingWithShadow.getTranslationY();
        final int nTranslationY = 0;
        final int nRelativePlayingHeightFrom = getResources().getDisplayMetrics().heightPixels - mTabLayout.getHeight() - mLinearControl.getHeight() - getStatusBarHeight() + (int) (16.0 * mDensity);
        int nTempRelativePlayingHeight = (int) (82.0 * mDensity);
        if(bBottom) nTempRelativePlayingHeight = 0;
        final int nRelativePlayingHeight = nTempRelativePlayingHeight;
        final int nArtworkWidthFrom = mBtnArtworkInPlayingBar.getWidth();
        final int nArtworkWidth = (int) (44.0 * mDensity);
        final int nArtworkLeftMarginFrom = paramArtwork.leftMargin;
        final int nArtworkLeftMargin = (int) (8.0 * mDensity);
        final int nArtworkTopMarginFrom = paramArtwork.topMargin;
        final int nArtworkTopMargin = (int) (8.0 * mDensity);
        final int nTitleTopMarginFrom = paramTitle.topMargin;
        final int nTitleLeftMarginFrom = paramTitle.leftMargin;
        final int nTitleRightMarginFrom = paramTitle.rightMargin;
        final float fTitleFontFrom = 15.0f;
        final float fTitleFont = 13.0f;
        Paint paint = new Paint();
        mTextTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, fTitleFont);
        paint.setTextSize(mTextTitle.getTextSize());
        mTextTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, fTitleFontFrom);
        final int nTitleWidthFrom = paramTitle.width;
        final int nTitleWidth = (int) paint.measureText(mTextTitle.getText().toString());
        final int nArtistTopMarginFrom = paramArtist.topMargin;
        final float fArtistFontFrom = 13.0f;
        final float fArtistFont = 10.0f;
        mTextArtist.setTextSize(TypedValue.COMPLEX_UNIT_DIP, fArtistFont);
        paint.setTextSize(mTextArtist.getTextSize());
        mTextArtist.setTextSize(TypedValue.COMPLEX_UNIT_DIP, fArtistFontFrom);
        final int nArtistWidthFrom = paramArtist.width;
        final int nArtistWidth = (int) paint.measureText(mTextArtist.getText().toString());
        final int nBtnPlayTopMarginFrom = paramBtnPlay.topMargin;
        final int nBtnPlayRightMarginFrom = paramBtnPlay.rightMargin;
        final int nBtnPlayRightMargin = (int) (88.0 * mDensity);
        final int nBtnHeightFrom = (int) (44.0 * mDensity);
        final int nBtnHeight = (int) (60.0 * mDensity);
        final int nBtnForwardRightMarginFrom = paramBtnForward.rightMargin;
        final int nBtnForwardRightMargin = (int) (44.0 * mDensity);

        ValueAnimator anim = ValueAnimator.ofFloat(0.0f, 1.0f);
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float fProgress = valueAnimator.getAnimatedFraction();
                advanceAnimation(mTabLayout, "bottomMargin", -mTabLayout.getHeight(), 0, fProgress);
                advanceAnimation(mViewSep2, "bottomMargin", mTabLayout.getHeight(), 0, fProgress);
                mRelativePlayingWithShadow.setTranslationY(nTranslationYFrom + (nTranslationY - nTranslationYFrom) * fProgress);
                advanceAnimation(mRelativePlayingWithShadow, "height", nRelativePlayingHeightFrom, nRelativePlayingHeight, fProgress);
                advanceAnimation(mRelativePlayingWithShadow, "bottomMargin", -mTabLayout.getHeight(), 0, fProgress);
                advanceAnimation(mRelativePlaying, "height", nRelativePlayingHeightFrom, nRelativePlayingHeight, fProgress);
                advanceAnimation(mBtnArtworkInPlayingBar, "width", nArtworkWidthFrom, nArtworkWidth, fProgress);
                advanceAnimation(mBtnArtworkInPlayingBar, "height", nArtworkWidthFrom, nArtworkWidth, fProgress);
                advanceAnimation(mBtnArtworkInPlayingBar, "leftMargin", nArtworkLeftMarginFrom, nArtworkLeftMargin, fProgress);
                advanceAnimation(mBtnArtworkInPlayingBar, "topMargin", nArtworkTopMarginFrom, nArtworkTopMargin, fProgress);
                advanceAnimation(mTextTitle, "width", nTitleWidthFrom, nTitleWidth, fProgress);
                advanceAnimation(mTextTitle, "topMargin", nTitleTopMarginFrom, (int) (14.0 * mDensity), fProgress);
                advanceAnimation(mTextTitle, "leftMargin", nTitleLeftMarginFrom, (int) (60.0 * mDensity), fProgress);
                advanceAnimation(mTextTitle, "rightMargin", nTitleRightMarginFrom, (int) (132.0 * mDensity), fProgress);
                mTextTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, fTitleFontFrom + (fTitleFont - fTitleFontFrom) * fProgress);
                advanceAnimation(mTextArtist, "width", nArtistWidthFrom, nArtistWidth, fProgress);
                advanceAnimation(mTextArtist, "topMargin", nArtistTopMarginFrom, (int) (33.0 * mDensity), fProgress);
                advanceAnimation(mTextArtist, "leftMargin", nTitleLeftMarginFrom, (int) (60.0 * mDensity), fProgress);
                advanceAnimation(mTextArtist, "rightMargin", nTitleRightMarginFrom, (int) (132.0 * mDensity), fProgress);
                mTextArtist.setTextSize(TypedValue.COMPLEX_UNIT_DIP, fArtistFontFrom + (fArtistFont - fArtistFontFrom) * fProgress);
                advanceAnimation(mBtnPlayInPlayingBar, "topMargin", nBtnPlayTopMarginFrom, 0, fProgress);
                advanceAnimation(mBtnForwardInPlayingBar, "topMargin", nBtnPlayTopMarginFrom, 0, fProgress);
                advanceAnimation(mBtnPlayInPlayingBar, "rightMargin", nBtnPlayRightMarginFrom, nBtnPlayRightMargin, fProgress);
                advanceAnimation(mBtnPlayInPlayingBar, "height", nBtnHeightFrom, nBtnHeight, fProgress);
                advanceAnimation(mBtnForwardInPlayingBar, "height", nBtnHeightFrom, nBtnHeight, fProgress);
                advanceAnimation(mBtnRewindInPlayingBar, "height", nBtnHeightFrom, nBtnHeight, fProgress);
                advanceAnimation(mBtnForwardInPlayingBar, "rightMargin", nBtnForwardRightMarginFrom, nBtnForwardRightMargin, fProgress);
            }
        });
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                mTextTitle.setGravity(Gravity.START);
                paramTitle.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 1);
                mTextArtist.setGravity(Gravity.START);
                paramArtist.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, 1);
                if(Build.VERSION.SDK_INT >= 17) {
                    paramTitle.addRule(RelativeLayout.ALIGN_PARENT_END, 1);
                    paramArtist.addRule(RelativeLayout.ALIGN_PARENT_END, 1);
                }
                mImgViewDown.clearAnimation();
                mImgViewDown.setVisibility(View.GONE);
                mSeekCurPos.clearAnimation();
                mSeekCurPos.setVisibility(View.GONE);
                mTextCurPos.clearAnimation();
                mTextCurPos.setVisibility(View.GONE);
                mTextRemain.clearAnimation();
                mTextRemain.setVisibility(View.GONE);
                mBtnRewindInPlayingBar.clearAnimation();
                mBtnRewindInPlayingBar.setVisibility(View.GONE);
                mBtnMoreInPlayingBar.clearAnimation();
                mBtnMoreInPlayingBar.setVisibility(View.GONE);
                mBtnShuffleInPlayingBar.clearAnimation();
                mBtnShuffleInPlayingBar.setVisibility(View.GONE);
                mBtnRepeatInPlayingBar.clearAnimation();
                mBtnRepeatInPlayingBar.setVisibility(View.GONE);
                mRelativePlaying.setOnClickListener(activity);
                mBtnArtworkInPlayingBar.setOnClickListener(null);
                mBtnArtworkInPlayingBar.setAnimation(false);
                mBtnArtworkInPlayingBar.setClickable(false);

                RelativeLayout.LayoutParams paramContainer = (RelativeLayout.LayoutParams) mViewPager.getLayoutParams();
                RelativeLayout.LayoutParams paramRecording = (RelativeLayout.LayoutParams) mRelativeRecording.getLayoutParams();
                if (MainActivity.sRecord != 0) {
                    paramContainer.addRule(RelativeLayout.ABOVE, R.id.relativeRecording);
                    paramContainer.bottomMargin = 0;
                    paramRecording.addRule(RelativeLayout.ABOVE, R.id.relativePlayingWithShadow);
                    if(bBottom) paramRecording.bottomMargin = 0;
                    else paramRecording.bottomMargin = (int) (-22 * mDensity);
                } else {
                    paramContainer.addRule(RelativeLayout.ABOVE, R.id.relativePlayingWithShadow);
                    if(bBottom) paramContainer.bottomMargin = 0;
                    else paramContainer.bottomMargin = (int) (-22 * mDensity);
                }

                if(bBottom) {
                    mRelativePlayingWithShadow.setVisibility(View.GONE);
                    RelativeLayout.LayoutParams paramPlayingWithShadow = (RelativeLayout.LayoutParams) mRelativePlayingWithShadow.getLayoutParams();
                    paramPlayingWithShadow.height = (int) (82.0 * mDensity);
                    RelativeLayout.LayoutParams paramPlaying = (RelativeLayout.LayoutParams) mRelativePlaying.getLayoutParams();
                    paramPlaying.height = (int) (82.0 * mDensity);
                }
            }
        });
        anim.setDuration(lDuration).start();

        if (BASS.BASS_ChannelIsActive(MainActivity.sStream) != BASS.BASS_ACTIVE_PLAYING)
            mBtnPlayInPlayingBar.setImageResource(R.drawable.ic_bar_button_play);
        else mBtnPlayInPlayingBar.setImageResource(R.drawable.ic_bar_button_pause);
        mBtnForwardInPlayingBar.setImageResource(R.drawable.ic_bar_button_forward);

        mImgViewDown.animate().alpha(0.0f).setDuration(lDuration);
        mSeekCurPos.animate().alpha(0.0f).setDuration(lDuration);
        mTextCurPos.animate().alpha(0.0f).setDuration(lDuration);
        mTextRemain.animate().alpha(0.0f).setDuration(lDuration);
        mBtnRewindInPlayingBar.animate().alpha(0.0f).setDuration(lDuration);
        mBtnMoreInPlayingBar.animate().alpha(0.0f).setDuration(lDuration);
        mBtnShuffleInPlayingBar.animate().alpha(0.0f).setDuration(lDuration);
        mBtnRepeatInPlayingBar.animate().alpha(0.0f).setDuration(lDuration);
        mBtnCloseInPlayingBar.animate().alpha(1.0f).setDuration(lDuration);
        mAdView.animate().translationY(0).setDuration(lDuration);
        mTabLayout.animate().translationY(0).setDuration(lDuration);
        mViewSep2.animate().translationY(0).setDuration(lDuration);
    }

    private void showSaveExportMenu()
    {
        final BottomMenu menu = new BottomMenu(this);
        menu.setTitle(getString(R.string.saveExport));
        menu.addMenu(getString(R.string.saveToApp), R.drawable.ic_actionsheet_save, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                menu.dismiss();
                playlistFragment.saveSongToLocal();
            }
        });
        if(Build.VERSION.SDK_INT >= 18) {
            menu.addMenu(getString(R.string.saveAsVideo), R.drawable.ic_actionsheet_film, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    menu.dismiss();
                    playlistFragment.saveSongToGallery();
                }
            });
        }
        menu.addMenu(getString(R.string.export), R.drawable.ic_actionsheet_share, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                menu.dismiss();
                playlistFragment.export();
            }
        });
        menu.setCancelMenu();
        menu.show();
    }

    private void openItem()
    {
        SharedPreferences preferences = getSharedPreferences("SaveData", Activity.MODE_PRIVATE);
        preferences.edit().putBoolean("bPinkCamperDisplayed", true).apply();
        preferences.edit().putBoolean("bBlueCamperDisplayed", true).apply();
        preferences.edit().putBoolean("bOrangeCamperDisplayed", true).apply();
        FragmentManager fragmentManager = getSupportFragmentManager();
        final FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.setCustomAnimations(R.anim.slide_in_up, R.anim.slide_out_up);
        transaction.replace(R.id.relativeMain, new ItemFragment());
        transaction.commit();
    }

    public void openSetting()
    {
        FragmentManager fragmentManager = getSupportFragmentManager();
        final FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.setCustomAnimations(R.anim.slide_in_up, R.anim.slide_out_up);
        transaction.replace(R.id.relativeMain, new SettingFragment());
        transaction.commit();
    }

    public void open()
    {
        if (Build.VERSION.SDK_INT < 19)
        {
            final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("audio/*");
            playlistFragment.startActivityForResult(intent, 1);
        }
        else
        {
            final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("audio/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            playlistFragment.startActivityForResult(intent, 1);
        }
    }

    public void openGallery()
    {
        if (Build.VERSION.SDK_INT < 19)
        {
            final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("video/*");
            playlistFragment.startActivityForResult(intent, 2);
        }
        else
        {
            final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("video/*");
            playlistFragment.startActivityForResult(intent, 2);
        }
    }

    @SuppressLint("NewApi")
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode != RESULT_OK) return;

        if(requestCode == 1001)
        {
            String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");

            try {
                JSONObject jo = new JSONObject(purchaseData);
                jo.getString("productId");

                hideAds();
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
        }
        else if(requestCode == 1002)
        {
            String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");

            try {
                JSONObject jo = new JSONObject(purchaseData);
                jo.getString("productId");

                FragmentManager fragmentManager = getSupportFragmentManager();
                ItemFragment itemFragment = (ItemFragment)fragmentManager.findFragmentById(R.id.relativeMain);
                itemFragment.buyPurpleSeaUrchinPointer();
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
        }
        else if(requestCode == 1003)
        {
            String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");

            try {
                JSONObject jo = new JSONObject(purchaseData);
                jo.getString("productId");

                FragmentManager fragmentManager = getSupportFragmentManager();
                ItemFragment itemFragment = (ItemFragment)fragmentManager.findFragmentById(R.id.relativeMain);
                itemFragment.buyElegantSeaUrchinPointer();
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
        }
        else if(requestCode == 1004)
        {
            String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");

            try {
                JSONObject jo = new JSONObject(purchaseData);
                jo.getString("productId");

                FragmentManager fragmentManager = getSupportFragmentManager();
                ItemFragment itemFragment = (ItemFragment)fragmentManager.findFragmentById(R.id.relativeMain);
                itemFragment.buyPinkCamperPointer();
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
        }
        else if(requestCode == 1005)
        {
            String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");

            try {
                JSONObject jo = new JSONObject(purchaseData);
                jo.getString("productId");

                FragmentManager fragmentManager = getSupportFragmentManager();
                ItemFragment itemFragment = (ItemFragment)fragmentManager.findFragmentById(R.id.relativeMain);
                itemFragment.buyBlueCamperPointer();
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
        }
        else if(requestCode == 1006)
        {
            String purchaseData = data.getStringExtra("INAPP_PURCHASE_DATA");

            try {
                JSONObject jo = new JSONObject(purchaseData);
                jo.getString("productId");

                FragmentManager fragmentManager = getSupportFragmentManager();
                ItemFragment itemFragment = (ItemFragment)fragmentManager.findFragmentById(R.id.relativeMain);
                itemFragment.buyOrangeCamperPointer();
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    private void hideAds()
    {
        if(mAdView.getVisibility() != AdView.GONE) {
            mAdView.setVisibility(AdView.GONE);

            SharedPreferences preferences = getSharedPreferences("SaveData", Activity.MODE_PRIVATE);
            preferences.edit().putBoolean("hideads", mAdView.getVisibility() == AdView.GONE).apply();
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void initialize(Bundle savedInstanceState)
    {
        MobileAds.initialize(this, "ca-app-pub-9499594730627438~9516019647");

        mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        BASS.BASS_Init(-1, 44100, 0);
        BASS.BASS_SetConfig(BASS.BASS_CONFIG_FLOATDSP, 1);
        BASS.BASS_SetConfig(BASS_CONFIG_AAC_MP4, 1);

        BASS.BASS_PluginLoad("libbass_aac.so", 0);
        BASS.BASS_PluginLoad("libbassflac.so", 0);

        SectionsPagerAdapter sectionsPagerAdapter;
        if(savedInstanceState == null) {
            sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());
            playlistFragment = (PlaylistFragment)sectionsPagerAdapter.getItem(0);
            loopFragment = (LoopFragment)sectionsPagerAdapter.getItem(1);
            controlFragment = (ControlFragment)sectionsPagerAdapter.getItem(2);
            equalizerFragment = (EqualizerFragment)sectionsPagerAdapter.getItem(3);
            effectFragment = (EffectFragment)sectionsPagerAdapter.getItem(4);
        }
        else {
            playlistFragment = (PlaylistFragment)getSupportFragmentManager().getFragment(savedInstanceState, "playlistFragment");
            loopFragment = (LoopFragment)getSupportFragmentManager().getFragment(savedInstanceState, "loopFragment");
            controlFragment = (ControlFragment)getSupportFragmentManager().getFragment(savedInstanceState, "controlFragment");
            equalizerFragment = (EqualizerFragment)getSupportFragmentManager().getFragment(savedInstanceState, "equalizerFragment");
            effectFragment = (EffectFragment)getSupportFragmentManager().getFragment(savedInstanceState, "effectFragment");
            sectionsPagerAdapter = new SectionsPagerAdapter(this, getSupportFragmentManager());
        }

        mViewPager = findViewById(R.id.container);
        mViewPager.setSwipeHold(true);
        mViewPager.setAdapter(sectionsPagerAdapter);
        mViewPager.setOffscreenPageLimit(4);
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                if(position == 0 && findViewById(R.id.relativeSongs).getVisibility() == View.VISIBLE) mViewSep1.setVisibility(View.INVISIBLE);
                else mViewSep1.setVisibility(View.VISIBLE);
                if(position == 1) loopFragment.updateCurPos();
                for(int i = 0; i < 5; i++) {
                    TabLayout.Tab tab = mTabLayout.getTabAt(i);
                    if(tab == null) continue;
                    TextView textView = (TextView)tab.getCustomView();
                    if(textView == null) continue;
                    if(i == position) {
                        int color = Color.parseColor("#FF007AFF");
                        textView.setTextColor(color);
                        for (Drawable drawable : textView.getCompoundDrawables()) {
                            if (drawable != null)
                                drawable.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
                        }
                    }
                    else {
                        int color = Color.parseColor("#FF808080");
                        textView.setTextColor(color);
                        for (Drawable drawable : textView.getCompoundDrawables()) {
                            if (drawable != null)
                                drawable.setColorFilter(null);
                        }
                    }
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
        mTabLayout.setupWithViewPager(mViewPager);

        ViewGroup vg = findViewById(R.id.layout_root);
        TextView textView0 = (TextView) LayoutInflater.from(this).inflate(R.layout.tab, vg);
        textView0.setText(R.string.playlist);
        textView0.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_playlist, 0, 0);
        int color = Color.parseColor("#FF007AFF");
        textView0.setTextColor(color);
        for (Drawable drawable : textView0.getCompoundDrawables()) {
            if (drawable != null)
                drawable.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
        }
        TabLayout.Tab tab0 = mTabLayout.getTabAt(0);
        if(tab0 != null) tab0.setCustomView(textView0);

        TextView textView1 = (TextView) LayoutInflater.from(this).inflate(R.layout.tab, vg);
        textView1.setText(R.string.loop);
        textView1.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_abloop, 0, 0);
        color = Color.parseColor("#FF808080");
        textView1.setTextColor(color);
        TabLayout.Tab tab1 = mTabLayout.getTabAt(1);
        if(tab1 != null) tab1.setCustomView(textView1);

        TextView textView2 = (TextView) LayoutInflater.from(this).inflate(R.layout.tab, vg);
        textView2.setText(R.string.control);
        textView2.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_control, 0, 0);
        textView2.setTextColor(color);
        TabLayout.Tab tab2 = mTabLayout.getTabAt(2);
        if(tab2 != null) tab2.setCustomView(textView2);

        TextView textView3 = (TextView) LayoutInflater.from(this).inflate(R.layout.tab, vg);
        textView3.setText(R.string.equalizer);
        textView3.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_equalizer, 0, 0);
        textView3.setTextColor(color);
        TabLayout.Tab tab3 = mTabLayout.getTabAt(3);
        if(tab3 != null) tab3.setCustomView(textView3);

        TextView textView4 = (TextView) LayoutInflater.from(this).inflate(R.layout.tab, vg);
        textView4.setText(R.string.effect);
        textView4.setCompoundDrawablesWithIntrinsicBounds(0, R.drawable.ic_effect, 0, 0);
        textView4.setTextColor(color);
        TabLayout.Tab tab4 = mTabLayout.getTabAt(4);
        if(tab4 != null) tab4.setCustomView(textView4);

        AnimationButton btnMenu = findViewById(R.id.btnMenu);
        btnMenu.setOnClickListener(this);

        mBtnRewind.setOnLongClickListener(this);
        mBtnRewind.setOnTouchListener(this);

        mBtnForward.setOnLongClickListener(this);
        mBtnForward.setOnTouchListener(this);

        ScrollView scrollMenu = findViewById(R.id.scrollMenu);
        scrollMenu.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if(event.getAction() == MotionEvent.ACTION_UP) {
                    findViewById(R.id.relativeSave).setBackgroundColor(Color.argb(255, 255, 255, 255));
                    findViewById(R.id.relativeLock).setBackgroundColor(Color.argb(255, 255, 255, 255));
                    findViewById(R.id.relativeAddSong).setBackgroundColor(Color.argb(255, 255, 255, 255));
                    findViewById(R.id.relativeHideAds).setBackgroundColor(Color.argb(255, 255, 255, 255));
                    findViewById(R.id.relativeItem).setBackgroundColor(Color.argb(255, 255, 255, 255));
                    findViewById(R.id.relativeReport).setBackgroundColor(Color.argb(255, 255, 255, 255));
                    findViewById(R.id.relativeReview).setBackgroundColor(Color.argb(255, 255, 255, 255));
                    findViewById(R.id.relativeInfo).setBackgroundColor(Color.argb(255, 255, 255, 255));
                }
                return false;
            }
        });
    }

    private boolean isAdsVisible() {
        return (mAdView.getVisibility() != AdView.GONE);
    }

    public void setSync()
    {
        if(mSync != 0)
        {
            BASS.BASS_ChannelRemoveSync(sStream, mSync);
            mSync = 0;
        }

        LinearLayout ABButton = findViewById(R.id.ABButton);
        LinearLayout MarkerButton = findViewById(R.id.MarkerButton);
        AnimationButton btnLoopmarker = findViewById(R.id.btnLoopmarker);

        if(effectFragment.isReverse()) {
            if(ABButton.getVisibility() == View.VISIBLE && mLoopA) // ABループ中でA位置が設定されている
                mSync = BASS.BASS_ChannelSetSync(sStream, BASS.BASS_SYNC_POS, BASS.BASS_ChannelSeconds2Bytes(sStream, mLoopAPos), EndSync, this);
            else if(MarkerButton.getVisibility() == View.VISIBLE && btnLoopmarker.isSelected()) // マーカー再生中
                mSync = BASS.BASS_ChannelSetSync(sStream, BASS.BASS_SYNC_POS, BASS.BASS_ChannelSeconds2Bytes(sStream, loopFragment.getMarkerDstPos()), EndSync, this);
            else
                mSync = BASS.BASS_ChannelSetSync(sStream, BASS.BASS_SYNC_END, 0, EndSync, this);
        }
        else {
            double mLength = BASS.BASS_ChannelBytes2Seconds(sStream, BASS.BASS_ChannelGetLength(sStream, BASS.BASS_POS_BYTE));
            if(ABButton.getVisibility() == View.VISIBLE && mLoopB) // ABループ中でB位置が設定されている
                mSync = BASS.BASS_ChannelSetSync(sStream, BASS.BASS_SYNC_POS, BASS.BASS_ChannelSeconds2Bytes(sStream, mLoopBPos), EndSync, this);
            else if(MarkerButton.getVisibility() == View.VISIBLE && btnLoopmarker.isSelected()) // マーカー再生中
                mSync = BASS.BASS_ChannelSetSync(sStream, BASS.BASS_SYNC_POS, BASS.BASS_ChannelSeconds2Bytes(sStream, loopFragment.getMarkerDstPos()), EndSync, this);
            else
                mSync = BASS.BASS_ChannelSetSync(sStream, BASS.BASS_SYNC_POS, BASS.BASS_ChannelSeconds2Bytes(sStream, mLength - 0.75), EndSync, this);
        }
    }

    public void onEnded(final boolean bForce)
    {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                LinearLayout ABButton = findViewById(R.id.ABButton);
                LinearLayout MarkerButton = findViewById(R.id.MarkerButton);
                AnimationButton btnLoopmarker = findViewById(R.id.btnLoopmarker);

                if (ABButton.getVisibility() == View.VISIBLE && (mLoopA || mLoopB) && !mPlayNextByBPos) {
                    if (effectFragment.isReverse())
                        BASS.BASS_ChannelSetPosition(sStream, BASS.BASS_ChannelSeconds2Bytes(sStream, mLoopBPos), BASS.BASS_POS_BYTE);
                    else
                        BASS.BASS_ChannelSetPosition(sStream, BASS.BASS_ChannelSeconds2Bytes(sStream, mLoopAPos), BASS.BASS_POS_BYTE);
                    setSync();
                    if (BASS.BASS_ChannelIsActive(sStream) != BASS.BASS_ACTIVE_PLAYING)
                        BASS.BASS_ChannelPlay(sStream, false);
                } else if (MarkerButton.getVisibility() == View.VISIBLE && btnLoopmarker.isSelected()) {
                    BASS.BASS_ChannelSetPosition(sStream, BASS.BASS_ChannelSeconds2Bytes(sStream, loopFragment.getMarkerSrcPos()), BASS.BASS_POS_BYTE);
                    setSync();
                    if (BASS.BASS_ChannelIsActive(sStream) != BASS.BASS_ACTIVE_PLAYING)
                        BASS.BASS_ChannelPlay(sStream, false);
                } else {
                    mWaitEnd = true;
                    final Handler handler = new Handler();
                    Runnable timer = new Runnable() {
                        public void run() {
                            if (!bForce && BASS.BASS_ChannelIsActive(sStream) == BASS.BASS_ACTIVE_PLAYING) {
                                if(mWaitEnd) {
                                    handler.postDelayed(this, 100);
                                    return;
                                }
                            }
                            mWaitEnd = false;

                            boolean bSingle = false;
                            if (mBtnShuffle.getContentDescription().toString().equals(getString(R.string.singleOn)))
                                bSingle = true;

                            boolean bRepeatSingle = false;
                            if (mBtnRepeat.getContentDescription().toString().equals(getString(R.string.repeatSingleOn)))
                                bRepeatSingle = true;

                            if (bSingle)
                                playlistFragment.playNext(false);
                            else if (bRepeatSingle)
                                BASS.BASS_ChannelPlay(sStream, true);
                            else
                                playlistFragment.playNext(true);
                        }
                    };
                    handler.postDelayed(timer, 0);
                }
            }
        });
    }

    private final BASS.SYNCPROC EndSync = new BASS.SYNCPROC()
    {
        public void SYNCPROC(int handle, int channel, int data, Object user)
        {
            MainActivity activity = (MainActivity)user;
            activity.onEnded(false);
        }
    };

    public void clearLoop()
    {
        clearLoop(true);
    }

    public void clearLoop(boolean bSave)
    {
        mLoopAPos = 0.0;
        mLoopA = false;
        mLoopBPos = 0.0;
        mLoopB = false;
        loopFragment.clearLoop(bSave);
    }

    @Override
    public void onDestroy() {
        BASS.BASS_Free();

        stopNotification();
        unbindService(mServiceConn);
        mBound = false;
        super.onDestroy();
    }

    private String readChangeLog() {
        StringBuilder sb = new StringBuilder();
        String tmp;
        BufferedReader br = null;
        boolean bFirst = true;
        try {
            br = new BufferedReader(new InputStreamReader(getResources().openRawResource(R.raw.changelog)));
            while ((tmp = br.readLine()) != null) {
                if(bFirst) bFirst = false;
                else
                    sb.append(System.getProperty("line.separator"));
                sb.append(tmp);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return sb.toString();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {

        if(keyCode == KeyEvent.KEYCODE_BACK){
            ItemFragment itemFragment = null;
            SettingFragment settingFragment = null;
            SpeedRangeSettingFragment speedRangeSettingFragment = null;
            PitchRangeSettingFragment pitchRangeSettingFragment = null;
            for (Fragment f : getSupportFragmentManager().getFragments()) {
                switch(f.getClass().getName())
                {
                    case "com.edolfzoku.hayaemon2.ItemFragment":
                        itemFragment = (ItemFragment)f;
                        break;
                    case "com.edolfzoku.hayaemon2.SettingFragment":
                        settingFragment = (SettingFragment)f;
                        break;
                    case "com.edolfzoku.hayaemon2.SpeedRangeSettingFragment":
                        speedRangeSettingFragment = (SpeedRangeSettingFragment)f;
                        break;
                    case "com.edolfzoku.hayaemon2.PitchRangeSettingFragment":
                        pitchRangeSettingFragment = (PitchRangeSettingFragment)f;
                        break;
                }
            }
            if(mDrawerLayout.isDrawerOpen(Gravity.START)) {
                mDrawerLayout.closeDrawer(Gravity.START);
                return true;
            }
            else if(mSeekCurPos.getVisibility() == View.VISIBLE) {
                downViewPlaying(false);
                return true;
            }
            else if(itemFragment != null) { // 課金アイテム画面
                findViewById(R.id.btnCloseItem).performClick();
                return true;
            }
            else if(settingFragment != null) { // オプション設定画面
                findViewById(R.id.btnCloseSetting).performClick();
                return true;
            }
            else if(speedRangeSettingFragment != null) { // 速度の表示範囲画面
                findViewById(R.id.btnReturnSpeedRangeSetting).performClick();
                return true;
            }
            else if(pitchRangeSettingFragment != null) { // 音程の表示範囲画面
                findViewById(R.id.btnReturnPitchRangeSetting).performClick();
                return true;
            }
            else if(mTabLayout.getSelectedTabPosition() == 0) { // 再生リスト画面
                if(findViewById(R.id.relativeLyrics).getVisibility() == View.VISIBLE) { // 歌詞表示画面
                    findViewById(R.id.btnFinishLyrics).performClick();
                    return true;
                }
                else if(findViewById(R.id.relativePlaylists).getVisibility() == View.VISIBLE) { // 再生リスト整理画面
                    if(playlistFragment.isSorting()) { // 並べ替え中
                        findViewById(R.id.btnSortPlaylist).performClick();
                        return true;
                    }
                    else { // 通常の再生リスト整理画面
                        playlistFragment.onPlaylistItemClick(playlistFragment.getSelectedPlaylist());
                        return true;
                    }
                }
                else if(playlistFragment.isMultiSelecting()) { // 複数選択モード中
                    playlistFragment.finishMultipleSelection();
                    return true;
                }
                else if(playlistFragment.isSorting()) { // 並べ替え中
                    findViewById(R.id.textFinishSort).performClick();
                    return true;
                }
            }
            else if(mTabLayout.getSelectedTabPosition() == 1) { // ループ画面
                mViewPager.setCurrentItem(0);
                return true;
            }
            else if(mTabLayout.getSelectedTabPosition() == 2) { // コントロール画面
                mViewPager.setCurrentItem(0);
                return true;
            }
            else if(mTabLayout.getSelectedTabPosition() == 3) { // イコライザ画面
                if(findViewById(R.id.scrollCustomEqualizer).getVisibility() == View.VISIBLE) { // カスタマイズ画面
                    findViewById(R.id.btnBackCustomize).performClick();
                }
                else mViewPager.setCurrentItem(0);
                return true;
            }
            else if(mTabLayout.getSelectedTabPosition() == 4) { // エフェクト画面
                if(findViewById(R.id.relativeEffectDetail).getVisibility() == View.VISIBLE) { // エフェクト詳細画面
                    findViewById(R.id.btnEffectBack).performClick();
                    return true;
                }
                else { // 通常のエフェクト画面
                    mViewPager.setCurrentItem(0);
                    return true;
                }
            }
        }
        return super.onKeyDown(keyCode, event);
    }

    public void sendAccessibilityEvent(String text, View source) {
        AccessibilityManager manager = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
        if(manager == null || !manager.isEnabled()) return;
        int nEventType;
        if (Build.VERSION.SDK_INT < 16) nEventType = AccessibilityEvent.TYPE_VIEW_FOCUSED;
        else nEventType = AccessibilityEventCompat.TYPE_ANNOUNCEMENT;
        AccessibilityEvent event = AccessibilityEvent.obtain(nEventType);
        event.setClassName(getClass().getName());
        event.getText().add(text);
        event.setSource(source);
        manager.sendAccessibilityEvent(event);
    }

    public static final BASS.BASS_FILEPROCS fileProcs = new BASS.BASS_FILEPROCS() {
        @Override
        public boolean FILESEEKPROC(long offset, Object user) {
            FileProcsParams params = (FileProcsParams)user;
            FileChannel fileChannel = params.fileChannel;
            if(fileChannel != null) {
                try {
                    fileChannel.position(offset);
                    return true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return false;
        }

        @Override
        public int FILEREADPROC(ByteBuffer buffer, int length, Object user) {
            FileProcsParams params = (FileProcsParams)user;
            FileChannel fileChannel = params.fileChannel;
            InputStream inputStream = params.inputStream;
            if(fileChannel != null) {
                try {
                    return fileChannel.read(buffer);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else {
                if(length == 0)
                    return 0;
                byte b[] = new byte[length];
                int r;
                try
                {
                    r = inputStream.read(b);
                }
                catch (Exception e)
                {
                    return 0;
                }
                if (r <= 0) return 0;
                buffer.put(b, 0, r);
                return r;
            }
            return 0;
        }

        @Override
        public long FILELENPROC(Object user) {
            FileProcsParams params = (FileProcsParams)user;
            FileChannel fileChannel = params.fileChannel;
            if(fileChannel != null) {
                try {
                    return fileChannel.size();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return 0;
        }

        @Override
        public void FILECLOSEPROC(Object user) {
            FileProcsParams params = (FileProcsParams)user;
            AssetFileDescriptor assetFileDescriptor = params.assetFileDescriptor;
            FileChannel fileChannel = params.fileChannel;
            InputStream inputStream = params.inputStream;
            try {
                if(fileChannel != null) fileChannel.close();
                if(assetFileDescriptor != null) assetFileDescriptor.close();
                if(inputStream != null) inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };
}
