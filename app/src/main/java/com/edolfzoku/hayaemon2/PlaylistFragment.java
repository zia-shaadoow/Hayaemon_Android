/*
 * PlaylistFragment
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

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMetadataRetriever;
import android.media.MediaMuxer;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.gson.Gson;
import com.mpatric.mp3agic.ID3v2;
import com.mpatric.mp3agic.Mp3File;
import com.un4seen.bass.BASS;
import com.un4seen.bass.BASS_FX;
import com.un4seen.bass.BASSenc;
import com.un4seen.bass.BASSenc_MP3;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.text.DateFormat;

import static android.app.Activity.RESULT_OK;

public class PlaylistFragment extends Fragment implements View.OnClickListener, View.OnLongClickListener {
    private ArrayList<String> arPlaylistNames;
    private  ArrayList<ArrayList<SongItem>> arPlaylists;
    private ArrayList<ArrayList<EffectSaver>> arEffects;
    private ArrayList<ArrayList<String>> arLyrics;
    private List<Boolean> arPlayed;
    private RecyclerView recyclerPlaylists;
    private RecyclerView recyclerTab;
    private RecyclerView recyclerSongs;
    private PlaylistsAdapter playlistsAdapter;
    private PlaylistTabAdapter tabAdapter;
    private SongsAdapter songsAdapter;
    private ItemTouchHelper playlistTouchHelper;
    private ItemTouchHelper songTouchHelper;
    private MainActivity activity = null;
    private int nPlayingPlaylist = -1;
    private int nSelectedPlaylist = 0;
    private int nPlaying;
    private int nSelectedItem;
    private boolean bSorting = false;
    private boolean bMultiSelecting = false;
    private boolean bAllowSelectNone = false;
    public int hRecord = 0;
    private ByteBuffer recbuf;
    private SongSavingTask task;
    private VideoSavingTask videoSavingTask;
    private DownloadTask downloadTask;
    private boolean bFinish = false;
    private ProgressBar progress;
    private boolean bForceNormal = false;
    private boolean bForceReverse = false;

    public ArrayList<ArrayList<SongItem>> getArPlaylists() { return arPlaylists; }
    public void setArPlaylists(ArrayList<ArrayList<SongItem>> arLists) { arPlaylists = arLists; }
    public ArrayList<ArrayList<EffectSaver>> getArEffects() { return arEffects; }
    public void setArEffects(ArrayList<ArrayList<EffectSaver>> arEffects) { this.arEffects = arEffects; }
    public ArrayList<ArrayList<String>> getArLyrics() { return arLyrics; }
    public void setArLyrics(ArrayList<ArrayList<String>> arLyrics) { this.arLyrics = arLyrics; }
    public ArrayList<String> getArPlaylistNames() { return arPlaylistNames; }
    public void setArPlaylistNames(ArrayList<String> arNames) { arPlaylistNames = arNames; }
    public int getSelectedPlaylist() { return nSelectedPlaylist; }
    public void setSelectedItem(int nSelected) { nSelectedItem = nSelected; }
    public int getSelectedItem() { return nSelectedItem; }
    public int getPlaying() { return nPlaying; }
    public int getPlayingPlaylist() { return nPlayingPlaylist; }
    public ItemTouchHelper getPlaylistTouchHelper() { return playlistTouchHelper; }
    public ItemTouchHelper getSongTouchHelper() { return songTouchHelper; }
    public boolean isSorting() { return bSorting; }
    public boolean isMultiSelecting() { return bMultiSelecting; }
    public int getSongCount(int nPlaylist) { return arPlaylists.get(nPlaylist).size(); }
    public SongsAdapter getSongsAdapter() { return songsAdapter; }
    public boolean isFinish() { return bFinish; }
    public void setProgress(int nProgress) { progress.setProgress(nProgress); }
    public boolean isSelected(int nSong) {
        ArrayList<SongItem> arSongs = arPlaylists.get(nSelectedPlaylist);
        SongItem item = arSongs.get(nSong);
        return item.isSelected();
    }
    public boolean isLock(int nSong) {
        ArrayList<EffectSaver> arEffectSavers = arEffects.get(nSelectedPlaylist);
        EffectSaver saver = arEffectSavers.get(nSong);
        return saver.isSave();
    }

    public PlaylistFragment()
    {
        nPlaying = -1;
        arPlaylistNames = new ArrayList<>();
        arPlaylists = new ArrayList<>();
        arEffects = new ArrayList<>();
        arLyrics = new ArrayList<>();
        arPlayed = new ArrayList<>();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof MainActivity) {
            activity = (MainActivity) context;
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();

        activity = null;
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.btnSortPlaylist)
        {
            if(bSorting)
            {
                recyclerPlaylists.setPadding(0, 0, 0, (int)(80 * getResources().getDisplayMetrics().density + 0.5));
                AnimationButton btnAddPlaylist = activity.findViewById(R.id.btnAddPlaylist);
                btnAddPlaylist.setVisibility(View.VISIBLE);
                bSorting = false;
                playlistsAdapter.notifyDataSetChanged();
                Button btnSortPlaylist = activity.findViewById(R.id.btnSortPlaylist);
                btnSortPlaylist.setText(R.string.sort);

                playlistTouchHelper.attachToRecyclerView(null);
            }
            else
            {
                recyclerPlaylists.setPadding(0, 0, 0, 0);
                AnimationButton btnAddPlaylist = activity.findViewById(R.id.btnAddPlaylist);
                btnAddPlaylist.setVisibility(View.GONE);
                bSorting = true;
                playlistsAdapter.notifyDataSetChanged();
                Button btnSortPlaylist = activity.findViewById(R.id.btnSortPlaylist);
                btnSortPlaylist.setText(R.string.finishSort);

                playlistTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
                    @Override
                    public boolean onMove(RecyclerView recyclerSongs, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                        final int fromPos = viewHolder.getAdapterPosition();
                        final int toPos = target.getAdapterPosition();

                        ArrayList<SongItem> arSongsTemp = arPlaylists.get(fromPos);
                        arPlaylists.remove(fromPos);
                        arPlaylists.add(toPos, arSongsTemp);

                        ArrayList<EffectSaver> arEffectSavers = arEffects.get(fromPos);
                        arEffects.remove(fromPos);
                        arEffects.add(toPos, arEffectSavers);

                        ArrayList<String> arTempLyrics = arLyrics.get(fromPos);
                        arLyrics.remove(fromPos);
                        arLyrics.add(toPos, arTempLyrics);

                        String strTemp = arPlaylistNames.get(fromPos);
                        arPlaylistNames.remove(fromPos);
                        arPlaylistNames.add(toPos, strTemp);

                        if(fromPos == nPlayingPlaylist) nPlayingPlaylist = toPos;
                        else if(fromPos < nPlayingPlaylist && nPlayingPlaylist <= toPos) nPlayingPlaylist--;
                        else if(fromPos > nPlayingPlaylist && nPlayingPlaylist >= toPos) nPlayingPlaylist++;

                        tabAdapter.notifyItemMoved(fromPos, toPos);
                        playlistsAdapter.notifyItemMoved(fromPos, toPos);

                        return true;
                    }

                    @Override
                    public void clearView(RecyclerView recyclerSongs, RecyclerView.ViewHolder viewHolder) {
                        super.clearView(recyclerSongs, viewHolder);

                        tabAdapter.notifyDataSetChanged();
                        playlistsAdapter.notifyDataSetChanged();

                        saveFiles(true, true, true, true, false);
                    }

                    @Override
                    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                    }
                });
                playlistTouchHelper.attachToRecyclerView(recyclerPlaylists);
            }
        }
        else if(v.getId() == R.id.btnAddPlaylist)
        {
            final Handler handler = new Handler();
            Runnable timer=new Runnable() {
                public void run()
                {
                    AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                    builder.setTitle(R.string.addNewList);
                    final EditText editText = new EditText (activity);
                    editText.setHint(R.string.playlist);
                    editText.setHintTextColor(Color.argb(255, 192, 192, 192));
                    editText.setText(R.string.playlist);
                    builder.setView(editText);
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            addPlaylist(editText.getText().toString());
                        }
                    });
                    builder.setNegativeButton(R.string.cancel, null);
                    final AlertDialog alertDialog = builder.create();
                    alertDialog.setOnShowListener(new DialogInterface.OnShowListener()
                    {
                        @Override
                        public void onShow(DialogInterface arg0)
                        {
                            editText.requestFocus();
                            editText.setSelection(editText.getText().toString().length());
                            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                            if (null != imm) imm.showSoftInput(editText, 0);
                        }
                    });
                    alertDialog.show();
                }
            };
            handler.postDelayed(timer, 80);
        }
        else if(v.getId() == R.id.btnRewind)
        {
            if(MainActivity.hStream == 0) return;
            if(!activity.effectFragment.isReverse() && BASS.BASS_ChannelBytes2Seconds(MainActivity.hStream, BASS.BASS_ChannelGetPosition(MainActivity.hStream, BASS.BASS_POS_BYTE)) > activity.dLoopA + 1.0)
                BASS.BASS_ChannelSetPosition(MainActivity.hStream, BASS.BASS_ChannelSeconds2Bytes(MainActivity.hStream, activity.dLoopA), BASS.BASS_POS_BYTE);
            else if(activity.effectFragment.isReverse() && BASS.BASS_ChannelBytes2Seconds(MainActivity.hStream, BASS.BASS_ChannelGetPosition(MainActivity.hStream, BASS.BASS_POS_BYTE)) < activity.dLoopA - 1.0)
                BASS.BASS_ChannelSetPosition(MainActivity.hStream, BASS.BASS_ChannelSeconds2Bytes(MainActivity.hStream, activity.dLoopB), BASS.BASS_POS_BYTE);
            else
                playPrev();
        }
        else if(v.getId() == R.id.btnPlay)
            onPlayBtnClicked();
        else if(v.getId() == R.id.btnForward)
        {
            if(MainActivity.hStream == 0) return;
            playNext(true);
        }
        else if(v.getId() == R.id.btnRecord)
        {
            startRecord();
        }
        else if(v.getId() == R.id.buttonLeft)
        {
            RelativeLayout relativeSongs = activity.findViewById(R.id.relativeSongs);
            relativeSongs.setVisibility(View.INVISIBLE);
            playlistsAdapter.notifyDataSetChanged();
            RelativeLayout relativePlaylists = activity.findViewById(R.id.relativePlaylists);
            relativePlaylists.setVisibility(View.VISIBLE);
            activity.findViewById(R.id.viewSep1).setVisibility(View.VISIBLE);
        }
        else if(v.getId() == R.id.buttonAddPlaylist_small)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(R.string.addNewList);
            final EditText editText = new EditText (activity);
            editText.setHint(R.string.playlist);
            editText.setHintTextColor(Color.argb(255, 192, 192, 192));
            editText.setText(R.string.playlist);
            builder.setView(editText);
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    addPlaylist(editText.getText().toString());
                }
            });
            builder.setNegativeButton(R.string.cancel, null);
            final AlertDialog alertDialog = builder.create();
            alertDialog.setOnShowListener(new DialogInterface.OnShowListener()
            {
                @Override
                public void onShow(DialogInterface arg0)
                {
                    editText.requestFocus();
                    editText.setSelection(editText.getText().toString().length());
                    InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (null != imm) imm.showSoftInput(editText, 0);
                }
            });
            alertDialog.show();
        }
        else if(v.getId() == R.id.btnAddSong)
        {
            final Handler handler = new Handler();
            Runnable timer=new Runnable() {
                public void run()
                {
                    final BottomMenu menu = new BottomMenu(activity);
                    menu.setTitle(getString(R.string.addSong));
                    menu.addMenu(getString(R.string.addFromLocal), R.drawable.ic_actionsheet_music, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            menu.dismiss();
                            activity.open();
                        }
                    });
                    if(Build.VERSION.SDK_INT >= 18) {
                        menu.addMenu(getString(R.string.addFromGallery), R.drawable.ic_actionsheet_film, new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                menu.dismiss();
                                activity.openGallery();
                            }
                        });
                    }
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
                                    startAddURL(editURL.getText().toString());
                                }
                            });
                            builder.setNegativeButton(R.string.cancel, null);
                            final AlertDialog alertDialog = builder.create();
                            alertDialog.setOnShowListener(new DialogInterface.OnShowListener()
                            {
                                @Override
                                public void onShow(DialogInterface arg0)
                                {
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
            };
            handler.postDelayed(timer, 80);
        }
        else if(v.getId() == R.id.textFinishSort)
        {
            recyclerSongs.setPadding(0, 0, 0, (int)(80 * getResources().getDisplayMetrics().density + 0.5));
            TextView textFinishSort = activity.findViewById(R.id.textFinishSort);
            textFinishSort.setVisibility(View.GONE);
            AnimationButton btnAddSong = activity.findViewById(R.id.btnAddSong);
            btnAddSong.setVisibility(View.VISIBLE);
            bSorting = false;
            songsAdapter.notifyDataSetChanged();

            songTouchHelper.attachToRecyclerView(null);
        }
        else if(v.getId() == R.id.btnFinishLyrics)
        {
            Button btnFinishLyrics = activity.findViewById(R.id.btnFinishLyrics);
            if(btnFinishLyrics.getText().toString().equals("閉じる")) {
                RelativeLayout relativeSongs = activity.findViewById(R.id.relativeSongs);
                relativeSongs.setVisibility(View.VISIBLE);
                RelativeLayout relativeLyrics = activity.findViewById(R.id.relativeLyrics);
                relativeLyrics.setVisibility(View.INVISIBLE);
                activity.findViewById(R.id.viewSep1).setVisibility(View.INVISIBLE);
            }
            else {
                TextView textLyrics = activity.findViewById(R.id.textLyrics);
                EditText editLyrics = activity.findViewById(R.id.editLyrics);
                AnimationButton btnEdit = activity.findViewById(R.id.btnEdit);
                TextView textNoLyrics = activity.findViewById(R.id.textNoLyrics);
                ImageView imgEdit = activity.findViewById(R.id.imgEdit);
                TextView textTapEdit = activity.findViewById(R.id.textTapEdit);
                String strLyrics = editLyrics.getText().toString();
                if(nSelectedPlaylist < 0) nSelectedPlaylist = 0;
                else if(nSelectedPlaylist >= arLyrics.size()) nSelectedPlaylist = arLyrics.size() - 1;
                ArrayList<String> arTempLyrics = arLyrics.get(nSelectedPlaylist);
                arTempLyrics.set(nSelectedItem, strLyrics);
                textLyrics.setText(strLyrics);
                btnFinishLyrics.setText(R.string.close);
                textLyrics.setText(strLyrics);
                if(strLyrics.equals("")) {
                    editLyrics.setVisibility(View.INVISIBLE);
                    textNoLyrics.setVisibility(View.VISIBLE);
                    textLyrics.setVisibility(View.INVISIBLE);
                    btnEdit.setVisibility(View.INVISIBLE);
                    imgEdit.setVisibility(View.VISIBLE);
                    textTapEdit.setVisibility(View.VISIBLE);
                }
                else {
                    editLyrics.setVisibility(View.INVISIBLE);
                    textNoLyrics.setVisibility(View.INVISIBLE);
                    textLyrics.setVisibility(View.VISIBLE);
                    btnEdit.setVisibility(View.VISIBLE);
                    imgEdit.setVisibility(View.INVISIBLE);
                    textTapEdit.setVisibility(View.INVISIBLE);
                }
                editLyrics.clearFocus();
                InputMethodManager imm = (InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                if(imm != null) imm.hideSoftInputFromWindow(editLyrics.getWindowToken(), 0);

                saveFiles(false, false, true, false, false);
            }
        }
        else if(v.getId() == R.id.btnEdit)
        {
            final Handler handler = new Handler();
            Runnable timer=new Runnable() {
                public void run()
                {
                    TextView textLyrics = activity.findViewById(R.id.textLyrics);
                    textLyrics.setVisibility(View.INVISIBLE);
                    Button btnFinishLyrics = activity.findViewById(R.id.btnFinishLyrics);
                    btnFinishLyrics.setText(R.string.done);
                    AnimationButton btnEdit = activity.findViewById(R.id.btnEdit);
                    btnEdit.setVisibility(View.INVISIBLE);
                    EditText editLyrics = activity.findViewById(R.id.editLyrics);
                    editLyrics.setText(textLyrics.getText());
                    editLyrics.setVisibility(View.VISIBLE);
                    editLyrics.requestFocus();
                    InputMethodManager imm = (InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                    if(imm != null) imm.showSoftInput(editLyrics, InputMethodManager.SHOW_IMPLICIT);
                    int nPos = editLyrics.getText().length();
                    editLyrics.setSelection(nPos);
                }
            };
            handler.postDelayed(timer, 80);
        }
        else if(v.getId() == R.id.textNoLyrics)
        {
            TextView textNoLyrics = activity.findViewById(R.id.textNoLyrics);
            textNoLyrics.setVisibility(View.INVISIBLE);
            ImageView imgEdit = activity.findViewById(R.id.imgEdit);
            imgEdit.setVisibility(View.INVISIBLE);
            TextView textTapEdit = activity.findViewById(R.id.textTapEdit);
            textTapEdit.setVisibility(View.INVISIBLE);

            TextView textLyrics = activity.findViewById(R.id.textLyrics);
            textLyrics.setVisibility(View.INVISIBLE);
            Button btnFinishLyrics = activity.findViewById(R.id.btnFinishLyrics);
            btnFinishLyrics.setText(R.string.done);
            AnimationButton btnEdit = activity.findViewById(R.id.btnEdit);
            btnEdit.setVisibility(View.INVISIBLE);
            EditText editLyrics = activity.findViewById(R.id.editLyrics);
            editLyrics.setText("");
            editLyrics.setVisibility(View.VISIBLE);
            editLyrics.requestFocus();
            InputMethodManager imm = (InputMethodManager)activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            if(imm != null) imm.showSoftInput(editLyrics, InputMethodManager.SHOW_IMPLICIT);
        }
        else if(v.getId() == R.id.btnCloseInMultipleSelection)
            finishMultipleSelection();
        else if(v.getId() == R.id.imgSelectAllInMultipleSelection)
            selectAllMultipleSelection();
        else if(v.getId() == R.id.btnDeleteInMultipleSelection)
            deleteMultipleSelection();
        else if(v.getId() == R.id.btnCopyInMultipleSelection)
            copyMultipleSelection();
        else if(v.getId() == R.id.btnMoveInMultipleSelection)
            moveMultipleSelection();
        else if(v.getId() == R.id.btnMoreInMultipleSelection)
            showMenuMultipleSelection();
    }

    @Override
    public boolean onLongClick(View v)
    {
        if(v.getId() == R.id.btnPlay) {
            final BottomMenu menu = new BottomMenu(activity);
            menu.setTitle(getString(R.string.playStop));
            if(MainActivity.hStream == 0 || BASS.BASS_ChannelIsActive(MainActivity.hStream) != BASS.BASS_ACTIVE_PLAYING || activity.effectFragment.isReverse()) {
                menu.addMenu(getString(R.string.play), R.drawable.ic_actionsheet_play, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                    menu.dismiss();
                    if(activity.effectFragment.isReverse()) activity.effectFragment.onEffectItemClick(EffectFragment.kEffectTypeReverse);
                    if(MainActivity.hStream != 0 && BASS.BASS_ChannelIsActive(MainActivity.hStream) == BASS.BASS_ACTIVE_PAUSED)
                        play();
                    else if(MainActivity.hStream == 0 || BASS.BASS_ChannelIsActive(MainActivity.hStream) != BASS.BASS_ACTIVE_PLAYING) {
                        bForceNormal = true;
                        onPlayBtnClicked();
                    }
                        }
                });
            }
            if(MainActivity.hStream != 0 && BASS.BASS_ChannelIsActive(MainActivity.hStream) == BASS.BASS_ACTIVE_PLAYING) {
                menu.addMenu(getString(R.string.pause), R.drawable.ic_actionsheet_pause, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        menu.dismiss();
                        pause();
                    }
                });
            }
            if(MainActivity.hStream == 0 || BASS.BASS_ChannelIsActive(MainActivity.hStream) != BASS.BASS_ACTIVE_PLAYING || !activity.effectFragment.isReverse()) {
                menu.addMenu(getString(R.string.reverse), R.drawable.ic_actionsheet_reverse, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        menu.dismiss();
                        if(!activity.effectFragment.isReverse()) activity.effectFragment.onEffectItemClick(EffectFragment.kEffectTypeReverse);
                        if(MainActivity.hStream != 0 && BASS.BASS_ChannelIsActive(MainActivity.hStream) == BASS.BASS_ACTIVE_PAUSED)
                            play();
                        else if(MainActivity.hStream == 0 || BASS.BASS_ChannelIsActive(MainActivity.hStream) != BASS.BASS_ACTIVE_PLAYING) {
                            bForceReverse = true;
                            onPlayBtnClicked();
                        }
                    }
                });
            }
            if(MainActivity.hStream != 0) {
                menu.addDestructiveMenu(getString(R.string.stop), R.drawable.ic_actionsheet_stop, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        menu.dismiss();
                        stop();
                            }
                    });
            }
            menu.setCancelMenu();
            menu.show();
        }
        return false;
    }

    public void onTouchMultipleSelectionItem(final int nItem)
    {
        ArrayList<SongItem> arSongs = activity.playlistFragment.getArPlaylists().get(activity.playlistFragment.getSelectedPlaylist());
        SongItem item = arSongs.get(nItem);
        item.setSelected(!item.isSelected());
        int nSelected = 0;
        for(int i = 0; i < arSongs.size(); i++) {
            if(arSongs.get(i).isSelected()) nSelected++;
        }
        if(nSelected == 0 && !bAllowSelectNone) finishMultipleSelection();
        else if(nSelected == arSongs.size()) {
            ImageView imgSelectAllInMultipleSelection = activity.findViewById(R.id.imgSelectAllInMultipleSelection);
            imgSelectAllInMultipleSelection.setImageResource(R.drawable.ic_button_check_on);
        }
        else {
            ImageView imgSelectAllInMultipleSelection = activity.findViewById(R.id.imgSelectAllInMultipleSelection);
            imgSelectAllInMultipleSelection.setImageResource(R.drawable.ic_button_check_off);
        }
    }

    public void startMultipleSelection(final int nItem)
    {
        bAllowSelectNone = (nItem == -1);
        bMultiSelecting = true;
        bSorting = false;
        ArrayList<SongItem> arSongs = arPlaylists.get(nSelectedPlaylist);
        for(int i = 0; i < arSongs.size(); i++)
            arSongs.get(i).setSelected(i == nItem);
        songsAdapter.notifyDataSetChanged();

        RecyclerView recyclerTab = activity.findViewById(R.id.recyclerTab);
        ImageButton buttonLeft = activity.findViewById(R.id.buttonLeft);
        ImageButton buttonAddPlaylist_small = activity.findViewById(R.id.buttonAddPlaylist_small);
        View devider2 = activity.findViewById(R.id.devider2);
        View viewMultipleSelection = activity.findViewById(R.id.viewMultipleSelection);
        final View viewSep1 = activity.findViewById(R.id.viewSep1);
        TextView textPlaylistInMultipleSelection = activity.findViewById(R.id.textPlaylistInMultipleSelection);
        textPlaylistInMultipleSelection.setText(arPlaylistNames.get(nSelectedPlaylist));

        int nTabHeight = recyclerTab.getHeight();
        int nDuration = 200;
        recyclerTab.animate().translationY(-nTabHeight).setDuration(nDuration).start();
        buttonLeft.animate().translationY(-nTabHeight).setDuration(nDuration).start();
        buttonAddPlaylist_small.animate().translationY(-nTabHeight).setDuration(nDuration).start();
        devider2.animate().translationY(-nTabHeight).setDuration(nDuration).start();
        int nHeight = (int)(66 *  getResources().getDisplayMetrics().density + 0.5);
        viewMultipleSelection.setTranslationY(-nHeight);
        viewMultipleSelection.setVisibility(View.VISIBLE);
        viewMultipleSelection.animate().setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                viewSep1.setVisibility(View.VISIBLE);
            }
        }).translationY(0).setDuration(nDuration).start();

        startSort();
    }

    private void finishMultipleSelection()
    {
        bMultiSelecting = false;
        bSorting = false;
        ArrayList<SongItem> arSongs = arPlaylists.get(nSelectedPlaylist);
        for(int i = 0; i < arSongs.size(); i++)
            arSongs.get(i).setSelected(false);
        songsAdapter.notifyDataSetChanged();

        RecyclerView recyclerTab = activity.findViewById(R.id.recyclerTab);
        ImageButton buttonLeft = activity.findViewById(R.id.buttonLeft);
        ImageButton buttonAddPlaylist_small = activity.findViewById(R.id.buttonAddPlaylist_small);
        View devider2 = activity.findViewById(R.id.devider2);
        final View viewMultipleSelection = activity.findViewById(R.id.viewMultipleSelection);
        View viewSep1 = activity.findViewById(R.id.viewSep1);
        viewSep1.setVisibility(View.INVISIBLE);

        int nDuration = 200;
        recyclerTab.animate().translationY(0).setDuration(nDuration).start();
        buttonLeft.animate().translationY(0).setDuration(nDuration).start();
        buttonAddPlaylist_small.animate().translationY(0).setDuration(nDuration).start();
        devider2.animate().translationY(0).setDuration(nDuration).start();
        viewMultipleSelection.animate().setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                viewMultipleSelection.setVisibility(View.GONE);
                ImageView imgSelectAllInMultipleSelection = activity.findViewById(R.id.imgSelectAllInMultipleSelection);
                imgSelectAllInMultipleSelection.setImageResource(R.drawable.ic_button_check_off);
            }
        }).translationY(-viewMultipleSelection.getHeight()).setDuration(nDuration).start();
    }

    private void selectAllMultipleSelection()
    {
        boolean bUnselectSongFounded = false;
        ArrayList<SongItem> arSongs = arPlaylists.get(nSelectedPlaylist);
        for(int i = 0; i < arSongs.size(); i++) {
            SongItem song = arSongs.get(i);
            if(!song.isSelected()) {
                bUnselectSongFounded = true;
                break;
            }
        }

        ImageView imgSelectAllInMultipleSelection = activity.findViewById(R.id.imgSelectAllInMultipleSelection);
        if(bUnselectSongFounded) {
            imgSelectAllInMultipleSelection.setImageResource(R.drawable.ic_button_check_on);
            for(int i = 0; i < arSongs.size(); i++) {
                SongItem song = arSongs.get(i);
                song.setSelected(true);
            }
        }
        else {
            imgSelectAllInMultipleSelection.setImageResource(R.drawable.ic_button_check_off);
            for(int i = 0; i < arSongs.size(); i++) {
                SongItem song = arSongs.get(i);
                song.setSelected(false);
            }
            if(!bAllowSelectNone) finishMultipleSelection();
        }
        songsAdapter.notifyDataSetChanged();
    }

    private void deleteMultipleSelection()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.delete);
        builder.setMessage(R.string.askDeleteSong);
        builder.setPositiveButton(getString(R.string.decideNot), null);
        builder.setNegativeButton(getString(R.string.doDelete), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                boolean bDeletePlaying = false; // 再生中の曲を削除したか
                ArrayList<SongItem> arSongs = arPlaylists.get(nSelectedPlaylist);
                for(int i = 0; i < arSongs.size(); i++) {
                    if (arSongs.get(i).isSelected()) {
                        if(nSelectedPlaylist == nPlayingPlaylist && i == nPlaying)
                            bDeletePlaying = true;
                        removeSong(nSelectedPlaylist, i);
                    }
                }

                if(bDeletePlaying) {
                    arSongs = arPlaylists.get(nSelectedPlaylist);
                    if(nPlaying < arSongs.size())
                        playSong(nPlaying, true);
                    else if(nPlaying > 0 && nPlaying == arSongs.size())
                        playSong(nPlaying-1, true);
                    else
                        stop();
                }
                finishMultipleSelection();
            }
        });
        final AlertDialog alertDialog = builder.create();
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener()
        {
            @Override
            public void onShow(DialogInterface arg0)
            {
                Button positiveButton = alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
                positiveButton.setTextColor(Color.argb(255, 255, 0, 0));
            }
        });
        alertDialog.show();
    }

    private void copyMultipleSelection()
    {
        final BottomMenu menu = new BottomMenu(activity);
        menu.setTitle(getString(R.string.copy));
        for(int i = 0; i < arPlaylistNames.size(); i++)
        {
            final int nPlaylistTo = i;
            menu.addMenu(arPlaylistNames.get(i), R.drawable.ic_actionsheet_folder, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    copyMultipleSelection(nPlaylistTo);
                    menu.dismiss();
                }
            });
        }
        menu.setCancelMenu();
        menu.show();
    }

    private void copyMultipleSelection(int nPlaylistTo)
    {
        ArrayList<SongItem> arSongs = arPlaylists.get(nSelectedPlaylist);
        for(int i = 0; i < arSongs.size(); i++) {
            if (arSongs.get(i).isSelected())
                copySong(nSelectedPlaylist, i, nPlaylistTo);
        }
        finishMultipleSelection();
    }

    private void moveMultipleSelection()
    {
        final BottomMenu menu = new BottomMenu(activity);
        menu.setTitle(getString(R.string.moveToAnotherPlaylist));
        for(int i = 0; i < arPlaylistNames.size(); i++)
        {
            if(nSelectedPlaylist == i) continue;
            final int nPlaylistTo = i;
            menu.addMenu(arPlaylistNames.get(i), R.drawable.ic_actionsheet_folder, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    moveMultipleSelection(nPlaylistTo);
                    menu.dismiss();
                }
            });
        }
        menu.setCancelMenu();
        menu.show();
    }

    private void moveMultipleSelection(int nPlaylistTo)
    {
        ArrayList<SongItem> arSongs = arPlaylists.get(nSelectedPlaylist);
        for(int i = 0; i < arSongs.size(); i++) {
            if (arSongs.get(i).isSelected()) {
                moveSong(nSelectedPlaylist, i, nPlaylistTo);
                i--;
            }
        }
        finishMultipleSelection();
    }

    private void showMenuMultipleSelection()
    {
        boolean bLockFounded = false;
        boolean bUnlockFounded = false;
        boolean bChangeArtworkFounded = false;
        ArrayList<SongItem> arSongs = arPlaylists.get(nSelectedPlaylist);
        ArrayList<EffectSaver> arEffectSavers = arEffects.get(nSelectedPlaylist);
        for(int i = 0; i < arSongs.size(); i++) {
            SongItem song = arSongs.get(i);
            EffectSaver saver = arEffectSavers.get(i);
            if (song.isSelected()) {
                if(song.getPathArtwork() != null && !song.getPathArtwork().equals("")) bChangeArtworkFounded = true;
                if(saver.isSave()) bLockFounded = true;
                else bUnlockFounded = true;
            }
        }

        final BottomMenu menu = new BottomMenu(activity);
        menu.setTitle(getString(R.string.selectedSongs));
        menu.addMenu(getString(R.string.changeArtwork), R.drawable.ic_actionsheet_film, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                changeArtworkMultipleSelection();
                menu.dismiss();
            }
        });
        if(bChangeArtworkFounded)
            menu.addDestructiveMenu(getString(R.string.resetArtwork), R.drawable.ic_actionsheet_initialize, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    resetArtworkMultipleSelection();
                    menu.dismiss();
                }
            });
        if(bUnlockFounded)
            menu.addMenu(getString(R.string.restoreEffect), R.drawable.ic_actionsheet_lock, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    restoreEffectMultipleSelection();
                    menu.dismiss();
                }
            });
        if(bLockFounded)
            menu.addMenu(getString(R.string.cancelRestoreEffect), R.drawable.ic_actionsheet_unlock, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    cancelRestoreEffectMultipleSelection();
                    menu.dismiss();
                }
            });
        menu.setCancelMenu();
        menu.show();
    }

    private void changeArtworkMultipleSelection()
    {
        if (Build.VERSION.SDK_INT < 19)
        {
            final Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
            startActivityForResult(intent, 4);
        }
        else
        {
            final Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            startActivityForResult(intent, 4);
        }
    }

    private void resetArtworkMultipleSelection()
    {
        ArrayList<SongItem> arSongs = arPlaylists.get(nSelectedPlaylist);
        for(int i = 0; i < arSongs.size(); i++) {
            SongItem song = arSongs.get(i);
            if(song.isSelected()) {
                boolean bFounded = false;
                // 同じアートワークを使っている曲が無いかチェック
                for(int j = 0; j < arPlaylists.size(); j++) {
                    ArrayList<SongItem> arTempSongs = arPlaylists.get(j);
                    for(int k = 0; k < arTempSongs.size(); k++) {
                        if(j == nSelectedPlaylist && k == i) continue;
                        SongItem songTemp = arTempSongs.get(k);
                        if(song.getPathArtwork() != null && songTemp.getPathArtwork() != null && song.getPathArtwork().equals(songTemp.getPathArtwork())) {
                            bFounded = true;
                            break;
                        }
                    }
                }

                // 同じアートワークを使っている曲が無ければ削除
                if(!bFounded) {
                    if(song.getPathArtwork() != null) {
                        File fileBefore = new File(song.getPathArtwork());
                        if (fileBefore.exists()) {
                            if (!fileBefore.delete()) System.out.println("ファイルの削除に失敗しました");
                            song.setPathArtwork(null);
                        }
                    }
                }

                song.setPathArtwork(null);
                if(nSelectedPlaylist == nPlayingPlaylist && i == nPlaying) {
                    MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                    Bitmap bitmap = null;
                    boolean bError = false;
                    try {
                        mmr.setDataSource(activity, Uri.parse(song.getPath()));
                    }
                    catch(Exception e) {
                        bError = true;
                    }
                    if(!bError) {
                        byte[] data = mmr.getEmbeddedPicture();
                        if(data != null) {
                            bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                        }
                    }
                    AnimationButton btnArtworkInPlayingBar = activity.findViewById(R.id.btnArtworkInPlayingBar);
                    if(bitmap != null) btnArtworkInPlayingBar.setImageBitmap(bitmap);
                    else btnArtworkInPlayingBar.setImageResource(R.drawable.ic_playing_large_artwork);
                    btnArtworkInPlayingBar.setImageBitmap(bitmap);
                }
            }
        }
        saveFiles(true, false, false, false, false);
        finishMultipleSelection();
    }

    private void restoreEffectMultipleSelection()
    {
        ArrayList<SongItem> arSongs = arPlaylists.get(nSelectedPlaylist);
        for(int i = 0; i < arSongs.size(); i++) {
            SongItem song = arSongs.get(i);
            if (song.isSelected()) {
                nSelectedItem = i;
                setSavingEffect();
            }
        }
        songsAdapter.notifyDataSetChanged();
        finishMultipleSelection();
    }

    private void cancelRestoreEffectMultipleSelection()
    {
        ArrayList<SongItem> arSongs = arPlaylists.get(nSelectedPlaylist);
        for(int i = 0; i < arSongs.size(); i++) {
            SongItem song = arSongs.get(i);
            if (song.isSelected()) {
                nSelectedItem = i;
                cancelSavingEffect();
            }
        }
        songsAdapter.notifyDataSetChanged();
        finishMultipleSelection();
    }

    public void onPlayBtnClicked()
    {
        if(BASS.BASS_ChannelIsActive(MainActivity.hStream) == BASS.BASS_ACTIVE_PLAYING)
            pause();
        else
        {
            if(BASS.BASS_ChannelIsActive(MainActivity.hStream) == BASS.BASS_ACTIVE_PAUSED)
            {
                double dPos = BASS.BASS_ChannelBytes2Seconds(MainActivity.hStream, BASS.BASS_ChannelGetPosition(MainActivity.hStream, BASS.BASS_POS_BYTE));
                double dLength = BASS.BASS_ChannelBytes2Seconds(MainActivity.hStream, BASS.BASS_ChannelGetLength(MainActivity.hStream, BASS.BASS_POS_BYTE));
                if(!activity.effectFragment.isReverse() && dPos >= dLength - 0.75) {
                    play();
                    activity.onEnded(false);
                }
                else play();
            }
            else
            {
                if(MainActivity.hStream == 0)
                {
                    if(nSelectedPlaylist < 0) nSelectedPlaylist = 0;
                    else if(nSelectedPlaylist >= arPlaylists.size()) nSelectedPlaylist = arPlaylists.size() - 1;
                    nPlayingPlaylist = nSelectedPlaylist;
                    ArrayList<SongItem> arSongs = arPlaylists.get(nSelectedPlaylist);
                    arPlayed = new ArrayList<>();
                    for(int i = 0; i < arSongs.size(); i++)
                        arPlayed.add(false);
                    playNext(true);
                }
                else
                    play();
            }
        }
    }

    public void startAddURL(String strURL)
    {
        StatFs sf = new StatFs(activity.getFilesDir().toString());
        long nFreeSpace;
        if(Build.VERSION.SDK_INT >= 18)
            nFreeSpace = sf.getAvailableBlocksLong() * sf.getBlockSizeLong();
        else
            nFreeSpace = (long)sf.getAvailableBlocks() * (long)sf.getBlockSize();
        if(nFreeSpace < 100) {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(R.string.diskFullError);
            builder.setMessage(R.string.diskFullErrorDetail);
            builder.setPositiveButton("OK", null);
            builder.show();
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.downloading);
        LinearLayout linearLayout = new LinearLayout(activity);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        progress = new ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        progress.setProgress(0);
        LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        param.topMargin = (int)(24 *  getResources().getDisplayMetrics().density + 0.5);
        param.leftMargin = (int)(16 *  getResources().getDisplayMetrics().density + 0.5);
        param.rightMargin = (int)(16 *  getResources().getDisplayMetrics().density + 0.5);
        linearLayout.addView(progress, param);
        builder.setView(linearLayout);

        String strPathTo;
        int i = 0;
        File fileForCheck;
        while (true) {
            strPathTo = activity.getFilesDir() + "/recorded" +  String.format(Locale.getDefault(), "%d", i) + ".mp3";
            fileForCheck = new File(strPathTo);
            if (!fileForCheck.exists()) break;
            i++;
        }
        bFinish = false;
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                bFinish = true;
            }
        });
        AlertDialog alert = builder.show();

        if(downloadTask != null && downloadTask.getStatus() == AsyncTask.Status.RUNNING)
            downloadTask.cancel(true);
        try
        {
            downloadTask = new DownloadTask(this, new URL(strURL), strPathTo, alert);
            downloadTask.execute(0);
        }
        catch (MalformedURLException e)
        {
            if(alert.isShowing()) alert.dismiss();
        }
    }

    public void finishAddURL(String strPathTo, AlertDialog alert, int nError)
    {
        if(alert.isShowing()) alert.dismiss();

        final File file = new File(strPathTo);
        if(nError == 1)
        {
            if(!file.delete()) System.out.println("ファイルが削除できませんでした");
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(R.string.downloadError);
            builder.setMessage(R.string.downloadErrorDetail);
            builder.setPositiveButton("OK", null);
            builder.show();
            return;
        }

        int hTempStream = BASS.BASS_StreamCreateFile(strPathTo, 0, 0, BASS.BASS_STREAM_DECODE | BASS_FX.BASS_FX_FREESOURCE);
        if(hTempStream == 0)
        {
            if(!file.delete()) System.out.println("ファイルが削除できませんでした");
            AlertDialog.Builder builder = new    AlertDialog.Builder(activity);
            builder.setTitle(R.string.playableError);
            builder.setMessage(R.string.playableErrorDetail);
            builder.setPositiveButton("OK", null);
            builder.show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.addURL);
        LinearLayout linearLayout = new LinearLayout(activity);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        final EditText editTitle = new EditText (activity);
        editTitle.setHint(R.string.title);
        editTitle.setHintTextColor(Color.argb(255, 192, 192, 192));
        DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault());
        Date date = new Date(System.currentTimeMillis());
        editTitle.setText(String.format(Locale.getDefault(), "タイトル(%s)", df.format(date)));
        final EditText editArtist = new EditText (activity);
        editArtist.setHint(R.string.artist);
        editArtist.setHintTextColor(Color.argb(255, 192, 192, 192));
        editArtist.setText("");
        linearLayout.addView(editTitle);
        linearLayout.addView(editArtist);
        builder.setView(linearLayout);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                ArrayList<SongItem> arSongs = arPlaylists.get(nSelectedPlaylist);
                SongItem item = new SongItem(String.format(Locale.getDefault(), "%d", arSongs.size()+1), editTitle.getText().toString(), editArtist.getText().toString(), file.getPath());
                arSongs.add(item);
                ArrayList<EffectSaver> arEffectSavers = arEffects.get(nSelectedPlaylist);
                EffectSaver saver = new EffectSaver();
                arEffectSavers.add(saver);
                ArrayList<String> arTempLyrics = arLyrics.get(nSelectedPlaylist);
                arTempLyrics.add(null);
                if(nSelectedPlaylist == nPlayingPlaylist) arPlayed.add(false);
                songsAdapter.notifyDataSetChanged();

                saveFiles(true, true, true, true, false);
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if(!file.delete()) System.out.println("ファイルが削除できませんでした");
            }
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if(!file.delete()) System.out.println("ファイルが削除できませんでした");
            }
        });
        final AlertDialog alertDialog = builder.create();
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener()
        {
            @Override
            public void onShow(DialogInterface arg0)
            {
                editTitle.requestFocus();
                editTitle.setSelection(editTitle.getText().toString().length());
                InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (null != imm) imm.showSoftInput(editTitle, 0);
            }
        });
        alertDialog.show();
    }

    public void startRecord()
    {
        StatFs sf = new StatFs(activity.getFilesDir().toString());
        long nFreeSpace;
        if(Build.VERSION.SDK_INT >= 18)
            nFreeSpace = sf.getAvailableBlocksLong() * sf.getBlockSizeLong();
        else
            nFreeSpace = (long)sf.getAvailableBlocks() * (long)sf.getBlockSize();
        if(nFreeSpace < 100) {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(R.string.diskFullError);
            builder.setMessage(R.string.diskFullErrorDetail);
            builder.setPositiveButton("OK", null);
            builder.show();
            return;
        }

        final RelativeLayout relativeRecording = activity.findViewById(R.id.relativeRecording);
        final TextView text = activity.findViewById(R.id.textRecordingTime);
        final AnimationButton btnStopRecording = activity.findViewById(R.id.btnStopRecording);
        btnStopRecording.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopRecord();
            }
        });

        final RelativeLayout.LayoutParams paramContainer = (RelativeLayout.LayoutParams)activity.findViewById(R.id.container).getLayoutParams();
        final RelativeLayout.LayoutParams paramRecording = (RelativeLayout.LayoutParams)relativeRecording.getLayoutParams();
        paramContainer.addRule(RelativeLayout.ABOVE, R.id.relativeRecording);
        paramContainer.bottomMargin = 0;
        if(activity.findViewById(R.id.seekCurPos).getVisibility() == View.VISIBLE)
            paramRecording.addRule(RelativeLayout.ABOVE, R.id.adView);
        else paramRecording.addRule(RelativeLayout.ABOVE, R.id.relativePlayingWithShadow);
        if(MainActivity.hStream == 0) paramRecording.bottomMargin = 0;
        else {
            if(activity.findViewById(R.id.seekCurPos).getVisibility() == View.VISIBLE)
                paramRecording.bottomMargin = (int) (60 * getResources().getDisplayMetrics().density + 0.5);
            else paramRecording.bottomMargin = (int) (-22 * getResources().getDisplayMetrics().density + 0.5);
        }

        activity.findViewById(R.id.btnAddPlaylist).setVisibility(View.INVISIBLE);
        activity.findViewById(R.id.btnAddSong).setVisibility(View.INVISIBLE);
        activity.findViewById(R.id.btnEdit).setVisibility(View.INVISIBLE);
        relativeRecording.setTranslationY((int)(64 * getResources().getDisplayMetrics().density + 0.5));
        relativeRecording.setVisibility(View.VISIBLE);
        relativeRecording.animate()
                .translationY(0)
                .setDuration(200);

        BASS.BASS_RecordInit(-1);
        recbuf = ByteBuffer.allocateDirect(200000);
        recbuf.order(ByteOrder.LITTLE_ENDIAN);
        recbuf.put(new byte[]{'R','I','F','F',0,0,0,0,'W','A','V','E','f','m','t',' ',16,0,0,0});
        recbuf.putShort((short)1);
        recbuf.putShort((short)1);
        recbuf.putInt(44100);
        recbuf.putInt(44100 * 2);
        recbuf.putShort((short)2);
        recbuf.putShort((short)16);
        recbuf.put(new byte[]{'d','a','t','a',0,0,0,0});
        BASS.RECORDPROC RecordingCallback = new BASS.RECORDPROC() {
            public boolean RECORDPROC(int handle, ByteBuffer buffer, int length, Object user) {
                try {
                    recbuf.put(buffer);
                } catch (BufferOverflowException e) {
                    ByteBuffer temp;
                    try {
                        temp = ByteBuffer.allocateDirect(recbuf.position() + length + 200000);
                    } catch (Error e2) {
                        activity.runOnUiThread(new Runnable() {
                            public void run() {
                                stopRecord();
                            }
                        });
                        return false;
                    }
                    temp.order(ByteOrder.LITTLE_ENDIAN);
                    recbuf.limit(recbuf.position());
                    recbuf.position(0);
                    temp.put(recbuf);
                    recbuf = temp;
                    recbuf.put(buffer);
                }
                return true;
            }
        };
        if(hRecord != 0) {
            stopRecord();
            return;
        }
        if(Build.VERSION.SDK_INT >= 23) {
            if (activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                activity.requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1);
                return;
            }
        }
        hRecord = BASS.BASS_RecordStart(44100, 1, 0, RecordingCallback, 0);

        AnimationButton btnRecord = activity.findViewById(R.id.btnRecord);
        btnRecord.setColorFilter(new PorterDuffColorFilter(Color.parseColor("#FF007AFF"), PorterDuff.Mode.SRC_IN));

        final Handler handler = new Handler();
        Runnable timer=new Runnable() {
            public void run()
            {
                if (hRecord == 0) return;
                double dPos = BASS.BASS_ChannelBytes2Seconds(hRecord, BASS.BASS_ChannelGetPosition(hRecord, BASS.BASS_POS_BYTE));
                int nHour = (int)(dPos / (60 * 60) % 60);
                int nMinute = (int)(dPos / 60 % 60);
                int nSecond = (int)(dPos % 60);
                int nMillisecond = (int)(dPos * 100 % 100);
                text.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d.%02d", nHour, nMinute, nSecond, nMillisecond));
                handler.postDelayed(this, 50);
            }
        };
        handler.postDelayed(timer, 50);
    }

    private void stopRecord()
    {
        final RelativeLayout.LayoutParams paramContainer = (RelativeLayout.LayoutParams)activity.findViewById(R.id.container).getLayoutParams();
        final RelativeLayout.LayoutParams paramRecording = (RelativeLayout.LayoutParams)activity.findViewById(R.id.relativeRecording).getLayoutParams();
        paramRecording.bottomMargin = 0;
        if(MainActivity.hStream == 0) paramContainer.bottomMargin = 0;
        else paramContainer.bottomMargin = (int) (-22 * getResources().getDisplayMetrics().density + 0.5);

        activity.findViewById(R.id.relativeRecording).setVisibility(View.GONE);
        activity.findViewById(R.id.btnAddPlaylist).setVisibility(View.VISIBLE);
        activity.findViewById(R.id.btnAddSong).setVisibility(View.VISIBLE);
        activity.findViewById(R.id.btnEdit).setVisibility(View.VISIBLE);

        BASS.BASS_ChannelStop(hRecord);
        hRecord = 0;

        AnimationButton btnRecord = activity.findViewById(R.id.btnRecord);
        btnRecord.clearColorFilter();

        recbuf.limit(recbuf.position());
        recbuf.putInt(4, recbuf.position()-8);
        recbuf.putInt(40, recbuf.position()-44);
        int i = 0;
        String strPath;
        File fileForCheck;
        while(true) {
            strPath = activity.getFilesDir() + "/recorded" + String.format(Locale.getDefault(), "%d", i) + ".wav";
            fileForCheck = new File(strPath);
            if(!fileForCheck.exists()) break;
            i++;
        }
        final File file = new File(strPath);
        try {
            FileChannel fc = new FileOutputStream(file).getChannel();
            recbuf.position(0);
            fc.write(recbuf);
            fc.close();
        } catch (IOException e) {
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.newRecord);
        LinearLayout linearLayout = new LinearLayout(activity);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        final EditText editTitle = new EditText (activity);
        editTitle.setHint(R.string.title);
        editTitle.setHintTextColor(Color.argb(255, 192, 192, 192));
        DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault());
        Date date = new Date(System.currentTimeMillis());
        editTitle.setText(String.format(Locale.getDefault(), "%s(%s)", getString(R.string.newRecord), df.format((date))));
        final EditText editArtist = new EditText (activity);
        editArtist.setHint(R.string.artist);
        editArtist.setHintTextColor(Color.argb(255, 192, 192, 192));
        editArtist.setText("");
        linearLayout.addView(editTitle);
        linearLayout.addView(editArtist);
        builder.setView(linearLayout);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                ArrayList<SongItem> arSongs = arPlaylists.get(nSelectedPlaylist);
                SongItem item = new SongItem(String.format(Locale.getDefault(), "%d", arSongs.size()+1), editTitle.getText().toString(), editArtist.getText().toString(), file.getPath());
                arSongs.add(item);
                ArrayList<EffectSaver> arEffectSavers = arEffects.get(nSelectedPlaylist);
                EffectSaver saver = new EffectSaver();
                arEffectSavers.add(saver);
                ArrayList<String> arTempLyrics = arLyrics.get(nSelectedPlaylist);
                arTempLyrics.add(null);
                if(nSelectedPlaylist == nPlayingPlaylist) arPlayed.add(false);
                songsAdapter.notifyDataSetChanged();

                saveFiles(true, true, true, true, false);
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if(!file.delete()) System.out.println("ファイルが削除できませんでした");
            }
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if(!file.delete()) System.out.println("ファイルが削除できませんでした");
            }
        });
        final AlertDialog alertDialog = builder.create();
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener()
        {
            @Override
            public void onShow(DialogInterface arg0)
            {
                editTitle.requestFocus();
                editTitle.setSelection(editTitle.getText().toString().length());
                InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (null != imm) imm.showSoftInput(editTitle, 0);
            }
        });
        alertDialog.show();
    }

    public void addPlaylist(String strName)
    {
        arPlaylistNames.add(strName);
        ArrayList<SongItem> arSongs = new ArrayList<>();
        arPlaylists.add(arSongs);
        ArrayList<EffectSaver> arEffectSavers = new ArrayList<>();
        arEffects.add(arEffectSavers);
        ArrayList<String> arTempLyrics = new ArrayList<>();
        arLyrics.add(arTempLyrics);
        if(activity != null)
            saveFiles(true, true, true, true, false);
        selectPlaylist(arPlaylists.size() - 1);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.fragment_playlist, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState)
    {
        super.onActivityCreated(savedInstanceState);

        activity = (MainActivity)getActivity();
        if(activity == null) return;

        tabAdapter = new PlaylistTabAdapter(activity, arPlaylistNames);
        playlistsAdapter = new PlaylistsAdapter(activity, arPlaylistNames);
        songsAdapter = new SongsAdapter(activity);

        recyclerPlaylists = activity.findViewById(R.id.recyclerPlaylists);
        recyclerPlaylists.setHasFixedSize(false);
        LinearLayoutManager playlistsManager = new LinearLayoutManager(activity);
        recyclerPlaylists.setLayoutManager(playlistsManager);
        recyclerPlaylists.setAdapter(playlistsAdapter);
        recyclerPlaylists.setOnClickListener(this);

        recyclerTab = activity.findViewById(R.id.recyclerTab);
        recyclerTab.setHasFixedSize(false);
        LinearLayoutManager tabManager = new LinearLayoutManager(activity);
        tabManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        recyclerTab.setLayoutManager(tabManager);
        recyclerTab.setAdapter(tabAdapter);

        recyclerSongs = activity.findViewById(R.id.recyclerSongs);
        recyclerSongs.setHasFixedSize(false);
        LinearLayoutManager songsManager = new LinearLayoutManager(activity);
        recyclerSongs.setLayoutManager(songsManager);
        recyclerSongs.setAdapter(songsAdapter);
        recyclerSongs.setOnClickListener(this);

        AnimationButton btnRewind = activity.findViewById(R.id.btnRewind);
        btnRewind.setOnClickListener(this);

        AnimationButton btnPlay = activity.findViewById(R.id.btnPlay);
        btnPlay.setOnClickListener(this);
        btnPlay.setOnLongClickListener(this);

        AnimationButton btnForward = activity.findViewById(R.id.btnForward);
        btnForward.setOnClickListener(this);

        AnimationButton btnRecord = activity.findViewById(R.id.btnRecord);
        btnRecord.setOnClickListener(this);

        Button btnSortPlaylist = activity.findViewById(R.id.btnSortPlaylist);
        btnSortPlaylist.setOnClickListener(this);

        AnimationButton btnAddPlaylist = activity.findViewById(R.id.btnAddPlaylist);
        btnAddPlaylist.setOnClickListener(this);

        ImageButton buttonLeft = activity.findViewById(R.id.buttonLeft);
        buttonLeft.setOnClickListener(this);

        ImageButton buttonAddPlaylist_small = activity.findViewById(R.id.buttonAddPlaylist_small);
        buttonAddPlaylist_small.setOnClickListener(this);

        AnimationButton btnAddSong = activity.findViewById(R.id.btnAddSong) ;
        btnAddSong.setOnClickListener(this);

        TextView textFinishSort = activity.findViewById(R.id.textFinishSort);
        textFinishSort.setOnClickListener(this);

        Button btnFinishLyrics = activity.findViewById(R.id.btnFinishLyrics);
        btnFinishLyrics.setOnClickListener(this);

        AnimationButton btnEdit = activity.findViewById(R.id.btnEdit);
        btnEdit.setOnClickListener(this);

        TextView textNoLyrics = activity.findViewById(R.id.textNoLyrics);
        textNoLyrics.setOnClickListener(this);

        activity.findViewById(R.id.btnCloseInMultipleSelection).setOnClickListener(this);
        activity.findViewById(R.id.imgSelectAllInMultipleSelection).setOnClickListener(this);
        activity.findViewById(R.id.btnDeleteInMultipleSelection).setOnClickListener(this);
        activity.findViewById(R.id.btnCopyInMultipleSelection).setOnClickListener(this);
        activity.findViewById(R.id.btnMoveInMultipleSelection).setOnClickListener(this);
        activity.findViewById(R.id.btnMoreInMultipleSelection).setOnClickListener(this);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(requestCode == 1)
        {
            if(resultCode == RESULT_OK)
            {
                final int takeFlags = data.getFlags()
                        & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                if(Build.VERSION.SDK_INT < 19)
                {
                    addSong(activity, data.getData());
                }
                else
                {
                    if(data.getClipData() == null)
                    {
                        addSong(activity, data.getData());
                        Uri uri = data.getData();
                        if(uri != null)
                            activity.getContentResolver().takePersistableUriPermission(uri, takeFlags);
                    }
                    else
                    {
                        for(int i = 0; i < data.getClipData().getItemCount(); i++)
                        {
                            Uri uri = data.getClipData().getItemAt(i).getUri();
                            addSong(activity, uri);
                            activity.getContentResolver().takePersistableUriPermission(uri, takeFlags);
                        }
                    }
                }
                songsAdapter.notifyDataSetChanged();
            }
        }
        else if(requestCode == 2)
        {
            if(resultCode == RESULT_OK)
            {
                final int takeFlags = data.getFlags()
                        & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                        | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                if(Build.VERSION.SDK_INT < 19)
                    addVideo(activity, data.getData());
                else
                {
                    if(data.getClipData() == null)
                    {
                        addVideo(activity, data.getData());
                        Uri uri = data.getData();
                        if(uri != null)
                            activity.getContentResolver().takePersistableUriPermission(uri, takeFlags);
                    }
                    else
                    {
                        for(int i = 0; i < data.getClipData().getItemCount(); i++)
                        {
                            Uri uri = data.getClipData().getItemAt(i).getUri();
                            addVideo(activity, uri);
                            activity.getContentResolver().takePersistableUriPermission(uri, takeFlags);
                        }
                    }
                }
                songsAdapter.notifyDataSetChanged();
            }
        }
        else if(requestCode == 3)
        {
            if(resultCode == RESULT_OK)
            {
                final int takeFlags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                if(Build.VERSION.SDK_INT < 19)
                    setArtwork(data.getData());
                else
                {
                    if(data.getClipData() == null)
                    {
                        setArtwork(data.getData());
                        Uri uri = data.getData();
                        if(uri != null)
                            activity.getContentResolver().takePersistableUriPermission(uri, takeFlags);
                    }
                    else
                    {
                        for(int i = 0; i < data.getClipData().getItemCount(); i++)
                        {
                            Uri uri = data.getClipData().getItemAt(i).getUri();
                            setArtwork(uri);
                            activity.getContentResolver().takePersistableUriPermission(uri, takeFlags);
                        }
                    }
                }
                songsAdapter.notifyDataSetChanged();
            }
        }
        else if(requestCode == 4)
        {
            if(resultCode == RESULT_OK)
            {
                final int takeFlags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                if(Build.VERSION.SDK_INT < 19)
                    setArtworkMultipleSelection(data.getData());
                else
                {
                    if(data.getClipData() == null)
                    {
                        setArtworkMultipleSelection(data.getData());
                        Uri uri = data.getData();
                        if(uri != null)
                            activity.getContentResolver().takePersistableUriPermission(uri, takeFlags);
                    }
                    else
                    {
                        for(int i = 0; i < data.getClipData().getItemCount(); i++)
                        {
                            Uri uri = data.getClipData().getItemAt(i).getUri();
                            setArtworkMultipleSelection(uri);
                            activity.getContentResolver().takePersistableUriPermission(uri, takeFlags);
                        }
                    }
                }
                songsAdapter.notifyDataSetChanged();
            }
        }

        saveFiles(true, true, true, true, false);
    }

    private void setArtwork(Uri uri)
    {
        ArrayList<SongItem> arSongs = arPlaylists.get(nSelectedPlaylist);
        SongItem song = arSongs.get(nSelectedItem);
        if(song.getPathArtwork() != null) {
            boolean bFounded = false;
            // 同じアートワークを使っている曲が無いかチェック
            for(int j = 0; j < arPlaylists.size(); j++) {
                ArrayList<SongItem> arTempSongs = arPlaylists.get(j);
                for(int k = 0; k < arTempSongs.size(); k++) {
                    if(j == nSelectedPlaylist && k == nSelectedItem) continue;
                    SongItem songTemp = arTempSongs.get(k);
                    if(song.getPathArtwork() != null && songTemp.getPathArtwork() != null && song.getPathArtwork().equals(songTemp.getPathArtwork())) {
                        bFounded = true;
                        break;
                    }
                }
            }

            // 同じアートワークを使っている曲が無ければ削除
            if(!bFounded) {
                File fileBefore = new File(song.getPathArtwork());
                if (fileBefore.exists()) {
                    if (!fileBefore.delete()) System.out.println("ファイルの削除に失敗しました");
                    song.setPathArtwork(null);
                }
            }
        }

        Bitmap bitmap;
        int nArtworkSize = getResources().getDisplayMetrics().widthPixels / 2;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(activity.getContentResolver(), uri);
        }catch (IOException e) {
            e.printStackTrace();
            return;
        }
        bitmap = ThumbnailUtils.extractThumbnail(bitmap, nArtworkSize, nArtworkSize, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
        AnimationButton btnArtworkInPlayingBar = activity.findViewById(R.id.btnArtworkInPlayingBar);
        btnArtworkInPlayingBar.setImageBitmap(bitmap);
        String strPathTo;
        int i = 0;
        File file;
        while (true) {
            strPathTo = activity.getFilesDir() + "/artwork" +  String.format(Locale.getDefault(), "%d", i) + ".png";
            file = new File(strPathTo);
            if (!file.exists()) break;
            i++;
        }

        try {
            FileOutputStream outStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream);
            outStream.close();
            song.setPathArtwork(strPathTo);
            saveFiles(true, false, false, false, false);
        }
        catch(Exception e) {
            e.printStackTrace();
        }
    }

    public void resetArtwork()
    {
        ArrayList<SongItem> arSongs = arPlaylists.get(nSelectedPlaylist);
        SongItem song = arSongs.get(nSelectedItem);
        if(song.getPathArtwork() != null) {
            boolean bFounded = false;
            // 同じアートワークを使っている曲が無いかチェック
            for(int j = 0; j < arPlaylists.size(); j++) {
                ArrayList<SongItem> arTempSongs = arPlaylists.get(j);
                for(int k = 0; k < arTempSongs.size(); k++) {
                    if(j == nSelectedPlaylist && k == nSelectedItem) continue;
                    SongItem songTemp = arTempSongs.get(k);
                    if(song.getPathArtwork() != null && songTemp.getPathArtwork() != null && song.getPathArtwork().equals(songTemp.getPathArtwork())) {
                        bFounded = true;
                        break;
                    }
                }
            }

            // 同じアートワークを使っている曲が無ければ削除
            if(!bFounded) {
                File fileBefore = new File(song.getPathArtwork());
                if (fileBefore.exists()) {
                    if (!fileBefore.delete()) System.out.println("ファイルの削除に失敗しました");
                    song.setPathArtwork(null);
                }
            }
        }
        song.setPathArtwork(null);
        AnimationButton btnArtworkInPlayingBar = activity.findViewById(R.id.btnArtworkInPlayingBar);
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        Bitmap bitmap = null;
        boolean bError = false;
        try {
            mmr.setDataSource(activity, Uri.parse(song.getPath()));
        }
        catch(Exception e) {
            bError = true;
        }
        if(!bError) {
            byte[] data = mmr.getEmbeddedPicture();
            if(data != null) {
                bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            }
        }
        if(bitmap != null) btnArtworkInPlayingBar.setImageBitmap(bitmap);
        else btnArtworkInPlayingBar.setImageResource(R.drawable.ic_playing_large_artwork);
        saveFiles(true, false, false, false, false);
    }

    private void setArtworkMultipleSelection(Uri uri)
    {
        Bitmap bitmap;
        int nArtworkSize = getResources().getDisplayMetrics().widthPixels / 2;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(activity.getContentResolver(), uri);
        }catch (IOException e) {
            e.printStackTrace();
            return;
        }
        bitmap = ThumbnailUtils.extractThumbnail(bitmap, nArtworkSize, nArtworkSize, ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
        String strPathTo;
        int i = 0;
        File file;
        while (true) {
            strPathTo = activity.getFilesDir() + "/artwork" +  String.format(Locale.getDefault(), "%d", i) + ".png";
            file = new File(strPathTo);
            if (!file.exists()) break;
            i++;
        }

        try {
            FileOutputStream outStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream);
            outStream.close();
        }
        catch(Exception e) {
            e.printStackTrace();
        }

        ArrayList<SongItem> arSongs = arPlaylists.get(nSelectedPlaylist);
        for(i = 0; i < arSongs.size(); i++) {
            SongItem song = arSongs.get(i);
            if(song.isSelected()) {
                boolean bFounded = false;
                // 同じアートワークを使っている曲が無いかチェック
                for(int j = 0; j < arPlaylists.size(); j++) {
                    ArrayList<SongItem> arTempSongs = arPlaylists.get(j);
                    for(int k = 0; k < arTempSongs.size(); k++) {
                        if(j == nSelectedPlaylist && k == i) continue;
                        SongItem songTemp = arTempSongs.get(k);
                        if(song.getPathArtwork() != null && songTemp.getPathArtwork() != null && song.getPathArtwork().equals(songTemp.getPathArtwork())) {
                            bFounded = true;
                            break;
                        }
                    }
                }

                // 同じアートワークを使っている曲が無ければ削除
                if(!bFounded) {
                    if(song.getPathArtwork() != null) {
                        File fileBefore = new File(song.getPathArtwork());
                        if (fileBefore.exists()) {
                            if (!fileBefore.delete()) System.out.println("ファイルの削除に失敗しました");
                            song.setPathArtwork(null);
                        }
                    }
                }

                song.setPathArtwork(strPathTo);
                if(nSelectedPlaylist == nPlayingPlaylist && i == nPlaying) {
                    AnimationButton btnArtworkInPlayingBar = activity.findViewById(R.id.btnArtworkInPlayingBar);
                    btnArtworkInPlayingBar.setImageBitmap(bitmap);
                }
            }
        }
        saveFiles(true, false, false, false, false);
        finishMultipleSelection();
    }

    public void showSongMenu(final int nItem)
    {
        nSelectedItem = nItem;
        ArrayList<SongItem> arSongs = arPlaylists.get(nSelectedPlaylist);
        final SongItem songItem = arSongs.get(nItem);
        String strTitle = songItem.getTitle();

        final BottomMenu menu = new BottomMenu(activity);
        menu.setTitle(strTitle);
        menu.addMenu(getString(R.string.saveExport), R.drawable.ic_actionsheet_save, new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                menu.dismiss();

                final BottomMenu menu = new BottomMenu(activity);
                menu.setTitle(getString(R.string.saveExport));
                menu.addMenu(getString(R.string.saveToApp), R.drawable.ic_actionsheet_save, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        menu.dismiss();
                        saveSongToLocal();
                    }
                });
                if(Build.VERSION.SDK_INT >= 18) {
                    menu.addMenu(getString(R.string.saveToGallery), R.drawable.ic_actionsheet_film, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            menu.dismiss();
                            saveSongToGallery();
                        }
                    });
                }
                menu.addMenu(getString(R.string.export), R.drawable.ic_actionsheet_share, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        menu.dismiss();
                        export();
                    }
                });
                menu.setCancelMenu();
                menu.show();
            }
        });
        menu.addMenu(getString(R.string.changeTitleAndArtist), R.drawable.ic_actionsheet_edit, new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                menu.dismiss();
                changeTitleAndArtist(nItem);
            }
        });
        menu.addMenu(getString(R.string.showLyrics), R.drawable.ic_actionsheet_file_text, new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                menu.dismiss();
                showLyrics();
            }
        });
        ArrayList<EffectSaver> arEffectSavers = arEffects.get(nSelectedPlaylist);
        EffectSaver saver = arEffectSavers.get(nItem);
        if(saver.isSave())
        {
            menu.addMenu(getString(R.string.cancelRestoreEffect), R.drawable.ic_actionsheet_unlock, new View.OnClickListener() {
                @Override
                public void onClick(View view)
                {
                    cancelSavingEffect();
                    songsAdapter.notifyDataSetChanged();
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
                    setSavingEffect();
                    songsAdapter.notifyDataSetChanged();
                    menu.dismiss();
                }
            });
        }
        menu.addSeparator();
        menu.addMenu(getString(R.string.copy), R.drawable.ic_actionsheet_copy, new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                menu.dismiss();
                final BottomMenu menu = new BottomMenu(activity);
                menu.setTitle(getString(R.string.copy));
                for(int i = 0; i < arPlaylistNames.size(); i++)
                {
                    final int nPlaylistTo = i;
                    menu.addMenu(arPlaylistNames.get(i), R.drawable.ic_actionsheet_folder, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            copySong(nSelectedPlaylist, nItem, nPlaylistTo);
                            menu.dismiss();
                        }
                    });
                }
                menu.setCancelMenu();
                menu.show();
            }
        });
        menu.addMenu(getString(R.string.moveToAnotherPlaylist), R.drawable.ic_actionsheet_folder_move, new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                menu.dismiss();

                final BottomMenu menu = new BottomMenu(activity);
                menu.setTitle(getString(R.string.moveToAnotherPlaylist));
                for(int i = 0; i < arPlaylistNames.size(); i++)
                {
                    if(nSelectedPlaylist == i) continue;
                    final int nPlaylistTo = i;
                    menu.addMenu(arPlaylistNames.get(i), R.drawable.ic_actionsheet_folder, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            moveSong(nSelectedPlaylist, nItem, nPlaylistTo);
                            menu.dismiss();
                        }
                    });
                }
                menu.setCancelMenu();
                menu.show();
            }
        });
        menu.addDestructiveMenu(getString(R.string.delete), R.drawable.ic_actionsheet_delete, new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                menu.dismiss();
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                String strTitle = songItem.getTitle();
                builder.setTitle(strTitle);
                builder.setMessage(R.string.askDeleteSong);
                builder.setPositiveButton(getString(R.string.decideNot), null);
                builder.setNegativeButton(getString(R.string.doDelete), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        boolean bDeletePlaying = false; // 再生中の曲を削除したか
                        if(nSelectedPlaylist == nPlayingPlaylist && nItem == nPlaying)
                            bDeletePlaying = true;
                        removeSong(nSelectedPlaylist, nItem);
                        if(bDeletePlaying) {
                            ArrayList<SongItem> arSongs = arPlaylists.get(nPlayingPlaylist);
                            if(nPlaying < arSongs.size())
                                playSong(nPlaying, true);
                            else if(nPlaying > 0 && nPlaying == arSongs.size())
                                playSong(nPlaying-1, true);
                            else
                                stop();
                        }
                    }
                });
                final AlertDialog alertDialog = builder.create();
                alertDialog.setOnShowListener(new DialogInterface.OnShowListener()
                {
                    @Override
                    public void onShow(DialogInterface arg0)
                    {
                        Button positiveButton = alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
                        positiveButton.setTextColor(Color.argb(255, 255, 0, 0));
                    }
                });
                alertDialog.show();
            }
        });
        menu.setCancelMenu();
        menu.show();
    }

    private void copySong(int nPlaylistFrom, int nItem, int nPlaylistTo)
    {
        ArrayList<SongItem> arSongsFrom = arPlaylists.get(nPlaylistFrom);
        ArrayList<SongItem> arSongsTo = arPlaylists.get(nPlaylistTo);
        SongItem itemFrom = arSongsFrom.get(nItem);
        File file = new File(itemFrom.getPath());
        String strPath = itemFrom.getPath();
        if(file.getParent().equals(activity.getFilesDir().toString()))
            strPath = activity.copyFile(Uri.parse(itemFrom.getPath())).toString();
        SongItem itemTo = new SongItem(String.format(Locale.getDefault(), "%d", arSongsTo.size()+1), itemFrom.getTitle(), itemFrom.getArtist(), strPath);
        arSongsTo.add(itemTo);

        ArrayList<EffectSaver> arEffectSaversFrom = arEffects.get(nSelectedPlaylist);
        ArrayList<EffectSaver> arEffectSaversTo = arEffects.get(nPlaylistTo);
        EffectSaver saverFrom = arEffectSaversFrom.get(nItem);
        if(saverFrom.isSave()) {
            EffectSaver saverTo = new EffectSaver(saverFrom);
            arEffectSaversTo.add(saverTo);
        }
        else {
            EffectSaver saverTo = new EffectSaver();
            arEffectSaversTo.add(saverTo);
        }

        ArrayList<String> arTempLyricsFrom = arLyrics.get(nSelectedPlaylist);
        ArrayList<String> arTempLyricsTo = arLyrics.get(nPlaylistTo);
        String strLyrics = arTempLyricsFrom.get(nItem);
        arTempLyricsTo.add(strLyrics);

        if(nPlaylistTo == nPlayingPlaylist)
            arPlayed.add(false);

        for(int i = nItem; i < arSongsFrom.size(); i++) {
            SongItem songItem = arSongsFrom.get(i);
            songItem.setNumber(String.format(Locale.getDefault(), "%d", i+1));
        }

        songsAdapter.notifyDataSetChanged();
        saveFiles(true, true, true, true, false);
    }

    private void moveSong(int nPlaylistFrom, int nItem, int nPlaylistTo)
    {
        ArrayList<SongItem> arSongsFrom = arPlaylists.get(nPlaylistFrom);
        ArrayList<SongItem> arSongsTo = arPlaylists.get(nPlaylistTo);
        SongItem item = arSongsFrom.get(nItem);
        arSongsTo.add(item);
        item.setNumber(String.format(Locale.getDefault(), "%d", arSongsTo.size()));
        arSongsFrom.remove(nItem);

        ArrayList<EffectSaver> arEffectSaversFrom = arEffects.get(nSelectedPlaylist);
        ArrayList<EffectSaver> arEffectSaversTo = arEffects.get(nPlaylistTo);
        EffectSaver saver = arEffectSaversFrom.get(nItem);
        arEffectSaversTo.add(saver);
        arEffectSaversFrom.remove(nItem);

        ArrayList<String> arTempLyricsFrom = arLyrics.get(nSelectedPlaylist);
        ArrayList<String> arTempLyricsTo = arLyrics.get(nPlaylistTo);
        String strLyrics = arTempLyricsFrom.get(nItem);
        arTempLyricsTo.add(strLyrics);
        arTempLyricsFrom.remove(nItem);

        if(nSelectedPlaylist == nPlayingPlaylist)
            arPlayed.remove(nItem);
        if(nPlaylistTo == nPlayingPlaylist)
            arPlayed.add(false);

        for(int i = nItem; i < arSongsFrom.size(); i++) {
            SongItem songItem = arSongsFrom.get(i);
            songItem.setNumber(String.format(Locale.getDefault(), "%d", i+1));
        }

        if(nSelectedPlaylist == nPlayingPlaylist) {
            if(nItem == nPlaying) {
                nPlayingPlaylist = nPlaylistTo;
                nPlaying = arSongsTo.size() - 1;
                playlistsAdapter.notifyDataSetChanged();
                tabAdapter.notifyDataSetChanged();
            }
            else if(nItem < nPlaying) nPlaying--;
        }

        songsAdapter.notifyDataSetChanged();
        saveFiles(true, true, true, true, false);
    }

    public void changeTitleAndArtist(final int nItem)
    {
        ArrayList<SongItem> arSongs = arPlaylists.get(nSelectedPlaylist);
        final SongItem songItem = arSongs.get(nItem);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.changeTitleAndArtist);
        LinearLayout linearLayout = new LinearLayout(activity);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        final EditText editTitle = new EditText (activity);
        editTitle.setHint(R.string.title);
        editTitle.setHintTextColor(Color.argb(255, 192, 192, 192));
        editTitle.setText(songItem.getTitle());
        final EditText editArtist = new EditText (activity);
        editArtist.setHint(R.string.artist);
        editArtist.setHintTextColor(Color.argb(255, 192, 192, 192));
        editArtist.setText(songItem.getArtist());
        linearLayout.addView(editTitle);
        linearLayout.addView(editArtist);
        builder.setView(linearLayout);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                songItem.setTitle(editTitle.getText().toString());
                songItem.setArtist(editArtist.getText().toString());

                if(nSelectedPlaylist == nPlayingPlaylist && nItem == nPlaying)
                {
                    TextView textTitleInPlayingBar = activity.findViewById(R.id.textTitleInPlayingBar);
                    textTitleInPlayingBar.setText(songItem.getTitle());
                    TextView textArtistInPlayingBar = activity.findViewById(R.id.textArtistInPlayingBar);
                    if(songItem.getArtist() == null || songItem.getArtist().equals(""))
                    {
                        textArtistInPlayingBar.setTextColor(Color.argb(255, 147, 156, 160));
                        textArtistInPlayingBar.setText(R.string.unknownArtist);
                    }
                    else
                    {
                        textArtistInPlayingBar.setTextColor(Color.argb(255, 102, 102, 102));
                        textArtistInPlayingBar.setText(songItem.getArtist());
                    }
                }

                songsAdapter.notifyDataSetChanged();

                saveFiles(true, true, true, true, false);
            }
        });
        builder.setNegativeButton(R.string.cancel, null);
        final AlertDialog alertDialog = builder.create();
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener()
        {
            @Override
            public void onShow(DialogInterface arg0)
            {
                editTitle.requestFocus();
                editTitle.setSelection(editTitle.getText().toString().length());
                InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (null != imm) imm.showSoftInput(editTitle, 0);
            }
        });
        alertDialog.show();
    }

    public void showPlaylistMenu(final int nPosition)
    {
        selectPlaylist(nPosition);
        String strPlaylist = arPlaylistNames.get(nPosition);

        final BottomMenu menu = new BottomMenu(activity);
        menu.setTitle(strPlaylist);
        menu.addMenu(getString(R.string.changePlaylistName), R.drawable.ic_actionsheet_edit, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                menu.dismiss();
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setTitle(R.string.changePlaylistName);
                final EditText editText = new EditText (activity);
                editText.setHint(R.string.playlist);
                editText.setHintTextColor(Color.argb(255, 192, 192, 192));
                editText.setText(arPlaylistNames.get(nPosition));
                builder.setView(editText);
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        arPlaylistNames.set(nPosition, editText.getText().toString());

                        tabAdapter.notifyDataSetChanged();
                        playlistsAdapter.notifyDataSetChanged();

                        saveFiles(true, true, true, true, false);
                    }
                });
                builder.setNegativeButton(R.string.cancel, null);
                final AlertDialog alertDialog = builder.create();
                alertDialog.setOnShowListener(new DialogInterface.OnShowListener()
                {
                    @Override
                    public void onShow(DialogInterface arg0)
                    {
                        editText.requestFocus();
                        editText.setSelection(editText.getText().toString().length());
                        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (null != imm) imm.showSoftInput(editText, 0);
                    }
                });
                alertDialog.show();
            }
        });
        menu.addMenu(getString(R.string.copyPlaylist), R.drawable.ic_actionsheet_copy, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                menu.dismiss();
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setTitle(R.string.copyPlaylist);
                final EditText editText = new EditText (activity);
                editText.setHint(R.string.playlist);
                editText.setHintTextColor(Color.argb(255, 192, 192, 192));
                editText.setText(String.format(Locale.getDefault(), "%s のコピー", arPlaylistNames.get(nPosition)));
                builder.setView(editText);
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        int nTo = nPosition + 1;
                        arPlaylistNames.add(nTo, editText.getText().toString());
                        ArrayList<SongItem> arSongs = new ArrayList<>();
                        arPlaylists.add(nTo, arSongs);
                        ArrayList<EffectSaver> arEffectSavers = new ArrayList<>();
                        arEffects.add(nTo, arEffectSavers);
                        ArrayList<String> arTempLyrics = new ArrayList<>();
                        arLyrics.add(nTo, arTempLyrics);

                        ArrayList<SongItem> arSongsFrom = arPlaylists.get(nPosition);
                        for(SongItem item : arSongsFrom) {
                            File file = new File(item.getPath());
                            String strPath = item.getPath();
                            if(file.getParent().equals(activity.getFilesDir().toString()))
                                strPath = activity.copyFile(Uri.parse(item.getPath())).toString();
                            SongItem itemTo = new SongItem(String.format(Locale.getDefault(), "%d", arSongs.size()+1), item.getTitle(), item.getArtist(), strPath);
                            arSongs.add(itemTo);
                        }

                        ArrayList<EffectSaver> arEffectSaversFrom = arEffects.get(nPosition);
                        for(EffectSaver saver : arEffectSaversFrom) {
                            if(saver.isSave()) {
                                EffectSaver saverTo = new EffectSaver(saver);
                                arEffectSavers.add(saverTo);
                            }
                            else {
                                EffectSaver saverTo = new EffectSaver();
                                arEffectSavers.add(saverTo);
                            }
                        }

                        ArrayList<String> arLyricsFrom = arLyrics.get(nSelectedPlaylist);
                        arTempLyrics.addAll(arLyricsFrom);

                        tabAdapter.notifyDataSetChanged();
                        playlistsAdapter.notifyDataSetChanged();
                        selectPlaylist(nTo);
                        if(activity != null)
                            saveFiles(true, true, true, true, false);
                    }
                });
                builder.setNegativeButton(R.string.cancel, null);
                final AlertDialog alertDialog = builder.create();
                alertDialog.setOnShowListener(new DialogInterface.OnShowListener()
                {
                    @Override
                    public void onShow(DialogInterface arg0)
                    {
                        editText.requestFocus();
                        editText.setSelection(editText.getText().toString().length());
                        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (null != imm) imm.showSoftInput(editText, 0);
                    }
                });
                alertDialog.show();
            }
        });
        menu.addDestructiveMenu(getString(R.string.emptyPlaylist), R.drawable.ic_actionsheet_folder_erase, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                menu.dismiss();
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setTitle(R.string.emptyPlaylist);
                builder.setMessage(R.string.askEmptyPlaylist);
                builder.setPositiveButton(getString(R.string.decideNot), null);
                builder.setNegativeButton(getString(R.string.doEmpty), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        ArrayList<SongItem> arSongs;
                        ArrayList<EffectSaver> arEffectSavers;
                        ArrayList<String> arTempLyrics;
                        arSongs = arPlaylists.get(nPosition);
                        arEffectSavers = arEffects.get(nPosition);
                        arTempLyrics = arLyrics.get(nPosition);
                        for(int i = 0; i < arSongs.size(); i++) {
                            SongItem song = arSongs.get(i);
                            File file = new File(song.getPath());
                            if(file.getParent() != null && file.getParent().equals(activity.getFilesDir().toString())) {
                                if(!file.delete()) System.out.println("ファイルが削除できませんでした");
                            }
                        }
                        arSongs.clear();
                        arEffectSavers.clear();
                        arTempLyrics.clear();

                        songsAdapter.notifyDataSetChanged();
                        playlistsAdapter.notifyDataSetChanged();
                        tabAdapter.notifyDataSetChanged();

                        saveFiles(true, true, true, true, false);
                    }
                });
                final AlertDialog alertDialog = builder.create();
                alertDialog.setOnShowListener(new DialogInterface.OnShowListener()
                {
                    @Override
                    public void onShow(DialogInterface arg0)
                    {
                        Button positiveButton = alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
                        positiveButton.setTextColor(Color.argb(255, 255, 0, 0));
                    }
                });
                alertDialog.show();
            }
        });
        menu.addDestructiveMenu(getString(R.string.deletePlaylist), R.drawable.ic_actionsheet_delete, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                menu.dismiss();
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setTitle(R.string.deletePlaylist);
                builder.setMessage(R.string.askDeletePlaylist);
                builder.setPositiveButton(getString(R.string.decideNot), null);
                builder.setNegativeButton(getString(R.string.doDelete), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if(nPosition == nPlayingPlaylist) stop();
                        else if(nPosition < nPlayingPlaylist) nPlayingPlaylist--;
                        ArrayList<SongItem> arSongs = arPlaylists.get(nPosition);
                        for(int i = 0; i < arSongs.size(); i++) {
                            SongItem song = arSongs.get(i);
                            File file = new File(song.getPath());
                            if(file.getParent().equals(activity.getFilesDir().toString())) {
                                if(!file.delete()) System.out.println("ファイルが削除できませんでした");
                            }
                        }
                        arPlaylists.remove(nPosition);
                        arEffects.remove(nPosition);
                        arPlaylistNames.remove(nPosition);
                        arLyrics.remove(nPosition);
                        if(arPlaylists.size() == 0)
                            addPlaylist(String.format(Locale.getDefault(), "%s 1", getString(R.string.playlist)));

                        int nSelect = nPosition;
                        if(nSelect >= arPlaylists.size()) nSelect = arPlaylists.size() - 1;

                        selectPlaylist(nSelect);

                        saveFiles(true, true, true, true, false);
                    }
                });
                final AlertDialog alertDialog = builder.create();
                alertDialog.setOnShowListener(new DialogInterface.OnShowListener()
                {
                    @Override
                    public void onShow(DialogInterface arg0)
                    {
                        Button positiveButton = alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
                        positiveButton.setTextColor(Color.argb(255, 255, 0, 0));
                    }
                });
                alertDialog.show();
            }
        });
        menu.setCancelMenu();
        menu.show();
    }

    public void showPlaylistTabMenu(final int nPosition)
    {
        selectPlaylist(nPosition);
        String strPlaylist = arPlaylistNames.get(nPosition);
        boolean bLockFounded = false;
        boolean bUnlockFounded = false;
        boolean bChangeArtworkFounded = false;
        ArrayList<SongItem> arSongs = arPlaylists.get(nSelectedPlaylist);
        ArrayList<EffectSaver> arEffectSavers = arEffects.get(nSelectedPlaylist);
        for(int i = 0; i < arSongs.size(); i++) {
            SongItem song = arSongs.get(i);
            song.setSelected(true);
            EffectSaver saver = arEffectSavers.get(i);
            if(song.getPathArtwork() != null && !song.getPathArtwork().equals("")) bChangeArtworkFounded = true;
            if(saver.isSave()) bLockFounded = true;
            else bUnlockFounded = true;
        }

        final BottomMenu menu = new BottomMenu(activity);
        menu.setTitle(strPlaylist);
        if(arSongs.size() >= 1)
            menu.addMenu(getString(R.string.selectSongs), R.drawable.ic_actionsheet_select, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    menu.dismiss();
                    startMultipleSelection(-1);
                }
            });
        if(arSongs.size() >= 2)
            menu.addMenu(getString(R.string.sortSongs), R.drawable.ic_actionsheet_sort, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    menu.dismiss();
                    recyclerSongs.setPadding(0, 0, 0, (int) (64 * getResources().getDisplayMetrics().density + 0.5));
                    TextView textFinishSort = activity.findViewById(R.id.textFinishSort);
                    textFinishSort.setVisibility(View.VISIBLE);
                    AnimationButton btnAddSong = activity.findViewById(R.id.btnAddSong);
                    btnAddSong.setVisibility(View.GONE);
                    bSorting = true;
                    menu.dismiss();
                    songsAdapter.notifyDataSetChanged();

                    startSort();
                }
            });
        if(arSongs.size() >= 1) menu.addSeparator();
        if(arSongs.size() >= 1) {
            menu.addMenu(getString(R.string.changeArtwork), R.drawable.ic_actionsheet_film, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    changeArtworkMultipleSelection();
                    menu.dismiss();
                }
            });
            if (bChangeArtworkFounded)
                menu.addDestructiveMenu(getString(R.string.resetArtwork), R.drawable.ic_actionsheet_initialize, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        resetArtworkMultipleSelection();
                        menu.dismiss();
                    }
                });
            if (bUnlockFounded)
                menu.addMenu(getString(R.string.restoreEffect), R.drawable.ic_actionsheet_lock, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        restoreEffectMultipleSelection();
                        menu.dismiss();
                    }
                });
            if (bLockFounded)
                menu.addMenu(getString(R.string.cancelRestoreEffect), R.drawable.ic_actionsheet_unlock, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        cancelRestoreEffectMultipleSelection();
                        menu.dismiss();
                    }
                });
            menu.addSeparator();
        }
        menu.addMenu(getString(R.string.changePlaylistName), R.drawable.ic_actionsheet_edit, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                menu.dismiss();
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setTitle(R.string.changePlaylistName);
                final EditText editText = new EditText (activity);
                editText.setHint(R.string.playlist);
                editText.setHintTextColor(Color.argb(255, 192, 192, 192));
                editText.setText(arPlaylistNames.get(nPosition));
                builder.setView(editText);
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        arPlaylistNames.set(nPosition, editText.getText().toString());

                        tabAdapter.notifyDataSetChanged();
                        playlistsAdapter.notifyDataSetChanged();

                        saveFiles(true, true, true, true, false);
                    }
                });
                builder.setNegativeButton(R.string.cancel, null);
                final AlertDialog alertDialog = builder.create();
                alertDialog.setOnShowListener(new DialogInterface.OnShowListener()
                {
                    @Override
                    public void onShow(DialogInterface arg0)
                    {
                        editText.requestFocus();
                        editText.setSelection(editText.getText().toString().length());
                        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (null != imm) imm.showSoftInput(editText, 0);
                    }
                });
                alertDialog.show();
            }
        });
        menu.addMenu(getString(R.string.copyPlaylist), R.drawable.ic_actionsheet_copy, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                menu.dismiss();
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setTitle(R.string.copyPlaylist);
                final EditText editText = new EditText (activity);
                editText.setHint(R.string.playlist);
                editText.setHintTextColor(Color.argb(255, 192, 192, 192));
                editText.setText(String.format(Locale.getDefault(), "%s のコピー", arPlaylistNames.get(nPosition)));
                builder.setView(editText);
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        int nTo = nPosition + 1;
                        arPlaylistNames.add(nTo, editText.getText().toString());
                        ArrayList<SongItem> arSongs = new ArrayList<>();
                        arPlaylists.add(nTo, arSongs);
                        ArrayList<EffectSaver> arEffectSavers = new ArrayList<>();
                        arEffects.add(nTo, arEffectSavers);
                        ArrayList<String> arTempLyrics = new ArrayList<>();
                        arLyrics.add(nTo, arTempLyrics);

                        ArrayList<SongItem> arSongsFrom = arPlaylists.get(nPosition);
                        for(SongItem item : arSongsFrom) {
                            File file = new File(item.getPath());
                            String strPath = item.getPath();
                            if(file.getParent().equals(activity.getFilesDir().toString()))
                                strPath = activity.copyFile(Uri.parse(item.getPath())).toString();
                            SongItem itemTo = new SongItem(String.format(Locale.getDefault(), "%d", arSongs.size()+1), item.getTitle(), item.getArtist(), strPath);
                            arSongs.add(itemTo);
                        }

                        ArrayList<EffectSaver> arEffectSaversFrom = arEffects.get(nPosition);
                        for(EffectSaver saver : arEffectSaversFrom) {
                            if(saver.isSave()) {
                                EffectSaver saverTo = new EffectSaver(saver);
                                arEffectSavers.add(saverTo);
                            }
                            else {
                                EffectSaver saverTo = new EffectSaver();
                                arEffectSavers.add(saverTo);
                            }
                        }

                        ArrayList<String> arLyricsFrom = arLyrics.get(nSelectedPlaylist);
                        arTempLyrics.addAll(arLyricsFrom);

                        tabAdapter.notifyDataSetChanged();
                        playlistsAdapter.notifyDataSetChanged();
                        selectPlaylist(nTo);
                        if(activity != null)
                            saveFiles(true, true, true, true, false);
                    }
                });
                builder.setNegativeButton(R.string.cancel, null);
                final AlertDialog alertDialog = builder.create();
                alertDialog.setOnShowListener(new DialogInterface.OnShowListener()
                {
                    @Override
                    public void onShow(DialogInterface arg0)
                    {
                        editText.requestFocus();
                        editText.setSelection(editText.getText().toString().length());
                        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                        if (null != imm) imm.showSoftInput(editText, 0);
                    }
                });
                alertDialog.show();
            }
        });
        menu.addDestructiveMenu(getString(R.string.emptyPlaylist), R.drawable.ic_actionsheet_folder_erase, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                menu.dismiss();
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setTitle(R.string.emptyPlaylist);
                builder.setMessage(R.string.askEmptyPlaylist);
                builder.setPositiveButton(getString(R.string.decideNot), null);
                builder.setNegativeButton(getString(R.string.doEmpty), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        ArrayList<SongItem> arSongs;
                        ArrayList<EffectSaver> arEffectSavers;
                        ArrayList<String> arTempLyrics;
                        arSongs = arPlaylists.get(nPosition);
                        arEffectSavers = arEffects.get(nPosition);
                        arTempLyrics = arLyrics.get(nPosition);
                        for(int i = 0; i < arSongs.size(); i++) {
                            SongItem song = arSongs.get(i);
                            File file = new File(song.getPath());
                            if(file.getParent() != null && file.getParent().equals(activity.getFilesDir().toString())) {
                                if(!file.delete()) System.out.println("ファイルが削除できませんでした");
                            }
                        }
                        arSongs.clear();
                        arEffectSavers.clear();
                        arTempLyrics.clear();

                        songsAdapter.notifyDataSetChanged();
                        playlistsAdapter.notifyDataSetChanged();
                        tabAdapter.notifyDataSetChanged();

                        saveFiles(true, true, true, true, false);
                    }
                });
                final AlertDialog alertDialog = builder.create();
                alertDialog.setOnShowListener(new DialogInterface.OnShowListener()
                {
                    @Override
                    public void onShow(DialogInterface arg0)
                    {
                        Button positiveButton = alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
                        positiveButton.setTextColor(Color.argb(255, 255, 0, 0));
                    }
                });
                alertDialog.show();
            }
        });
        menu.addDestructiveMenu(getString(R.string.deletePlaylist), R.drawable.ic_actionsheet_delete, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                menu.dismiss();
                AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                builder.setTitle(R.string.deletePlaylist);
                builder.setMessage(R.string.askDeletePlaylist);
                builder.setPositiveButton(getString(R.string.decideNot), null);
                builder.setNegativeButton(getString(R.string.doDelete), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        if(nPosition == nPlayingPlaylist) stop();
                        else if(nPosition < nPlayingPlaylist) nPlayingPlaylist--;
                        ArrayList<SongItem> arSongs = arPlaylists.get(nPosition);
                        for(int i = 0; i < arSongs.size(); i++) {
                            SongItem song = arSongs.get(i);
                            File file = new File(song.getPath());
                            if(file.getParent().equals(activity.getFilesDir().toString())) {
                                if(!file.delete()) System.out.println("ファイルが削除できませんでした");
                            }
                        }
                        arPlaylists.remove(nPosition);
                        arEffects.remove(nPosition);
                        arPlaylistNames.remove(nPosition);
                        arLyrics.remove(nPosition);
                        if(arPlaylists.size() == 0)
                            addPlaylist(String.format(Locale.getDefault(), "%s 1", getString(R.string.playlist)));

                        int nSelect = nPosition;
                        if(nSelect >= arPlaylists.size()) nSelect = arPlaylists.size() - 1;

                        selectPlaylist(nSelect);

                        saveFiles(true, true, true, true, false);
                    }
                });
                final AlertDialog alertDialog = builder.create();
                alertDialog.setOnShowListener(new DialogInterface.OnShowListener()
                {
                    @Override
                    public void onShow(DialogInterface arg0)
                    {
                        Button positiveButton = alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
                        positiveButton.setTextColor(Color.argb(255, 255, 0, 0));
                    }
                });
                alertDialog.show();
            }
        });
        menu.setCancelMenu();
        menu.show();
    }

    private void startSort()
    {
        songTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(RecyclerView recyclerSongs, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                final int fromPos = viewHolder.getAdapterPosition();
                final int toPos = target.getAdapterPosition();

                ArrayList<SongItem> arSongs = arPlaylists.get(nSelectedPlaylist);
                SongItem itemTemp = arSongs.get(fromPos);
                arSongs.remove(fromPos);
                arSongs.add(toPos, itemTemp);

                ArrayList<EffectSaver> arEffectSavers = arEffects.get(nSelectedPlaylist);
                EffectSaver saver = arEffectSavers.get(fromPos);
                arEffectSavers.remove(fromPos);
                arEffectSavers.add(toPos, saver);

                ArrayList<String> arTempLyrics = arLyrics.get(nSelectedPlaylist);
                String strLyrics = arTempLyrics.get(fromPos);
                arTempLyrics.remove(fromPos);
                arTempLyrics.add(toPos, strLyrics);

                if (nPlayingPlaylist == nSelectedPlaylist) {
                    Boolean bTemp = arPlayed.get(fromPos);
                    arPlayed.remove(fromPos);
                    arPlayed.add(toPos, bTemp);
                }

                int nStart = fromPos < toPos ? fromPos : toPos;
                for (int i = nStart; i < arSongs.size(); i++) {
                    SongItem songItem = arSongs.get(i);
                    songItem.setNumber(String.format(Locale.getDefault(), "%d", i + 1));
                }

                if (fromPos == nPlaying) nPlaying = toPos;
                else if (fromPos < nPlaying && nPlaying <= toPos) nPlaying--;
                else if (fromPos > nPlaying && nPlaying >= toPos) nPlaying++;

                songsAdapter.notifyItemMoved(fromPos, toPos);

                return true;
            }

            @Override
            public void clearView(RecyclerView recyclerSongs, RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerSongs, viewHolder);

                songsAdapter.notifyDataSetChanged();

                saveFiles(true, true, true, true, false);
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
            }
        });
        songTouchHelper.attachToRecyclerView(recyclerSongs);
    }

    public void showLyrics()
    {
        ArrayList<SongItem> arSongs = arPlaylists.get(nSelectedPlaylist);
        SongItem songItem = arSongs.get(nSelectedItem);

        ArrayList<String> arTempLyrics = arLyrics.get(nSelectedPlaylist);
        String strLyrics = arTempLyrics.get(nSelectedItem);

        TextView textLyricsTitle = activity.findViewById(R.id.textLyricsTitle);
        String strTitle = songItem.getTitle();
        if(songItem.getArtist() != null && !songItem.getArtist().equals(""))
            strTitle += " - " + songItem.getArtist();
        textLyricsTitle.setText(strTitle);

        TextView textNoLyrics = activity.findViewById(R.id.textNoLyrics);
        TextView textLyrics = activity.findViewById(R.id.textLyrics);
        AnimationButton btnEdit = activity.findViewById(R.id.btnEdit);
        ImageView imgEdit = activity.findViewById(R.id.imgEdit);
        TextView textTapEdit = activity.findViewById(R.id.textTapEdit);
        if(strLyrics == null || strLyrics.equals(""))
            strLyrics = getLyrics(nSelectedPlaylist, nSelectedItem);
        if(strLyrics == null || strLyrics.equals("")) {
            textNoLyrics.setVisibility(View.VISIBLE);
            textLyrics.setVisibility(View.INVISIBLE);
            btnEdit.setVisibility(View.INVISIBLE);
            imgEdit.setVisibility(View.VISIBLE);
            textTapEdit.setVisibility(View.VISIBLE);
        }
        else {
            textLyrics.setText(strLyrics);
            textNoLyrics.setVisibility(View.INVISIBLE);
            textLyrics.setVisibility(View.VISIBLE);
            btnEdit.setVisibility(View.VISIBLE);
            imgEdit.setVisibility(View.INVISIBLE);
            textTapEdit.setVisibility(View.INVISIBLE);
        }

        RelativeLayout relativeSongs = activity.findViewById(R.id.relativeSongs);
        relativeSongs.setVisibility(View.INVISIBLE);
        RelativeLayout relativeLyrics = activity.findViewById(R.id.relativeLyrics);
        relativeLyrics.setVisibility(View.VISIBLE);
        activity.findViewById(R.id.viewSep1).setVisibility(View.VISIBLE);
    }

    public void setSavingEffect()
    {
        ArrayList<EffectSaver> arEffectSavers = arEffects.get(nSelectedPlaylist);
        EffectSaver saver = arEffectSavers.get(nSelectedItem);
        saver.setSave(true);
        saver.setSpeed(activity.controlFragment.fSpeed);
        saver.setPitch(activity.controlFragment.fPitch);
        saver.setVol(activity.equalizerFragment.getArSeek().get(0).getProgress() - 30);
        saver.setEQ20K(activity.equalizerFragment.getArSeek().get(1).getProgress() - 30);
        saver.setEQ16K(activity.equalizerFragment.getArSeek().get(2).getProgress() - 30);
        saver.setEQ12_5K(activity.equalizerFragment.getArSeek().get(3).getProgress() - 30);
        saver.setEQ10K(activity.equalizerFragment.getArSeek().get(4).getProgress() - 30);
        saver.setEQ8K(activity.equalizerFragment.getArSeek().get(5).getProgress() - 30);
        saver.setEQ6_3K(activity.equalizerFragment.getArSeek().get(6).getProgress() - 30);
        saver.setEQ5K(activity.equalizerFragment.getArSeek().get(7).getProgress() - 30);
        saver.setEQ4K(activity.equalizerFragment.getArSeek().get(8).getProgress() - 30);
        saver.setEQ3_15K(activity.equalizerFragment.getArSeek().get(9).getProgress() - 30);
        saver.setEQ2_5K(activity.equalizerFragment.getArSeek().get(10).getProgress() - 30);
        saver.setEQ2K(activity.equalizerFragment.getArSeek().get(11).getProgress() - 30);
        saver.setEQ1_6K(activity.equalizerFragment.getArSeek().get(12).getProgress() - 30);
        saver.setEQ1_25K(activity.equalizerFragment.getArSeek().get(13).getProgress() - 30);
        saver.setEQ1K(activity.equalizerFragment.getArSeek().get(14).getProgress() - 30);
        saver.setEQ800(activity.equalizerFragment.getArSeek().get(15).getProgress() - 30);
        saver.setEQ630(activity.equalizerFragment.getArSeek().get(16).getProgress() - 30);
        saver.setEQ500(activity.equalizerFragment.getArSeek().get(17).getProgress() - 30);
        saver.setEQ400(activity.equalizerFragment.getArSeek().get(18).getProgress() - 30);
        saver.setEQ315(activity.equalizerFragment.getArSeek().get(19).getProgress() - 30);
        saver.setEQ250(activity.equalizerFragment.getArSeek().get(20).getProgress() - 30);
        saver.setEQ200(activity.equalizerFragment.getArSeek().get(21).getProgress() - 30);
        saver.setEQ160(activity.equalizerFragment.getArSeek().get(22).getProgress() - 30);
        saver.setEQ125(activity.equalizerFragment.getArSeek().get(23).getProgress() - 30);
        saver.setEQ100(activity.equalizerFragment.getArSeek().get(24).getProgress() - 30);
        saver.setEQ80(activity.equalizerFragment.getArSeek().get(25).getProgress() - 30);
        saver.setEQ63(activity.equalizerFragment.getArSeek().get(26).getProgress() - 30);
        saver.setEQ50(activity.equalizerFragment.getArSeek().get(27).getProgress() - 30);
        saver.setEQ40(activity.equalizerFragment.getArSeek().get(28).getProgress() - 30);
        saver.setEQ31_5(activity.equalizerFragment.getArSeek().get(29).getProgress() - 30);
        saver.setEQ25(activity.equalizerFragment.getArSeek().get(30).getProgress() - 30);
        saver.setEQ20(activity.equalizerFragment.getArSeek().get(31).getProgress() - 30);
        saver.setEffectItems(activity.effectFragment.getEffectItems());
        saver.setPan(activity.effectFragment.getPan());
        saver.setFreq(activity.effectFragment.getFreq());
        saver.setBPM(activity.effectFragment.getBPM());
        saver.setVol1(activity.effectFragment.getVol1());
        saver.setVol2(activity.effectFragment.getVol2());
        saver.setVol3(activity.effectFragment.getVol3());
        saver.setVol4(activity.effectFragment.getVol4());
        saver.setVol5(activity.effectFragment.getVol5());
        saver.setVol6(activity.effectFragment.getVol6());
        saver.setVol7(activity.effectFragment.getVol7());
        saver.setTimeOfIncreaseSpeed(activity.effectFragment.getTimeOfIncreaseSpeed());
        saver.setIncreaseSpeed(activity.effectFragment.getIncreaseSpeed());
        saver.setTimeOfDecreaseSpeed(activity.effectFragment.getTimeOfDecreaseSpeed());
        saver.setDecreaseSpeed(activity.effectFragment.getDecreaseSpeed());
        if(nSelectedPlaylist == nPlayingPlaylist && nSelectedItem == nPlaying) {
            LinearLayout ABButton = activity.findViewById(R.id.ABButton);
            AnimationButton btnLoopmarker = activity.findViewById(R.id.btnLoopmarker);
            if(ABButton.getVisibility() == View.VISIBLE) saver.setIsABLoop(true);
            else saver.setIsABLoop(false);
            saver.setIsLoopA(activity.bLoopA);
            saver.setLoopA(activity.dLoopA);
            saver.setIsLoopB(activity.bLoopB);
            saver.setLoopB(activity.dLoopB);
            saver.setArMarkerTime(activity.loopFragment.getArMarkerTime());
            saver.setIsLoopMarker(btnLoopmarker.isSelected());
            saver.setMarker(activity.loopFragment.getMarker());
        }

        saveFiles(false, true, false, false, false);
    }

    private void cancelSavingEffect()
    {
        ArrayList<EffectSaver> arEffectSavers = arEffects.get(nSelectedPlaylist);
        EffectSaver saver = arEffectSavers.get(nSelectedItem);
        saver.setSave(false);

        saveFiles(false, true, false, false, false);
    }

    public void updateSavingEffect()
    {
        if(MainActivity.hStream == 0 || nPlaying == -1) return;
        ArrayList<EffectSaver> arEffectSavers = arEffects.get(nPlayingPlaylist);
        EffectSaver saver = arEffectSavers.get(nPlaying);
        if(saver.isSave()) {
            saver.setSpeed(activity.controlFragment.fSpeed);
            saver.setPitch(activity.controlFragment.fPitch);
            saver.setVol(activity.equalizerFragment.getArSeek().get(0).getProgress() - 30);
            saver.setEQ20K(activity.equalizerFragment.getArSeek().get(1).getProgress() - 30);
            saver.setEQ16K(activity.equalizerFragment.getArSeek().get(2).getProgress() - 30);
            saver.setEQ12_5K(activity.equalizerFragment.getArSeek().get(3).getProgress() - 30);
            saver.setEQ10K(activity.equalizerFragment.getArSeek().get(4).getProgress() - 30);
            saver.setEQ8K(activity.equalizerFragment.getArSeek().get(5).getProgress() - 30);
            saver.setEQ6_3K(activity.equalizerFragment.getArSeek().get(6).getProgress() - 30);
            saver.setEQ5K(activity.equalizerFragment.getArSeek().get(7).getProgress() - 30);
            saver.setEQ4K(activity.equalizerFragment.getArSeek().get(8).getProgress() - 30);
            saver.setEQ3_15K(activity.equalizerFragment.getArSeek().get(9).getProgress() - 30);
            saver.setEQ2_5K(activity.equalizerFragment.getArSeek().get(10).getProgress() - 30);
            saver.setEQ2K(activity.equalizerFragment.getArSeek().get(11).getProgress() - 30);
            saver.setEQ1_6K(activity.equalizerFragment.getArSeek().get(12).getProgress() - 30);
            saver.setEQ1_25K(activity.equalizerFragment.getArSeek().get(13).getProgress() - 30);
            saver.setEQ1K(activity.equalizerFragment.getArSeek().get(14).getProgress() - 30);
            saver.setEQ800(activity.equalizerFragment.getArSeek().get(15).getProgress() - 30);
            saver.setEQ630(activity.equalizerFragment.getArSeek().get(16).getProgress() - 30);
            saver.setEQ500(activity.equalizerFragment.getArSeek().get(17).getProgress() - 30);
            saver.setEQ400(activity.equalizerFragment.getArSeek().get(18).getProgress() - 30);
            saver.setEQ315(activity.equalizerFragment.getArSeek().get(19).getProgress() - 30);
            saver.setEQ250(activity.equalizerFragment.getArSeek().get(20).getProgress() - 30);
            saver.setEQ200(activity.equalizerFragment.getArSeek().get(21).getProgress() - 30);
            saver.setEQ160(activity.equalizerFragment.getArSeek().get(22).getProgress() - 30);
            saver.setEQ125(activity.equalizerFragment.getArSeek().get(23).getProgress() - 30);
            saver.setEQ100(activity.equalizerFragment.getArSeek().get(24).getProgress() - 30);
            saver.setEQ80(activity.equalizerFragment.getArSeek().get(25).getProgress() - 30);
            saver.setEQ63(activity.equalizerFragment.getArSeek().get(26).getProgress() - 30);
            saver.setEQ50(activity.equalizerFragment.getArSeek().get(27).getProgress() - 30);
            saver.setEQ40(activity.equalizerFragment.getArSeek().get(28).getProgress() - 30);
            saver.setEQ31_5(activity.equalizerFragment.getArSeek().get(29).getProgress() - 30);
            saver.setEQ25(activity.equalizerFragment.getArSeek().get(30).getProgress() - 30);
            saver.setEQ20(activity.equalizerFragment.getArSeek().get(31).getProgress() - 30);
            saver.setEffectItems(activity.effectFragment.getEffectItems());
            saver.setPan(activity.effectFragment.getPan());
            saver.setFreq(activity.effectFragment.getFreq());
            saver.setBPM(activity.effectFragment.getBPM());
            saver.setVol1(activity.effectFragment.getVol1());
            saver.setVol2(activity.effectFragment.getVol2());
            saver.setVol3(activity.effectFragment.getVol3());
            saver.setVol4(activity.effectFragment.getVol4());
            saver.setVol5(activity.effectFragment.getVol5());
            saver.setVol6(activity.effectFragment.getVol6());
            saver.setVol7(activity.effectFragment.getVol7());
            saver.setTimeOfIncreaseSpeed(activity.effectFragment.getTimeOfIncreaseSpeed());
            saver.setIncreaseSpeed(activity.effectFragment.getIncreaseSpeed());
            saver.setTimeOfDecreaseSpeed(activity.effectFragment.getTimeOfDecreaseSpeed());
            saver.setDecreaseSpeed(activity.effectFragment.getDecreaseSpeed());
            LinearLayout ABButton = activity.findViewById(R.id.ABButton);
            AnimationButton btnLoopmarker = activity.findViewById(R.id.btnLoopmarker);
            if(ABButton.getVisibility() == View.VISIBLE) saver.setIsABLoop(true);
            else saver.setIsABLoop(false);
            saver.setIsLoopA(activity.bLoopA);
            saver.setLoopA(activity.dLoopA);
            saver.setIsLoopB(activity.bLoopB);
            saver.setLoopB(activity.dLoopB);
            saver.setArMarkerTime(activity.loopFragment.getArMarkerTime());
            saver.setIsLoopMarker(btnLoopmarker.isSelected());
            saver.setMarker(activity.loopFragment.getMarker());

            saveFiles(false, true, false, false, false);
        }
    }

    private void restoreEffect()
    {
        ArrayList<EffectSaver> arEffectSavers = arEffects.get(nPlayingPlaylist);
        EffectSaver saver = arEffectSavers.get(nPlaying);
        activity.controlFragment.setSpeed(saver.getSpeed(), false);
        activity.controlFragment.setPitch(saver.getPitch(), false);
        activity.equalizerFragment.setVol(saver.getVol(), false);
        activity.equalizerFragment.setEQ(1, saver.getEQ20K(), false);
        activity.equalizerFragment.setEQ(2, saver.getEQ16K(), false);
        activity.equalizerFragment.setEQ(3, saver.getEQ12_5K(), false);
        activity.equalizerFragment.setEQ(4, saver.getEQ10K(), false);
        activity.equalizerFragment.setEQ(5, saver.getEQ8K(), false);
        activity.equalizerFragment.setEQ(6, saver.getEQ6_3K(), false);
        activity.equalizerFragment.setEQ(7, saver.getEQ5K(), false);
        activity.equalizerFragment.setEQ(8, saver.getEQ4K(), false);
        activity.equalizerFragment.setEQ(9, saver.getEQ3_15K(), false);
        activity.equalizerFragment.setEQ(10, saver.getEQ2_5K(), false);
        activity.equalizerFragment.setEQ(11, saver.getEQ2K(), false);
        activity.equalizerFragment.setEQ(12, saver.getEQ1_6K(), false);
        activity.equalizerFragment.setEQ(13, saver.getEQ1_25K(), false);
        activity.equalizerFragment.setEQ(14, saver.getEQ1K(), false);
        activity.equalizerFragment.setEQ(15, saver.getEQ800(), false);
        activity.equalizerFragment.setEQ(16, saver.getEQ630(), false);
        activity.equalizerFragment.setEQ(17, saver.getEQ500(), false);
        activity.equalizerFragment.setEQ(18, saver.getEQ400(), false);
        activity.equalizerFragment.setEQ(19, saver.getEQ315(), false);
        activity.equalizerFragment.setEQ(20, saver.getEQ250(), false);
        activity.equalizerFragment.setEQ(21, saver.getEQ200(), false);
        activity.equalizerFragment.setEQ(22, saver.getEQ160(), false);
        activity.equalizerFragment.setEQ(23, saver.getEQ125(), false);
        activity.equalizerFragment.setEQ(24, saver.getEQ100(), false);
        activity.equalizerFragment.setEQ(25, saver.getEQ80(), false);
        activity.equalizerFragment.setEQ(26, saver.getEQ63(), false);
        activity.equalizerFragment.setEQ(27, saver.getEQ50(), false);
        activity.equalizerFragment.setEQ(28, saver.getEQ40(), false);
        activity.equalizerFragment.setEQ(29, saver.getEQ31_5(), false);
        activity.equalizerFragment.setEQ(30, saver.getEQ25(), false);
        activity.equalizerFragment.setEQ(31, saver.getEQ20(), false);
        ArrayList<EqualizerItem> arEqualizerItems = activity.equalizerFragment.getArEqualizerItems();
        for(int i = 0; i < arEqualizerItems.size(); i++) {
            EqualizerItem item = arEqualizerItems.get(i);
            item.setSelected(false);
        }
        activity.equalizerFragment.getEqualizersAdapter().notifyDataSetChanged();
        activity.effectFragment.setEffectItems(saver.getEffectItems());
        activity.effectFragment.setPan(saver.getPan(), false);
        activity.effectFragment.setFreq(saver.getFreq(), false);
        activity.effectFragment.setBPM(saver.getBPM());
        activity.effectFragment.setVol1(saver.getVol1());
        activity.effectFragment.setVol2(saver.getVol2());
        activity.effectFragment.setVol3(saver.getVol3());
        activity.effectFragment.setVol4(saver.getVol4());
        activity.effectFragment.setVol5(saver.getVol5());
        activity.effectFragment.setVol6(saver.getVol6());
        activity.effectFragment.setVol7(saver.getVol7());
        activity.effectFragment.setTimeOfIncreaseSpeed(saver.getTimeOfIncreaseSpeed());
        activity.effectFragment.setIncreaseSpeed(saver.getIncreaseSpeed());
        activity.effectFragment.setTimeOfDecreaseSpeed(saver.getTimeOfDecreaseSpeed());
        activity.effectFragment.setDecreaseSpeed(saver.getDecreaseSpeed());
        AnimationButton btnLoopmarker = activity.findViewById(R.id.btnLoopmarker);
        final RadioGroup radioGroupLoopMode = activity.findViewById(R.id.radioGroupLoopMode);
        if(saver.isABLoop()) radioGroupLoopMode.check(R.id.radioButtonABLoop);
        else radioGroupLoopMode.check(R.id.radioButtonMarkerPlay);
        if(saver.isLoopA()) activity.loopFragment.setLoopA(saver.getLoopA(), false);
        if(saver.isLoopB()) activity.loopFragment.setLoopB(saver.getLoopB(), false);
        activity.loopFragment.setArMarkerTime(saver.getArMarkerTime());
        if(saver.isLoopMarker()) {
            btnLoopmarker.setSelected(true);
            btnLoopmarker.setAlpha(0.3f);
        }
        else {
            btnLoopmarker.setSelected(false);
            btnLoopmarker.setAlpha(1.0f);
        }
        activity.loopFragment.setMarker(saver.getMarker());
    }

    private void saveSong(int nPurpose, String strFileName)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.saving);
        LinearLayout linearLayout = new LinearLayout(activity);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        progress = new ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal);
        progress.setMax(100);
        progress.setProgress(0);
        LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        param.topMargin = (int)(24 *  getResources().getDisplayMetrics().density + 0.5);
        param.leftMargin = (int)(16 *  getResources().getDisplayMetrics().density + 0.5);
        param.rightMargin = (int)(16 *  getResources().getDisplayMetrics().density + 0.5);
        linearLayout.addView(progress, param);
        builder.setView(linearLayout);

        ArrayList<SongItem> arSongs = arPlaylists.get(nSelectedPlaylist);
        SongItem item = arSongs.get(nSelectedItem);
        String strPath = item.getPath();
        int _hTempStream;
        BASS.BASS_FILEPROCS fileprocs=new BASS.BASS_FILEPROCS() {
            @Override
            public boolean FILESEEKPROC(long offset, Object user) {
                FileChannel fc=(FileChannel)user;
                try {
                    fc.position(offset);
                    return true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return false;
            }

            @Override
            public int FILEREADPROC(ByteBuffer buffer, int length, Object user) {
                FileChannel fc=(FileChannel)user;
                try {
                    return fc.read(buffer);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return 0;
            }

            @Override
            public long FILELENPROC(Object user) {
                FileChannel fc=(FileChannel)user;
                try {
                    return fc.size();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return 0;
            }

            @Override
            public void FILECLOSEPROC(Object user) {
                FileChannel fc=(FileChannel)user;
                try {
                    fc.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        Uri uri = Uri.parse(strPath);
        if(uri.getScheme() != null && uri.getScheme().equals("content")) {
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            try {
                mmr.setDataSource(activity.getApplicationContext(), Uri.parse(strPath));
            }
            catch(Exception e) {
                e.printStackTrace();
            }
            ContentResolver cr = activity.getApplicationContext().getContentResolver();

            try {
                AssetFileDescriptor afd = cr.openAssetFileDescriptor(Uri.parse(strPath), "r");
                if(afd == null) return;
                FileChannel fc = afd.createInputStream().getChannel();
                _hTempStream = BASS.BASS_StreamCreateFileUser(BASS.STREAMFILE_NOBUFFER, BASS.BASS_STREAM_DECODE, fileprocs, fc);
            } catch (Exception e) {
                return;
            }
        }
        else {
            _hTempStream = BASS.BASS_StreamCreateFile(strPath, 0, 0, BASS.BASS_STREAM_DECODE);
        }
        if(_hTempStream == 0) return;

        _hTempStream = BASS_FX.BASS_FX_ReverseCreate(_hTempStream, 2, BASS.BASS_STREAM_DECODE | BASS_FX.BASS_FX_FREESOURCE);
        _hTempStream = BASS_FX.BASS_FX_TempoCreate(_hTempStream, BASS.BASS_STREAM_DECODE | BASS_FX.BASS_FX_FREESOURCE);
        final int hTempStream = _hTempStream;
        int chan = BASS_FX.BASS_FX_TempoGetSource(hTempStream);
        if(activity.effectFragment.isReverse())
            BASS.BASS_ChannelSetAttribute(chan, BASS_FX.BASS_ATTRIB_REVERSE_DIR, BASS_FX.BASS_FX_RVS_REVERSE);
        else
            BASS.BASS_ChannelSetAttribute(chan, BASS_FX.BASS_ATTRIB_REVERSE_DIR, BASS_FX.BASS_FX_RVS_FORWARD);
        int hTempFxVol = BASS.BASS_ChannelSetFX(hTempStream, BASS_FX.BASS_FX_BFX_VOLUME, 0);
        int hTempFx20K = BASS.BASS_ChannelSetFX(hTempStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        int hTempFx16K = BASS.BASS_ChannelSetFX(hTempStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        int hTempFx12_5K = BASS.BASS_ChannelSetFX(hTempStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        int hTempFx10K = BASS.BASS_ChannelSetFX(hTempStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        int hTempFx8K = BASS.BASS_ChannelSetFX(hTempStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        int hTempFx6_3K = BASS.BASS_ChannelSetFX(hTempStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        int hTempFx5K = BASS.BASS_ChannelSetFX(hTempStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        int hTempFx4K = BASS.BASS_ChannelSetFX(hTempStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        int hTempFx3_15K = BASS.BASS_ChannelSetFX(hTempStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        int hTempFx2_5K = BASS.BASS_ChannelSetFX(hTempStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        int hTempFx2K = BASS.BASS_ChannelSetFX(hTempStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        int hTempFx1_6K = BASS.BASS_ChannelSetFX(hTempStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        int hTempFx1_25K = BASS.BASS_ChannelSetFX(hTempStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        int hTempFx1K = BASS.BASS_ChannelSetFX(hTempStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        int hTempFx800 = BASS.BASS_ChannelSetFX(hTempStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        int hTempFx630 = BASS.BASS_ChannelSetFX(hTempStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        int hTempFx500 = BASS.BASS_ChannelSetFX(hTempStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        int hTempFx400 = BASS.BASS_ChannelSetFX(hTempStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        int hTempFx315 = BASS.BASS_ChannelSetFX(hTempStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        int hTempFx250 = BASS.BASS_ChannelSetFX(hTempStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        int hTempFx200 = BASS.BASS_ChannelSetFX(hTempStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        int hTempFx160 = BASS.BASS_ChannelSetFX(hTempStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        int hTempFx125 = BASS.BASS_ChannelSetFX(hTempStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        int hTempFx100 = BASS.BASS_ChannelSetFX(hTempStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        int hTempFx80 = BASS.BASS_ChannelSetFX(hTempStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        int hTempFx63 = BASS.BASS_ChannelSetFX(hTempStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        int hTempFx50 = BASS.BASS_ChannelSetFX(hTempStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        int hTempFx40 = BASS.BASS_ChannelSetFX(hTempStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        int hTempFx31_5 = BASS.BASS_ChannelSetFX(hTempStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        int hTempFx25 = BASS.BASS_ChannelSetFX(hTempStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        int hTempFx20 = BASS.BASS_ChannelSetFX(hTempStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        BASS.BASS_ChannelSetAttribute(hTempStream, BASS_FX.BASS_ATTRIB_TEMPO, activity.controlFragment.fSpeed);
        BASS.BASS_ChannelSetAttribute(hTempStream, BASS_FX.BASS_ATTRIB_TEMPO_PITCH, activity.controlFragment.fPitch);
        int[] arHFX = new int[] {hTempFx20K, hTempFx16K, hTempFx12_5K, hTempFx10K, hTempFx8K, hTempFx6_3K, hTempFx5K, hTempFx4K, hTempFx3_15K, hTempFx2_5K, hTempFx2K, hTempFx1_6K, hTempFx1_25K, hTempFx1K, hTempFx800, hTempFx630, hTempFx500, hTempFx400, hTempFx315, hTempFx250, hTempFx200, hTempFx160, hTempFx125, hTempFx100, hTempFx80, hTempFx63, hTempFx50, hTempFx40, hTempFx31_5, hTempFx25, hTempFx20};
        float fLevel = activity.equalizerFragment.getArSeek().get(0).getProgress() - 30;
        if(fLevel == 0) fLevel = 1.0f;
        else if(fLevel < 0) fLevel = (fLevel + 30.0f) / 30.0f;
        else fLevel += 1.0f;
        BASS_FX.BASS_BFX_VOLUME vol = new BASS_FX.BASS_BFX_VOLUME();
        vol.lChannel = 0;
        vol.fVolume = fLevel;
        BASS.BASS_FXSetParameters(hTempFxVol, vol);

        for(int i = 0; i < 31; i++)
        {
            int nLevel = activity.equalizerFragment.getArSeek().get(i+1).getProgress() - 30;
            BASS_FX.BASS_BFX_PEAKEQ eq = new BASS_FX.BASS_BFX_PEAKEQ();
            eq.fBandwidth = 0.7f;
            eq.fQ = 0.0f;
            eq.lChannel = BASS_FX.BASS_BFX_CHANALL;
            eq.fGain = nLevel;
            eq.fCenter = activity.equalizerFragment.getArCenters()[i];
            BASS.BASS_FXSetParameters(arHFX[i], eq);
        }
        activity.effectFragment.applyEffect(hTempStream, item);
        String strPathTo;
        if(nPurpose == 0) // saveSongToLocal
        {
            int i = 0;
            File fileForCheck;
            while (true) {
                strPathTo = activity.getFilesDir() + "/recorded" + String.format(Locale.getDefault(), "%d", i) + ".mp3";
                fileForCheck = new File(strPathTo);
                if (!fileForCheck.exists()) break;
                i++;
            }
        }
        else if(nPurpose == 1) // export
        {
            File fileDir = new File(activity.getExternalCacheDir() + "/export");
            if(!fileDir.exists()) {
                if(!fileDir.mkdir()) System.out.println("ディレクトリが作成できませんでした");
            }
            strPathTo = activity.getExternalCacheDir() + "/export/";
            strPathTo += strFileName + ".mp3";
            File file = new File(strPathTo);
            if(file.exists()) {
                if(!file.delete()) System.out.println("ファイルが削除できませんでした");
            }
        }
        else // saveSongToGallery
        {
            File fileDir = new File(activity.getExternalCacheDir() + "/export");
            if(!fileDir.exists()) {
                if(!fileDir.mkdir()) System.out.println("ディレクトリが作成できませんでした");
            }
            strPathTo = activity.getExternalCacheDir() + "/export/export.wav";
            File file = new File(strPathTo);
            if (file.exists()) {
                if(!file.delete()) System.out.println("ファイルが削除できませんでした");
            }
        }

        double _dEnd = BASS.BASS_ChannelBytes2Seconds(hTempStream, BASS.BASS_ChannelGetLength(hTempStream, BASS.BASS_POS_BYTE));
        if(nSelectedPlaylist == nPlayingPlaylist && nSelectedItem == nPlaying)
        {
            if(activity.bLoopA)
                BASS.BASS_ChannelSetPosition(hTempStream, BASS.BASS_ChannelSeconds2Bytes(hTempStream, activity.dLoopA), BASS.BASS_POS_BYTE);
            if(activity.bLoopB)
                _dEnd = activity.dLoopB;
        }
        final double dEnd = _dEnd;
        int hTempEncode;
        if(nPurpose == 2) // saveSongToGallery
            hTempEncode = BASSenc.BASS_Encode_Start(hTempStream, strPathTo, BASSenc.BASS_ENCODE_PCM | BASSenc.BASS_ENCODE_FP_16BIT, null, null);
        else
            hTempEncode = BASSenc_MP3.BASS_Encode_MP3_StartFile(hTempStream, "", 0, strPathTo);
        final int hEncode = hTempEncode;
        bFinish = false;
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                bFinish = true;
            }
        });
        AlertDialog alert = builder.show();

        if(task != null && task.getStatus() == AsyncTask.Status.RUNNING)
            task.cancel(true);
        task = new SongSavingTask(nPurpose, this, hTempStream, hEncode, strPathTo, alert, dEnd);
        task.execute(0);
    }

    public void saveSongToLocal()
    {
        saveSong(0, null);
    }

    public void saveSongToGallery()
    {
        if(Build.VERSION.SDK_INT >= 23) {
            if (activity.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
                    activity.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                activity.requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
                return;
            }
        }
        saveSong(2, null);
    }

    public void finishSaveSongToLocal(int hTempStream, int hEncode, String strPathTo, AlertDialog alert)
    {
        if(alert.isShowing()) alert.dismiss();

        BASSenc.BASS_Encode_Stop(hEncode);
        BASS.BASS_StreamFree(hTempStream);

        if(bFinish) {
            File file = new File(strPathTo);
            if(!file.delete()) System.out.println("ファイルが削除できませんでした");
            bFinish = false;
            return;
        }

        ArrayList<SongItem> arSongs = arPlaylists.get(nSelectedPlaylist);
        SongItem item = arSongs.get(nSelectedItem);
        ArrayList<EffectSaver> arEffectSavers = arEffects.get(nSelectedPlaylist);
        EffectSaver saver = arEffectSavers.get(nSelectedItem);
        ArrayList<String> arTempLyrics = arLyrics.get(nSelectedPlaylist);
        String strLyrics = arTempLyrics.get(nSelectedItem);

        String strTitle = item.getTitle();
        float fSpeed = activity.controlFragment.fSpeed;
        float fPitch = activity.controlFragment.fPitch;
        String strSpeed = String.format(Locale.getDefault(), "%.1f%%", fSpeed + 100);
        String strPitch;
        if(fPitch >= 0.05f)
            strPitch = String.format(Locale.getDefault(), "♯%.1f", fPitch);
        else if(fPitch <= -0.05f)
            strPitch = String.format(Locale.getDefault(), "♭%.1f", fPitch * -1);
        else {
            strPitch = String.format(Locale.getDefault(), "%.1f", fPitch < 0.0f ? fPitch * -1 : fPitch);
            if(strPitch.equals("-0.0")) strPitch = "0.0";
        }

        if(fSpeed != 0.0f && fPitch != 0.0f)
            strTitle += "(" + getString(R.string.speed) + strSpeed + "," + getString(R.string.pitch) + strPitch + ")";
        else if(fSpeed != 0.0f)
            strTitle += "(" + getString(R.string.speed) + strSpeed + ")";
        else if(fPitch != 0.0f)
            strTitle += "(" + getString(R.string.pitch) + strPitch + ")";

        SongItem itemNew = new SongItem(String.format(Locale.getDefault(), "%d", arSongs.size()+1), strTitle, item.getArtist(), strPathTo);
        arSongs.add(itemNew);
        EffectSaver saverNew = new EffectSaver(saver);
        arEffectSavers.add(saverNew);
        arTempLyrics.add(strLyrics);
        if(nSelectedPlaylist == nPlayingPlaylist) arPlayed.add(false);
        songsAdapter.notifyDataSetChanged();

        saveFiles(true, true, true, true, false);
    }

    public void export()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.export);
        LinearLayout linearLayout = new LinearLayout(activity);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        final EditText editTitle = new EditText (activity);
        editTitle.setHint(R.string.fileName);
        editTitle.setHintTextColor(Color.argb(255, 192, 192, 192));
        ArrayList<SongItem> arSongs = arPlaylists.get(nSelectedPlaylist);
        SongItem item = arSongs.get(nSelectedItem);
        String strTitle = item.getTitle().replaceAll("[\\\\/:*?\"<>|]", "_");
        float fSpeed = activity.controlFragment.fSpeed;
        float fPitch = activity.controlFragment.fPitch;
        String strSpeed = String.format(Locale.getDefault(), "%.1f%%", fSpeed + 100);
        String strPitch;
        if(fPitch >= 0.05f)
            strPitch = String.format(Locale.getDefault(), "♯%.1f", fPitch);
        else if(fPitch <= -0.05f)
            strPitch = String.format(Locale.getDefault(), "♭%.1f", fPitch * -1);
        else {
            strPitch = String.format(Locale.getDefault(), "%.1f", fPitch < 0.0f ? fPitch * -1 : fPitch);
            if(strPitch.equals("-0.0")) strPitch = "0.0";
        }
        if(fSpeed != 0.0f && fPitch != 0.0f)
            strTitle += "(" + getString(R.string.speed) + strSpeed + "," + getString(R.string.pitch) + strPitch + ")";
        else if(fSpeed != 0.0f)
            strTitle += "(" + getString(R.string.speed) + strSpeed + ")";
        else if(fPitch != 0.0f)
            strTitle += "(" + getString(R.string.pitch) + strPitch + ")";
        DateFormat df = new SimpleDateFormat("_yyyyMMdd_HHmmss", Locale.getDefault());
        Date date = new Date(System.currentTimeMillis());
        editTitle.setText(String.format(Locale.getDefault(), "%s%s", strTitle, df.format(date)));
        linearLayout.addView(editTitle);
        builder.setView(linearLayout);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                saveSong(1, editTitle.getText().toString().replaceAll("[\\\\/:*?\"<>|]", "_"));
            }
        });
        builder.setNegativeButton(R.string.cancel, null);
        final AlertDialog alertDialog = builder.create();
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener()
        {
            @Override
            public void onShow(DialogInterface arg0)
            {
                editTitle.requestFocus();
                editTitle.setSelection(editTitle.getText().toString().length());
                InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (null != imm) imm.showSoftInput(editTitle, 0);
            }
        });
        alertDialog.show();
    }

    public void finishExport(int hTempStream, int hEncode, String strPathTo, AlertDialog alert)
    {
        if(alert.isShowing()) alert.dismiss();

        BASSenc.BASS_Encode_Stop(hEncode);
        BASS.BASS_StreamFree(hTempStream);

        if(bFinish) {
            File file = new File(strPathTo);
            if(!file.delete()) System.out.println("ファイルが削除できませんでした");
            bFinish = false;
            return;
        }

        Intent share = new Intent();
        share.setAction(Intent.ACTION_SEND);
        share.setType("audio/mp3");
        File file = new File(strPathTo);
        Uri uri = FileProvider.getUriForFile(activity, "com.edolfzoku.hayaemon2", file);
        PackageManager pm = activity.getPackageManager();
        int flag;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) flag = PackageManager.MATCH_ALL;
        else flag = PackageManager.MATCH_DEFAULT_ONLY;
        List<ResolveInfo> resInfoList = pm.queryIntentActivities(share, flag);
        for (ResolveInfo resolveInfo : resInfoList) {
            String packageName = resolveInfo.activityInfo.packageName;
            activity.grantUriPermission(packageName, uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        share.putExtra(Intent.EXTRA_STREAM, uri);
        startActivityForResult(Intent.createChooser(share, getString(R.string.export)), 0);

        file.deleteOnExit();
    }

    public void finishSaveSongToGallery(int hTempStream, int hEncode, String strPathTo, AlertDialog alert)
    {
        BASSenc.BASS_Encode_Stop(hEncode);
        int nLength = (int)BASS.BASS_ChannelBytes2Seconds(hTempStream, BASS.BASS_ChannelGetLength(hTempStream, BASS.BASS_POS_BYTE)) + 1;
        BASS.BASS_StreamFree(hTempStream);

        if (bFinish) {
            if (alert.isShowing()) alert.dismiss();
            File file = new File(strPathTo);
            if(!file.delete()) System.out.println("ファイルが削除できませんでした");
            bFinish = false;
            return;
        }

        if(videoSavingTask != null && videoSavingTask.getStatus() == AsyncTask.Status.RUNNING)
            videoSavingTask.cancel(true);
        videoSavingTask = new VideoSavingTask(this, strPathTo, alert, nLength);
        videoSavingTask.execute(0);
    }

    public void finishSaveSongToGallery2(int nLength, String strMP4Path, AlertDialog alert, String strPathTo)
    {
        if (alert.isShowing()) alert.dismiss();

        if (bFinish) {
            File file = new File(strPathTo);
            if(!file.delete()) System.out.println("ファイルが削除できませんでした");
            bFinish = false;
            return;
        }

        ContentValues values = new ContentValues();
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
        values.put(MediaStore.Video.Media.DURATION, nLength * 1000);
        values.put("_data", strMP4Path);
        ContentResolver cr = activity.getContentResolver();
        cr.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.saveToGallery);
        builder.setMessage(R.string.saved);
        builder.setPositiveButton("OK", null);
        builder.show();
    }

    public void play()
    {
        if(MainActivity.hStream == 0) return;
        BASS.BASS_ChannelPlay(MainActivity.hStream, false);
        AnimationButton btnPlay = activity.findViewById(R.id.btnPlay);
        btnPlay.setContentDescription(getString(R.string.pause));
        btnPlay.setImageResource(R.drawable.ic_bar_button_pause);
        AnimationButton btnPlayInPlayingBar = activity.findViewById(R.id.btnPlayInPlayingBar);
        btnPlayInPlayingBar.setContentDescription(getString(R.string.pause));
        if(activity.findViewById(R.id.seekCurPos).getVisibility() == View.VISIBLE)
            btnPlayInPlayingBar.setImageResource(R.drawable.ic_playing_large_pause);
        else btnPlayInPlayingBar.setImageResource(R.drawable.ic_bar_button_pause);
        songsAdapter.notifyDataSetChanged();
        playlistsAdapter.notifyDataSetChanged();
        tabAdapter.notifyDataSetChanged();
        activity.getForegroundService().setMainActivity(activity);
        activity.getForegroundService().startForeground();
    }

    public void pause()
    {
        if(MainActivity.hStream == 0) return;
        BASS.BASS_ChannelPause(MainActivity.hStream);
        AnimationButton btnPlay = activity.findViewById(R.id.btnPlay);
        btnPlay.setContentDescription(getString(R.string.play));
        btnPlay.setImageResource(R.drawable.ic_bar_button_play);
        AnimationButton btnPlayInPlayingBar = activity.findViewById(R.id.btnPlayInPlayingBar);
        btnPlayInPlayingBar.setContentDescription(getString(R.string.play));
        if(activity.findViewById(R.id.seekCurPos).getVisibility() == View.VISIBLE)
            btnPlayInPlayingBar.setImageResource(R.drawable.ic_playing_large_play);
        else btnPlayInPlayingBar.setImageResource(R.drawable.ic_bar_button_play);
        songsAdapter.notifyDataSetChanged();
        activity.getForegroundService().setMainActivity(activity);
        activity.getForegroundService().startForeground();
    }

    public void playPrev()
    {
        activity.setWaitEnd(false);
        if(MainActivity.hStream == 0) return;
        nPlaying--;
        if(nPlaying < 0) return;
        playSong(nPlaying, true);
    }

    public void playNext(boolean bPlay) {
        activity.setWaitEnd(false);
        int nTempPlaying = nPlaying;
        ArrayList<SongItem> arSongs = arPlaylists.get(nPlayingPlaylist);

        AnimationButton btnShuffle = activity.findViewById(R.id.btnShuffle);
        boolean bShuffle = false;
        boolean bSingle = false;
        if(btnShuffle.getContentDescription().toString().equals(getString(R.string.shuffleOn)))
            bShuffle = true;
        else if(btnShuffle.getContentDescription().toString().equals(getString(R.string.singleOn)))
            bSingle = true;

        AnimationButton btnRepeat = activity.findViewById(R.id.btnRepeat);
        boolean bRepeatAll = false;
        boolean bRepeatSingle = false;
        if(btnRepeat.getContentDescription().toString().equals(getString(R.string.repeatAllOn)))
            bRepeatAll = true;
        else if(btnRepeat.getContentDescription().toString().equals(getString(R.string.repeatSingleOn)))
            bRepeatSingle = true;

        if(bSingle) // １曲のみ
        {
            if(!bRepeatSingle) nTempPlaying++;
            if (nTempPlaying >= arSongs.size())
            {
                if(!bRepeatAll)
                {
                    stop();
                    return;
                }
                nTempPlaying = 0;
            }
        }
        else if(bShuffle) // シャッフル
        {
            ArrayList<Integer> arTemp = new ArrayList<>();
            for (int i = 0; i < arPlayed.size(); i++) {
                if (i == nTempPlaying) continue;
                boolean bPlayed = arPlayed.get(i);
                if (!bPlayed) {
                    arTemp.add(i);
                }
            }
            if (arTemp.size() == 0)
            {
                if(!bRepeatAll)
                {
                    stop();
                    return;
                }
                for (int i = 0; i < arPlayed.size(); i++)
                {
                    arPlayed.set(i, false);
                }
            }
            if (arPlayed.size() > 1)
            {
                Random random = new Random();
                if (arTemp.size() == 0 || arTemp.size() == arPlayed.size())
                {
                    nTempPlaying = random.nextInt(arPlayed.size());
                }
                else {
                    int nRandom = random.nextInt(arTemp.size());
                    nTempPlaying = arTemp.get(nRandom);
                }
            }
        }
        else
        {
            nTempPlaying++;
            if (nTempPlaying >= arSongs.size())
            {
                if(!bRepeatAll)
                {
                    stop();
                    return;
                }
                nTempPlaying = 0;
            }
        }
        ArrayList<EffectSaver> arEffectSavers = arEffects.get(nPlayingPlaylist);
        EffectSaver saver = arEffectSavers.get(nTempPlaying);
        if(saver.isSave()) {
            ArrayList<EffectItem> arSavedEffectItems = saver.getEffectItems();
            for(int i = 0; i < arSavedEffectItems.size(); i++) {
                EffectItem item = arSavedEffectItems.get(i);
                if(item.getEffectName().equals(activity.effectFragment.getEffectItems().get(EffectFragment.kEffectTypeReverse).getEffectName())) {
                    if(bForceNormal) item.setSelected(false);
                    else if(bForceReverse) item.setSelected(true);
                }
            }
        }
        bForceNormal = bForceReverse = false;
        playSong(nTempPlaying, bPlay);
        if(!bPlay) pause();
    }

    public void onPlaylistItemClick(int nPlaylist)
    {
        selectPlaylist(nPlaylist);
        RelativeLayout relativeSongs = activity.findViewById(R.id.relativeSongs);
        relativeSongs.setVisibility(View.VISIBLE);
        RelativeLayout relativePlaylists = activity.findViewById(R.id.relativePlaylists);
        relativePlaylists.setVisibility(View.INVISIBLE);
        activity.findViewById(R.id.viewSep1).setVisibility(View.INVISIBLE);
    }

    public void onSongItemClick(int nSong)
    {
        ArrayList<SongItem> arSongs = arPlaylists.get(nSelectedPlaylist);
        if(nPlayingPlaylist == nSelectedPlaylist && nPlaying == nSong)
        {
            if(BASS.BASS_ChannelIsActive(MainActivity.hStream) == BASS.BASS_ACTIVE_PLAYING)
                pause();
            else play();
            return;
        }
        if(nPlayingPlaylist != nSelectedPlaylist) {
            arPlayed = new ArrayList<>();
            for(int i = 0; i < arSongs.size(); i++)
                arPlayed.add(false);
        }
        nPlayingPlaylist = nSelectedPlaylist;
        playSong(nSong, true);
    }

    private void playSong(int nSong, boolean bPlay)
    {
        activity.setWaitEnd(false);
        activity.clearLoop(false);

        boolean bReloadLyrics = false;
        RelativeLayout relativeLyrics = activity.findViewById(R.id.relativeLyrics);
        TextView textLyrics = activity.findViewById(R.id.textLyrics);
        if(relativeLyrics.getVisibility() == View.VISIBLE && textLyrics.getVisibility() == View.VISIBLE && nPlayingPlaylist == nSelectedPlaylist && nPlaying == nSelectedItem) {
            bReloadLyrics = true;
            nSelectedItem = nSong;
        }

        if(nPlayingPlaylist < 0) nPlayingPlaylist = 0;
        else if(nPlayingPlaylist >= arEffects.size()) nPlayingPlaylist = arEffects.size() - 1;
        ArrayList<EffectSaver> arEffectSavers = arEffects.get(nPlayingPlaylist);
        if(0 <= nPlaying && nPlaying < arEffectSavers.size() && 0 <= nSong && nSong < arEffectSavers.size()) {
            EffectSaver saverBefore = arEffectSavers.get(nPlaying);
            EffectSaver saverAfter = arEffectSavers.get(nSong);
            if(saverBefore.isSave() && !saverAfter.isSave()) {
                activity.controlFragment.setSpeed(0.0f, false);
                activity.controlFragment.setPitch(0.0f, false);
                activity.equalizerFragment.setVol(0, false);
                for (int i = 1; i <= 31; i++) {
                    activity.equalizerFragment.setEQ(i, 0, false);
                }
                ArrayList<EqualizerItem> arEqualizerItems = activity.equalizerFragment.getArEqualizerItems();
                for(int i = 0; i < arEqualizerItems.size(); i++) {
                    EqualizerItem item = arEqualizerItems.get(i);
                    item.setSelected(false);
                }
                activity.equalizerFragment.getEqualizersAdapter().notifyDataSetChanged();
                nPlaying = nSong;
                activity.effectFragment.resetEffect();
            }
        }
        nPlaying = nSong;
        if(arPlaylists.size() == 0 || nPlayingPlaylist >= arPlaylists.size() || arPlaylists.get(nPlayingPlaylist).size() == 0 || nSong >= arPlaylists.get(nPlayingPlaylist).size())
            return;
        if(nSong < 0) nSong = 0;
        else if(nSong >= arPlaylists.get(nPlayingPlaylist).size()) nSong = arPlaylists.get(nPlayingPlaylist).size() - 1;
        SongItem item = arPlaylists.get(nPlayingPlaylist).get(nSong);
        final String strPath = item.getPath();
        if(MainActivity.hStream != 0)
        {
            BASS.BASS_StreamFree(MainActivity.hStream);
            MainActivity.hStream = 0;
        }
        arPlayed.set(nSong, true);

        BASS.BASS_FILEPROCS fileprocs=new BASS.BASS_FILEPROCS() {
            @Override
            public boolean FILESEEKPROC(long offset, Object user) {
                FileChannel fc=(FileChannel)user;
                try {
                    fc.position(offset);
                    return true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return false;
            }

            @Override
            public int FILEREADPROC(ByteBuffer buffer, int length, Object user) {
                FileChannel fc=(FileChannel)user;
                try {
                    return fc.read(buffer);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return 0;
            }

            @Override
            public long FILELENPROC(Object user) {
                FileChannel fc=(FileChannel)user;
                try {
                    return fc.size();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return 0;
            }

            @Override
            public void FILECLOSEPROC(Object user) {
                FileChannel fc=(FileChannel)user;
                try {
                    fc.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        Uri uri = Uri.parse(strPath);
        if(uri.getScheme() != null && uri.getScheme().equals("content")) {
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            try {
                mmr.setDataSource(activity.getApplicationContext(), Uri.parse(strPath));
            }
            catch(Exception e) {
                e.printStackTrace();
            }
            ContentResolver cr = activity.getApplicationContext().getContentResolver();

            try {
                AssetFileDescriptor afd = cr.openAssetFileDescriptor(Uri.parse(strPath), "r");
                if(afd == null) return;
                FileChannel fc = afd.createInputStream().getChannel();
                MainActivity.hStream = BASS.BASS_StreamCreateFileUser(BASS.STREAMFILE_NOBUFFER, BASS.BASS_STREAM_DECODE, fileprocs, fc);
            } catch (Exception e) {
                removeSong(nPlayingPlaylist, nPlaying);
                if(nPlaying >= arPlaylists.get(nPlayingPlaylist).size())
                    nPlaying = 0;
                if(arPlaylists.get(nPlayingPlaylist).size() != 0)
                    playSong(nPlaying, true);
                return;
            }
        }
        else {
            MainActivity.hStream = BASS.BASS_StreamCreateFile(strPath, 0, 0, BASS.BASS_STREAM_DECODE);
        }
        if(MainActivity.hStream == 0) return;

        final RelativeLayout relativePlayingWithShadow = activity.findViewById(R.id.relativePlayingWithShadow);
        AnimationButton btnArtworkInPlayingBar = activity.findViewById(R.id.btnArtworkInPlayingBar);
        Bitmap bitmap = null;
        if(item.getPathArtwork() != null && !item.getPathArtwork().equals("")) {
            bitmap = BitmapFactory.decodeFile(item.getPathArtwork());
        }
        else {
            MediaMetadataRetriever mmr = new MediaMetadataRetriever();
            boolean bError = false;
            try {
                mmr.setDataSource(activity.getApplicationContext(), Uri.parse(item.getPath()));
            } catch (Exception e) {
                bError = true;
            }
            if (!bError) {
                byte[] data = mmr.getEmbeddedPicture();
                if (data != null) {
                    bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                }
            }
        }
        if(bitmap != null) btnArtworkInPlayingBar.setImageBitmap(bitmap);
        else btnArtworkInPlayingBar.setImageResource(R.drawable.ic_playing_large_artwork);
        TextView textTitleInPlayingBar = activity.findViewById(R.id.textTitleInPlayingBar);
        textTitleInPlayingBar.setText(item.getTitle());
        TextView textArtistInPlayingBar = activity.findViewById(R.id.textArtistInPlayingBar);
        if(item.getArtist() == null || item.getArtist().equals(""))
        {
            textArtistInPlayingBar.setTextColor(Color.argb(255, 147, 156, 160));
            textArtistInPlayingBar.setText(R.string.unknownArtist);
        }
        else
        {
            textArtistInPlayingBar.setTextColor(Color.argb(255, 102, 102, 102));
            textArtistInPlayingBar.setText(item.getArtist());
        }

        if(relativePlayingWithShadow.getVisibility() != View.VISIBLE)
        {
            final RelativeLayout.LayoutParams paramContainer = (RelativeLayout.LayoutParams)activity.findViewById(R.id.container).getLayoutParams();
            final RelativeLayout.LayoutParams paramRecording = (RelativeLayout.LayoutParams)activity.findViewById(R.id.relativeRecording).getLayoutParams();
            if(hRecord == 0) {
                paramContainer.bottomMargin = (int) (-22 * getResources().getDisplayMetrics().density + 0.5);
                paramRecording.bottomMargin = 0;
            }
            else {
                paramContainer.bottomMargin = 0;
                paramRecording.bottomMargin = (int) (-22 * getResources().getDisplayMetrics().density + 0.5);
            }
            relativePlayingWithShadow.setTranslationY((int) (82 * getResources().getDisplayMetrics().density + 0.5));
            relativePlayingWithShadow.setVisibility(View.VISIBLE);
            relativePlayingWithShadow.animate()
                    .translationY(0)
                    .setDuration(200)
                    .setListener(new AnimatorListenerAdapter() {
                                     @Override
                                     public void onAnimationEnd(Animator animation) {
                                         super.onAnimationEnd(animation);
                                         activity.loopFragment.drawWaveForm(strPath);
                                     }
                                 });
        }
        else activity.loopFragment.drawWaveForm(strPath);

        MainActivity.hStream = BASS_FX.BASS_FX_ReverseCreate(MainActivity.hStream, 2, BASS.BASS_STREAM_DECODE | BASS_FX.BASS_FX_FREESOURCE);
        MainActivity.hStream = BASS_FX.BASS_FX_TempoCreate(MainActivity.hStream, BASS_FX.BASS_FX_FREESOURCE);
        int chan = BASS_FX.BASS_FX_TempoGetSource(MainActivity.hStream);
        if(activity.effectFragment.isReverse())
            BASS.BASS_ChannelSetAttribute(chan, BASS_FX.BASS_ATTRIB_REVERSE_DIR, BASS_FX.BASS_FX_RVS_REVERSE);
        else
            BASS.BASS_ChannelSetAttribute(chan, BASS_FX.BASS_ATTRIB_REVERSE_DIR, BASS_FX.BASS_FX_RVS_FORWARD);
        MainActivity.hFxVol = BASS.BASS_ChannelSetFX(MainActivity.hStream, BASS_FX.BASS_FX_BFX_VOLUME, 0);
        int hFx20K = BASS.BASS_ChannelSetFX(MainActivity.hStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        int hFx16K = BASS.BASS_ChannelSetFX(MainActivity.hStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        int hFx12_5K = BASS.BASS_ChannelSetFX(MainActivity.hStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        int hFx10K = BASS.BASS_ChannelSetFX(MainActivity.hStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        int hFx8K = BASS.BASS_ChannelSetFX(MainActivity.hStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        int hFx6_3K = BASS.BASS_ChannelSetFX(MainActivity.hStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        int hFx5K = BASS.BASS_ChannelSetFX(MainActivity.hStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        int hFx4K = BASS.BASS_ChannelSetFX(MainActivity.hStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        int hFx3_15K = BASS.BASS_ChannelSetFX(MainActivity.hStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        int hFx2_5K = BASS.BASS_ChannelSetFX(MainActivity.hStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        int hFx2K = BASS.BASS_ChannelSetFX(MainActivity.hStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        int hFx1_6K = BASS.BASS_ChannelSetFX(MainActivity.hStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        int hFx1_25K = BASS.BASS_ChannelSetFX(MainActivity.hStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        int hFx1K = BASS.BASS_ChannelSetFX(MainActivity.hStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        int hFx800 = BASS.BASS_ChannelSetFX(MainActivity.hStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        int hFx630 = BASS.BASS_ChannelSetFX(MainActivity.hStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        int hFx500 = BASS.BASS_ChannelSetFX(MainActivity.hStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        int hFx400 = BASS.BASS_ChannelSetFX(MainActivity.hStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        int hFx315 = BASS.BASS_ChannelSetFX(MainActivity.hStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        int hFx250 = BASS.BASS_ChannelSetFX(MainActivity.hStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        int hFx200 = BASS.BASS_ChannelSetFX(MainActivity.hStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        int hFx160 = BASS.BASS_ChannelSetFX(MainActivity.hStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        int hFx125 = BASS.BASS_ChannelSetFX(MainActivity.hStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        int hFx100 = BASS.BASS_ChannelSetFX(MainActivity.hStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        int hFx80 = BASS.BASS_ChannelSetFX(MainActivity.hStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        int hFx63 = BASS.BASS_ChannelSetFX(MainActivity.hStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        int hFx50 = BASS.BASS_ChannelSetFX(MainActivity.hStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        int hFx40 = BASS.BASS_ChannelSetFX(MainActivity.hStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        int hFx31_5 = BASS.BASS_ChannelSetFX(MainActivity.hStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        int hFx25 = BASS.BASS_ChannelSetFX(MainActivity.hStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        int hFx20 = BASS.BASS_ChannelSetFX(MainActivity.hStream, BASS_FX.BASS_FX_BFX_PEAKEQ, 1);
        activity.equalizerFragment.setArHFX(new int[]{hFx20K, hFx16K, hFx12_5K, hFx10K, hFx8K, hFx6_3K, hFx5K, hFx4K, hFx3_15K, hFx2_5K, hFx2K, hFx1_6K, hFx1_25K, hFx1K, hFx800, hFx630, hFx500, hFx400, hFx315, hFx250, hFx200, hFx160, hFx125, hFx100, hFx80, hFx63, hFx50, hFx40, hFx31_5, hFx25, hFx20});
        if(nPlaying < 0) nPlaying = 0;
        else if(nPlaying >= arEffectSavers.size()) nPlaying = arEffectSavers.size() - 1;
        EffectSaver saver = arEffectSavers.get(nPlaying);
        if(saver.isSave()) restoreEffect();
        BASS.BASS_ChannelSetAttribute(MainActivity.hStream, BASS_FX.BASS_ATTRIB_TEMPO, activity.controlFragment.fSpeed);
        BASS.BASS_ChannelSetAttribute(MainActivity.hStream, BASS_FX.BASS_ATTRIB_TEMPO_PITCH, activity.controlFragment.fPitch);
        activity.equalizerFragment.setEQ();
        activity.effectFragment.applyEffect();
        activity.setSync();
        if(bPlay)
            BASS.BASS_ChannelPlay(MainActivity.hStream, false);
        AnimationButton btnPlay = activity.findViewById(R.id.btnPlay);
        btnPlay.setContentDescription(getString(R.string.pause));
        btnPlay.setImageResource(R.drawable.ic_bar_button_pause);
        AnimationButton btnPlayInPlayingBar = activity.findViewById(R.id.btnPlayInPlayingBar);
        btnPlayInPlayingBar.setContentDescription(getString(R.string.pause));
        if(activity.findViewById(R.id.seekCurPos).getVisibility() == View.VISIBLE)
            btnPlayInPlayingBar.setImageResource(R.drawable.ic_playing_large_pause);
        else btnPlayInPlayingBar.setImageResource(R.drawable.ic_bar_button_pause);
        songsAdapter.notifyDataSetChanged();
        playlistsAdapter.notifyDataSetChanged();
        tabAdapter.notifyDataSetChanged();
        if(bReloadLyrics) showLyrics();

        activity.getForegroundService().setMainActivity(activity);
        activity.getForegroundService().startForeground();
    }

    private String getLyrics(int nPlaylist, int nSong) {
        ArrayList<SongItem> arSongs = arPlaylists.get(nPlaylist);
        final SongItem songItem = arSongs.get(nSong);

        try {
            String strPath = getFilePath(activity, Uri.parse(songItem.getPath()));
            if(strPath != null) {
                File file = new File(strPath);
                Mp3File mp3file = new Mp3File(file);
                ID3v2 id3v2Tag;
                if (mp3file.hasId3v2Tag()) {
                    id3v2Tag = mp3file.getId3v2Tag();
                    return id3v2Tag.getLyrics();
                }
            }
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @SuppressLint("NewApi")
    private static String getFilePath(Context context, Uri uri) {
        String selection = null;
        String[] selectionArgs = null;
        // Uri is different in versions after KITKAT (Android 4.4), we need to
        if (Build.VERSION.SDK_INT >= 19 && DocumentsContract.isDocumentUri(context.getApplicationContext(), uri)) {
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                return Environment.getExternalStorageDirectory() + "/" + split[1];
            } else if (isDownloadsDocument(uri)) {
                final String id = DocumentsContract.getDocumentId(uri);
                uri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
            } else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                if ("image".equals(type)) {
                    uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                selection = "_id=?";
                selectionArgs = new String[]{
                        split[1]
                };
            }
        }
        if ("content".equalsIgnoreCase(uri.getScheme())) {
            String[] projection = {
                    MediaStore.Images.Media.DATA
            };
            Cursor cursor;
            try {
                cursor = context.getContentResolver()
                        .query(uri, projection, selection, selectionArgs, null);
                if(cursor != null) {
                    int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                    if (cursor.moveToFirst()) {
                        String strPath = cursor.getString(column_index);
                        cursor.close();
                        return strPath;
                    }
                    cursor.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    public void stop()
    {
        activity.setWaitEnd(false);

        if(MainActivity.hStream == 0) return;

        final RelativeLayout relativePlayingWithShadow = activity.findViewById(R.id.relativePlayingWithShadow);

        SeekBar seekCurPos = activity.findViewById(R.id.seekCurPos);
        if(seekCurPos.getVisibility() == View.VISIBLE)
            activity.downViewPlaying(true);
        else {
            relativePlayingWithShadow.setVisibility(View.VISIBLE);
            relativePlayingWithShadow.animate()
                    .translationY((int) (82 * getResources().getDisplayMetrics().density + 0.5))
                    .setDuration(200)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            relativePlayingWithShadow.setVisibility(View.GONE);
                            final RelativeLayout.LayoutParams paramContainer = (RelativeLayout.LayoutParams) activity.findViewById(R.id.container).getLayoutParams();
                            final RelativeLayout.LayoutParams paramRecording = (RelativeLayout.LayoutParams) activity.findViewById(R.id.relativeRecording).getLayoutParams();
                            if (hRecord == 0) {
                                paramContainer.bottomMargin = 0;
                                paramRecording.bottomMargin = 0;
                            } else {
                                paramContainer.bottomMargin = 0;
                                paramRecording.bottomMargin = 0;
                            }
                        }
                    });
        }

        nPlaying = -1;
        BASS.BASS_ChannelStop(MainActivity.hStream);
        MainActivity.hStream = 0;
        AnimationButton btnPlay = activity.findViewById(R.id.btnPlay);
        btnPlay.setContentDescription(getString(R.string.play));
        btnPlay.setImageResource(R.drawable.ic_bar_button_play);
        AnimationButton btnPlayInPlayingBar = activity.findViewById(R.id.btnPlayInPlayingBar);
        btnPlayInPlayingBar.setContentDescription(getString(R.string.play));
        if(activity.findViewById(R.id.seekCurPos).getVisibility() == View.VISIBLE)
            btnPlayInPlayingBar.setImageResource(R.drawable.ic_playing_large_play);
        else btnPlayInPlayingBar.setImageResource(R.drawable.ic_bar_button_play);
        activity.clearLoop();
        songsAdapter.notifyDataSetChanged();
        playlistsAdapter.notifyDataSetChanged();
        tabAdapter.notifyDataSetChanged();

        activity.getForegroundService().stopForeground();
    }

    public void addSong(MainActivity activity, Uri uri)
    {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        boolean bError = false;
        try {
            mmr.setDataSource(activity.getApplicationContext(), uri);
        }
        catch(Exception e) {
            bError = true;
        }
        if(nSelectedPlaylist < 0) nSelectedPlaylist = 0;
        else if(nSelectedPlaylist >= arPlaylists.size()) nSelectedPlaylist = arPlaylists.size() - 1;
        ArrayList<SongItem> arSongs = arPlaylists.get(nSelectedPlaylist);
        String strTitle = null;
        String strArtist = null;
        if(!bError) {
            strTitle = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
            strArtist = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
        }
        if(strTitle != null) {
            SongItem item = new SongItem(String.format(Locale.getDefault(), "%d", arSongs.size()+1), strTitle, strArtist, uri.toString());
            arSongs.add(item);
        }
        else
        {
            strTitle = getFileNameFromUri(activity.getApplicationContext(), uri);
            if(strTitle == null) {
                int startIndex = uri.toString().lastIndexOf('/');
                strTitle = uri.toString().substring(startIndex + 1);
            }
            SongItem item = new SongItem(String.format(Locale.getDefault(), "%d", arSongs.size()+1), strTitle, "", uri.toString());
            arSongs.add(item);
        }
        ArrayList<EffectSaver> arEffectSavers = arEffects.get(nSelectedPlaylist);
        EffectSaver saver = new EffectSaver();
        arEffectSavers.add(saver);

        ArrayList<String> arTempLyrics = arLyrics.get(nSelectedPlaylist);
        arTempLyrics.add(null);

        if(nSelectedPlaylist == nPlayingPlaylist) arPlayed.add(false);
    }

    @SuppressWarnings("deprecation")
    private void addVideo(final MainActivity activity, Uri uri)
    {
        if(Build.VERSION.SDK_INT < 18) return;
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        try {
            mmr.setDataSource(activity.getApplicationContext(), uri);
        }
        catch(Exception e) {
            e.printStackTrace();
            return;
        }

        ContentResolver cr = activity.getApplicationContext().getContentResolver();

        AssetFileDescriptor afd = null;
        try {
            afd = cr.openAssetFileDescriptor(uri, "r");
        }
        catch(Exception e) {
            e.printStackTrace();
        }
        if(afd == null) return;
        MediaExtractor extractor = new MediaExtractor();
        try {
            extractor.setDataSource(afd.getFileDescriptor());
        }
        catch (Exception e) {
            return;
        }
        int trackCount = extractor.getTrackCount();
        String strPathTo;
        int n = 0;
        File fileForCheck;
        while (true) {
            strPathTo = activity.getFilesDir() + "/recorded" + String.format(Locale.getDefault(), "%d", n) + ".mp3";
            fileForCheck = new File(strPathTo);
            if (!fileForCheck.exists()) break;
            n++;
        }
        final File file = new File(strPathTo);
        MediaMuxer muxer;
        try {
            muxer = new MediaMuxer(strPathTo, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        }
        catch(Exception e) {
            return;
        }
        int audioTrackIndex = 0;
        for (int i = 0; i < trackCount; i++) {
            MediaFormat format = extractor.getTrackFormat(i);
            String COMPRESSED_AUDIO_FILE_MIME_TYPE = format.getString(MediaFormat.KEY_MIME);

            if(COMPRESSED_AUDIO_FILE_MIME_TYPE.startsWith("audio/")) {
                extractor.selectTrack(i);
                audioTrackIndex = muxer.addTrack(format);
            }
        }
        boolean sawEOS = false;
        int offset = 100;
        ByteBuffer dstBuf = ByteBuffer.allocate(256 * 1024);
        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        muxer.start();
        while (!sawEOS) {
            bufferInfo.offset = offset;
            bufferInfo.size = extractor.readSampleData(dstBuf, offset);
            if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                bufferInfo.size = 0;
            }
            else if (bufferInfo.size < 0) {
                sawEOS = true;
                bufferInfo.size = 0;
            }
            else {
                bufferInfo.presentationTimeUs = extractor.getSampleTime();
                if(Build.VERSION.SDK_INT >= 21)
                    bufferInfo.flags = MediaCodec.BUFFER_FLAG_KEY_FRAME;
                else
                    bufferInfo.flags = MediaCodec.BUFFER_FLAG_SYNC_FRAME;
                dstBuf.position(bufferInfo.offset);
                dstBuf.limit(bufferInfo.offset + bufferInfo.size);
                muxer.writeSampleData(audioTrackIndex, dstBuf, bufferInfo);
                extractor.advance();
            }
        }
        muxer.stop();
        muxer.release();
        try {
            afd.close();
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.addFromGallery);
        LinearLayout linearLayout = new LinearLayout(activity);
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        final EditText editTitle = new EditText (activity);
        editTitle.setHint(R.string.title);
        editTitle.setHintTextColor(Color.argb(255, 192, 192, 192));
        DateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault());
        Date date = new Date(System.currentTimeMillis());
        editTitle.setText(String.format(Locale.getDefault(), "ムービー(%s)", df.format(date)));
        final EditText editArtist = new EditText (activity);
        editArtist.setHint(R.string.artist);
        editArtist.setHintTextColor(Color.argb(255, 192, 192, 192));
        editArtist.setText("");
        linearLayout.addView(editTitle);
        linearLayout.addView(editArtist);
        builder.setView(linearLayout);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                ArrayList<SongItem> arSongs = arPlaylists.get(nSelectedPlaylist);
                SongItem item = new SongItem(String.format(Locale.getDefault(), "%d", arSongs.size()+1), editTitle.getText().toString(), editArtist.getText().toString(), file.getPath());
                arSongs.add(item);
                ArrayList<EffectSaver> arEffectSavers = arEffects.get(nSelectedPlaylist);
                EffectSaver saver = new EffectSaver();
                arEffectSavers.add(saver);
                ArrayList<String> arTempLyrics = arLyrics.get(nSelectedPlaylist);
                arTempLyrics.add(null);
                if(nSelectedPlaylist == nPlayingPlaylist) arPlayed.add(false);
                songsAdapter.notifyDataSetChanged();

                saveFiles(true, true, true, true, false);
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                if(!file.delete()) System.out.println("ファイルが削除できませんでした");
            }
        });
        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if(!file.delete()) System.out.println("ファイルが削除できませんでした");
            }
        });
        final AlertDialog alertDialog = builder.create();
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener()
        {
            @Override
            public void onShow(DialogInterface arg0)
            {
                editTitle.requestFocus();
                editTitle.setSelection(editTitle.getText().toString().length());
                InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
                if (null != imm) imm.showSoftInput(editTitle, 0);
            }
        });
        alertDialog.show();
    }

    private void removeSong(int nPlaylist, int nSong)
    {
        if(nSong < nPlaying) nPlaying--;

        ArrayList<SongItem> arSongs = arPlaylists.get(nPlaylist);
        SongItem song = arSongs.get(nSong);
        Uri uri = Uri.parse(song.getPath());
        if(!(uri.getScheme() != null && uri.getScheme().equals("content"))) {
            File file = new File(song.getPath());
            if(!file.delete()) System.out.println("ファイルが削除できませんでした");
        }

        arSongs.remove(nSong);
        if(nPlaylist == nPlayingPlaylist) arPlayed.remove(nSong);

        for(int i = nSong; i < arSongs.size(); i++) {
            SongItem songItem = arSongs.get(i);
            songItem.setNumber(String.format(Locale.getDefault(), "%d", i+1));
        }

        songsAdapter.notifyDataSetChanged();

        ArrayList<EffectSaver> arEffectSavers = arEffects.get(nPlaylist);
        arEffectSavers.remove(nSong);

        ArrayList<String> arTempLyrics = arLyrics.get(nPlaylist);
        arTempLyrics.remove(nSong);

        saveFiles(true, true, true, true, false);

    }

    private String getFileNameFromUri(Context context, Uri uri) {
        if (null == uri) return null;

        String scheme = uri.getScheme();

        String fileName = null;
        if(scheme == null) return null;
        switch (scheme) {
            case "content":
                String[] projection = {MediaStore.MediaColumns.DISPLAY_NAME};
                Cursor cursor;
                try {
                    cursor = context.getContentResolver()
                            .query(uri, projection, null, null, null);
                }
                catch(Exception e) {
                    return null;
                }
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        fileName = cursor.getString(
                                cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME));
                    }
                    cursor.close();
                }
                break;

            case "file":
                String strPath = uri.getPath();
                if(strPath != null) fileName = new File(strPath).getName();
                break;

            default:
                break;
        }
        return fileName;
    }

    public void selectPlaylist(int nSelect)
    {
        if(recyclerTab != null) recyclerTab.scrollToPosition(nSelect);
        nSelectedPlaylist = nSelect;
        if(tabAdapter != null) tabAdapter.notifyDataSetChanged();
        if(songsAdapter != null) songsAdapter.notifyDataSetChanged();
        if(playlistsAdapter != null) playlistsAdapter.notifyDataSetChanged();
    }

    public void updateSongTime(SongItem item)
    {
        MediaMetadataRetriever mmr = new MediaMetadataRetriever();
        mmr.setDataSource(activity.getApplicationContext(), Uri.parse(item.getPath()));
        long durationMs = Long.parseLong(mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
        long duration = durationMs / 1000;
        long lMinutes = duration / 60;
        long lSeconds = duration % 60;
        item.setTime(String.format(Locale.getDefault(), "%d:%02d", lMinutes, lSeconds));
        saveFiles(true, false, false, false, false);
    }

    public void updateSongs()
    {
        if(songsAdapter != null)
            songsAdapter.notifyDataSetChanged();
    }

    public void saveFiles(boolean bPlaylists, boolean bEffects, boolean bLyrics, boolean bPlaylistNames, boolean bPlayMode)
    {
        SharedPreferences preferences = activity.getSharedPreferences("SaveData", Activity.MODE_PRIVATE);
        Gson gson = new Gson();
        if(bPlaylists)
            preferences.edit().putString("arPlaylists", gson.toJson(arPlaylists)).apply();
        if(bEffects)
            preferences.edit().putString("arEffects", gson.toJson(arEffects)).apply();
        if(bLyrics)
            preferences.edit().putString("arLyrics", gson.toJson(arLyrics)).apply();
        if(bPlaylistNames)
            preferences.edit().putString("arPlaylistNames", gson.toJson(arPlaylistNames)).apply();
        if(bPlayMode)
        {
            AnimationButton btnShuffle = activity.findViewById(R.id.btnShuffle);
            int nShuffle = 0;
            if(btnShuffle.getContentDescription().toString().equals(getString(R.string.shuffleOn)))
                nShuffle = 1;
            else if(btnShuffle.getContentDescription().toString().equals(getString(R.string.singleOn)))
                nShuffle = 2;
            preferences.edit().putInt("shufflemode", nShuffle).apply();
            AnimationButton btnRepeat = activity.findViewById(R.id.btnRepeat);
            int nRepeat = 0;
            if(btnRepeat.getContentDescription().toString().equals(getString(R.string.repeatAllOn)))
                nRepeat = 1;
            else if(btnRepeat.getContentDescription().toString().equals(getString(R.string.repeatSingleOn)))
                nRepeat = 2;
            preferences.edit().putInt("repeatmode", nRepeat).apply();
        }
    }

    public void setPeak(float fPeak)
    {
        if(nPlayingPlaylist < 0 || nPlayingPlaylist >= arPlaylists.size()) return;
        ArrayList<SongItem> arSongs = arPlaylists.get(nPlayingPlaylist);
        if(nPlaying < 0 || nPlaying >= arSongs.size()) return;
        SongItem song = arSongs.get(nPlaying);
        if(song.getPeak() != fPeak) {
            song.setPeak(fPeak);
            saveFiles(true, false, false, false, false);
            activity.effectFragment.setPeak(fPeak);
        }
    }
}