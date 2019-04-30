/*
 * LoopFragment
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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.un4seen.bass.BASS;

import java.util.ArrayList;
import java.util.Locale;

public class LoopFragment extends Fragment implements View.OnTouchListener, View.OnFocusChangeListener, View.OnLongClickListener {
    private int mMarker; // マーカー再生時のループ位置
    private final Handler mHandler;
    private MainActivity mActivity;

    private WaveView mWaveView;
    private LinearLayout mABButton, mMarkerButton;
    private EditText mTextCurValue;
    private View mViewCurPos, mViewMaskA, mViewMaskB;
    private RadioGroup mRadioGroupLoopMode;
    private RelativeLayout mRelativeLoop, mRelativeWave;
    private AnimationButton mBtnRewind5Sec, mBtnRewind5Sec2, mBtnForward5Sec, mBtnForward5Sec2, mBtnLoopmarker, mBtnA, mBtnB;
    private EditText mTextAValue, mTextBValue;

    private ArrayList<Double> mMarkerTimes;
    private final ArrayList<TextView> mMarkerTexts;
    private boolean mContinue = false;
    private boolean mTouching = false;

    public LinearLayout getABButton() { return mABButton; }
    public AnimationButton getBtnLoopmarker() { return mBtnLoopmarker; }
    public RadioGroup getRadioGroupLoopMode() { return mRadioGroupLoopMode; }

    public ArrayList<Double>  getArMarkerTime() { return mMarkerTimes; }
    public void setArMarkerTime(ArrayList<Double> markerTimes) {
        if(markerTimes == null) return;
        mMarkerTimes = new ArrayList<>();
        int nScreenWidth = mWaveView.getWidth();
        int nMaxWidth = (int)(nScreenWidth * mWaveView.getZoom());
        double dLength = BASS.BASS_ChannelBytes2Seconds(MainActivity.sStream, BASS.BASS_ChannelGetLength(MainActivity.sStream, BASS.BASS_POS_BYTE));
        for(int i = 0; i < markerTimes.size(); i++) {
            double dPos = markerTimes.get(i);
            mMarkerTimes.add(dPos);

            TextView textView = new TextView(mActivity);
            textView.setText("▼");
            mRelativeLoop.addView(textView);
            textView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
            int nLeft = (int) (mViewCurPos.getX() - nMaxWidth * dPos / dLength - textView.getMeasuredWidth() / 2.0f);
            int nTop = mWaveView.getTop() - textView.getMeasuredHeight();
            textView.setTranslationX(nLeft);
            textView.setTranslationY(nTop);
            textView.requestLayout();
            mMarkerTexts.add(i, textView);
        }
    }
    public int getMarker() { return mMarker; }
    public void setMarker(int marker) { mMarker = marker; }
    public EditText getTextCurValue() { return mTextCurValue; }

    public LoopFragment()
    {
        mMarker = 0;
        mHandler = new Handler();
        mMarkerTexts = new ArrayList<>();
        mMarkerTimes = new ArrayList<>();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mActivity = (MainActivity)context;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mActivity = null;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState)
    {
        return inflater.inflate(R.layout.fragment_loop, container, false);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mViewCurPos = mActivity.findViewById(R.id.viewCurPos);
        mViewCurPos.setX(-(int) (1.0 * mActivity.getDensity()));
        mABButton = mActivity.findViewById(R.id.ABButton);
        mMarkerButton = mActivity.findViewById(R.id.MarkerButton);
        mTextCurValue = mActivity.findViewById(R.id.textCurValue);
        mViewMaskA = mActivity.findViewById(R.id.viewMaskA);
        mViewMaskB = mActivity.findViewById(R.id.viewMaskB);
        mWaveView = mActivity.findViewById(R.id.waveView);
        mBtnLoopmarker = mActivity.findViewById(R.id.btnLoopmarker);
        mRadioGroupLoopMode = mActivity.findViewById(R.id.radioGroupLoopMode);
        mRelativeLoop = mActivity.findViewById(R.id.relativeLoop);
        mBtnRewind5Sec = mActivity.findViewById(R.id.btnRewind5Sec);
        mBtnRewind5Sec2 = mActivity.findViewById(R.id.btnRewind5Sec2);
        mBtnForward5Sec = mActivity.findViewById(R.id.btnForward5Sec);
        mBtnForward5Sec2 = mActivity.findViewById(R.id.btnForward5Sec2);
        mTextAValue = mActivity.findViewById(R.id.textAValue);
        mTextBValue  = mActivity.findViewById(R.id.textBValue);
        mBtnA = mActivity.findViewById(R.id.btnA);
        mBtnB = mActivity.findViewById(R.id.btnB);
        mRelativeWave = mActivity.findViewById(R.id.relativeWave);
        AnimationButton btnDelmarker= mActivity.findViewById(R.id.btnDelmarker);
        AnimationButton btnPrevmarker = mActivity.findViewById(R.id.btnPrevmarker);
        RelativeLayout relativeZoomOut = mActivity.findViewById(R.id.relativeZoomOut);
        RelativeLayout relativeZoomIn = mActivity.findViewById(R.id.relativeZoomIn);
        AnimationButton btnAddmarker = mActivity.findViewById(R.id.btnAddmarker);
        AnimationButton btnNextmarker = mActivity.findViewById(R.id.btnNextmarker);
        final LinearLayout ABLabel = mActivity.findViewById(R.id.ABLabel);

        mTextAValue.setText(getString(R.string.zeroHMS));
        mTextBValue.setText(getString(R.string.zeroHMS));
        mTextCurValue.setText(getString(R.string.zeroHMS));

        mWaveView.setLoopFragment(this);
        mWaveView.setOnTouchListener(this);
        relativeZoomOut.setOnTouchListener(this);
        relativeZoomOut.setOnLongClickListener(this);
        relativeZoomIn.setOnTouchListener(this);
        relativeZoomIn.setOnLongClickListener(this);
        mActivity.findViewById(R.id.viewBtnALeft).setOnTouchListener(this);
        mBtnA.setSelected(false);
        mBtnA.setImageResource(R.drawable.ic_abloop_a);
        mBtnA.setOnTouchListener(this);
        mActivity.findViewById(R.id.viewBtnARight).setOnTouchListener(this);
        mActivity.findViewById(R.id.viewBtnRewindLeft).setOnTouchListener(this);
        mBtnRewind5Sec.setOnTouchListener(this);
        mBtnRewind5Sec.setOnLongClickListener(this);
        mBtnRewind5Sec.setTag(5);
        mActivity.findViewById(R.id.viewBtnRewindRight).setOnTouchListener(this);
        mActivity.findViewById(R.id.viewBtnForwardLeft).setOnTouchListener(this);
        mBtnForward5Sec.setOnTouchListener(this);
        mBtnForward5Sec.setOnLongClickListener(this);
        mBtnForward5Sec.setTag(5);
        mActivity.findViewById(R.id.viewBtnForwardRight).setOnTouchListener(this);
        mActivity.findViewById(R.id.viewBtnBLeft).setOnTouchListener(this);
        mBtnB.setSelected(false);
        mBtnB.setImageResource(R.drawable.ic_abloop_b);
        mBtnB.setOnTouchListener(this);
        mActivity.findViewById(R.id.viewBtnBRight).setOnTouchListener(this);

        mBtnRewind5Sec2.setOnTouchListener(this);
        mBtnRewind5Sec2.setOnLongClickListener(this);
        mBtnRewind5Sec2.setTag(5);
        btnPrevmarker.setOnTouchListener(this);
        btnDelmarker.setOnTouchListener(this);
        btnAddmarker.setOnTouchListener(this);
        btnNextmarker.setOnTouchListener(this);
        mBtnLoopmarker.setSelected(false);
        mBtnLoopmarker.setImageResource(R.drawable.ic_abloop_marker_loop);
        mBtnLoopmarker.setOnTouchListener(this);
        mBtnForward5Sec2.setOnTouchListener(this);
        mBtnForward5Sec2.setOnLongClickListener(this);
        mBtnForward5Sec2.setTag(5);

        mRadioGroupLoopMode.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int nItem) {
                if(nItem == R.id.radioButtonABLoop) {
                    ABLabel.setVisibility(View.VISIBLE);
                    mABButton.setVisibility(View.VISIBLE);
                    if(mActivity.isLoopA()) mViewMaskA.setVisibility(View.VISIBLE);
                    if(mActivity.isLoopB()) mViewMaskB.setVisibility(View.VISIBLE);
                    mActivity.setSync();
                    mActivity.playlistFragment.updateSavingEffect();

                    mMarkerButton.setVisibility(View.INVISIBLE);
                    for(int i = 0 ; i < mMarkerTexts.size(); i++)
                    {
                        TextView textView = mMarkerTexts.get(i);
                        textView.setVisibility(View.INVISIBLE);
                    }
                }
                else {
                    mMarkerButton.setVisibility(View.VISIBLE);
                    for(int i = 0 ; i < mMarkerTexts.size(); i++)
                    {
                        TextView textView = mMarkerTexts.get(i);
                        textView.setVisibility(View.VISIBLE);
                    }
                    mActivity.setSync();
                    mActivity.playlistFragment.updateSavingEffect();

                    ABLabel.setVisibility(View.INVISIBLE);
                    mABButton.setVisibility(View.INVISIBLE);
                    mViewMaskA.setVisibility(View.INVISIBLE);
                    mViewMaskB.setVisibility(View.INVISIBLE);
                }
            }
        });

        mTextAValue.setOnFocusChangeListener(this);
        mTextBValue.setOnFocusChangeListener(this);
        mTextCurValue.setOnFocusChangeListener(this);
    }

    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            long lDelay = 250;
            updateCurPos();
            mHandler.postDelayed(this, lDelay);
        }
    };

    void updateCurPos()
    {
        if(mActivity == null) return;

        long lDelay = 0;
        long nPos = 0;
        int nScreenWidth = mWaveView.getWidth();
        int nMaxWidth = (int) (nScreenWidth * mWaveView.getZoom());
        int nLeft = -(int) (1.0 * mActivity.getDensity());
        if (MainActivity.sStream != 0) {
            if(BASS.BASS_ChannelIsActive(MainActivity.sStream) == BASS.BASS_ACTIVE_PLAYING && !mTouching)
                lDelay = 250;
            double dPos = BASS.BASS_ChannelBytes2Seconds(MainActivity.sStream, BASS.BASS_ChannelGetPosition(MainActivity.sStream, BASS.BASS_POS_BYTE));
            if (mABButton.getVisibility() == View.VISIBLE && ((mActivity.isLoopA() && dPos < mActivity.getLoopAPos()) || (mActivity.isLoopB() && mActivity.getLoopBPos() < dPos)) && !mActivity.isPlayNextByBPos()) {
                if (mActivity.effectFragment.isReverse())
                    dPos = BASS.BASS_ChannelBytes2Seconds(MainActivity.sStream, BASS.BASS_ChannelSeconds2Bytes(MainActivity.sStream, mActivity.getLoopBPos()));
                else
                    dPos = BASS.BASS_ChannelBytes2Seconds(MainActivity.sStream, BASS.BASS_ChannelSeconds2Bytes(MainActivity.sStream, mActivity.getLoopAPos()));
                BASS.BASS_ChannelSetPosition(MainActivity.sStream, BASS.BASS_ChannelSeconds2Bytes(MainActivity.sStream, dPos), BASS.BASS_POS_BYTE);
            }
            if (dPos < 0) dPos = 0;
            int nMinute = (int) (dPos / 60);
            int nSecond = (int) (dPos % 60);
            int nHour = nMinute / 60;

            if (mActivity.getSeekCurPos().getVisibility() == View.VISIBLE) {
                double dRemain = mActivity.getLength() - dPos;
                int nRemainMinute = (int) (dRemain / 60);
                int nRemainSecond = (int) (dRemain % 60);

                String strCurPos = nMinute + (nSecond < 10 ? ":0" : ":") + nSecond;
                mActivity.getTextCurPos().setText(strCurPos);
                String strRemain = "-" + nRemainMinute + (nRemainSecond < 10 ? ":0" : ":") + nRemainSecond;
                mActivity.getTextRemain().setText(strRemain);
                mActivity.getSeekCurPos().setProgress((int) dPos);
            }

            nMinute = nMinute % 60;
            int nDec = (int) ((dPos * 100) % 100);

            if (mActivity.getViewPager().getCurrentItem() == 1) {
                String strCurValue = String.format(Locale.getDefault(), "%02d:%02d:%02d.%02d", nHour, nMinute, nSecond, nDec);
                if (!mTextCurValue.getText().toString().equals(strCurValue))
                    mTextCurValue.setText(strCurValue);
            }
            nPos = BASS.BASS_ChannelGetPosition(MainActivity.sStream, BASS.BASS_POS_BYTE);
            nLeft = (int) (nMaxWidth * nPos / mActivity.getByteLength());
        }
        if (mActivity.getViewPager().getCurrentItem() == 1) {
            int nTop = mViewCurPos.getTop();
            if (nScreenWidth / 2 <= nLeft && nLeft < nMaxWidth - nScreenWidth / 2)
                nLeft = (int) (nScreenWidth / 2.0f);
            else if (nMaxWidth - nScreenWidth / 2 <= nLeft)
                nLeft = nScreenWidth - (nMaxWidth - nLeft);
            mViewCurPos.animate()
                    .x(nLeft)
                    .y(nTop)
                    .setDuration(lDelay)
                    .setInterpolator(new LinearInterpolator())
                    .start();
            if (!mContinue) mWaveView.invalidate();
            if (mActivity.isLoopA()) {
                long nPosA = 0;
                if (MainActivity.sStream != 0)
                    nPosA = BASS.BASS_ChannelSeconds2Bytes(MainActivity.sStream, mActivity.getLoopAPos());
                nPosA = nPos - nPosA;
                int nLeftA = (int) (nLeft - nMaxWidth * nPosA / mActivity.getByteLength());
                if (nLeftA < 0) nLeftA = 0;
                mViewMaskA.getLayoutParams().width = nLeftA;
                mViewMaskA.requestLayout();
            }
            if (mActivity.isLoopB()) {
                long nPosB = 0;
                if (MainActivity.sStream != 0)
                    nPosB = BASS.BASS_ChannelSeconds2Bytes(MainActivity.sStream, mActivity.getLoopBPos());
                nPosB = nPos - nPosB;
                int nLeftB = (int) (nLeft - nMaxWidth * nPosB / mActivity.getByteLength());
                if (nLeftB < 0) nLeftB = 0;
                else if (nLeftB > mWaveView.getWidth()) nLeftB = mWaveView.getWidth();
                mViewMaskB.setTranslationX(nLeftB);
                mViewMaskB.getLayoutParams().width = mWaveView.getWidth() - nLeftB;
                mViewMaskB.requestLayout();
            }
            if (mMarkerButton.getVisibility() == View.VISIBLE) {
                for (int i = 0; i < mMarkerTimes.size(); i++) {
                    TextView textView = mMarkerTexts.get(i);
                    double dMarkerPos = mMarkerTimes.get(i);
                    long mMarkerPos = BASS.BASS_ChannelSeconds2Bytes(MainActivity.sStream, dMarkerPos);
                    mMarkerPos = nPos - mMarkerPos;
                    int mMarkerLeft = (int) (nLeft - nMaxWidth * mMarkerPos / (float) mActivity.getByteLength() - textView.getWidth() / 2.0f);
                    if (mMarkerLeft < -textView.getWidth())
                        mMarkerLeft = -textView.getWidth();
                    textView.setTranslationX(mMarkerLeft);
                    textView.requestLayout();
                }
            }
        }
    }

    public void startTimer()
    {
        stopTimer();
        mHandler.post(mRunnable);
    }

    void stopTimer()
    {
        mHandler.removeCallbacks(mRunnable);
        updateCurPos();
    }

    private void setZoomOut() {
        mWaveView.setZoom(mWaveView.getZoom() * 0.99f);
    }

    private void setZoomIn() {
        mWaveView.setZoom(mWaveView.getZoom() * 1.01f);
    }

    private final Runnable repeatZoomOut = new Runnable()
    {
        @Override
        public void run()
        {
            if(!mContinue)
                return;
            setZoomOut();
            long nLength = BASS.BASS_ChannelGetLength(MainActivity.sStream, BASS.BASS_POS_BYTE);
            long nPos = BASS.BASS_ChannelGetPosition(MainActivity.sStream, BASS.BASS_POS_BYTE);
            int nScreenWidth = mWaveView.getWidth();
            int nMaxWidth = (int)(nScreenWidth * mWaveView.getZoom());
            int nLeft = (int) (nMaxWidth * nPos / nLength);
            if(nLeft < nScreenWidth / 2)
                mWaveView.setPivotX(0.0f);
            else if(nScreenWidth / 2 <= nLeft && nLeft < nMaxWidth - nScreenWidth / 2)
                mWaveView.setPivotX(0.5f);
            else
                mWaveView.setPivotX(1.0f);
            mWaveView.setScaleX(mWaveView.getZoom());
            mHandler.postDelayed(this, 10);
        }
    };

    private final Runnable repeatZoomIn = new Runnable()
    {
        @Override
        public void run()
        {
            if(!mContinue)
                return;
            setZoomIn();
            long nLength = BASS.BASS_ChannelGetLength(MainActivity.sStream, BASS.BASS_POS_BYTE);
            long nPos = BASS.BASS_ChannelGetPosition(MainActivity.sStream, BASS.BASS_POS_BYTE);
            int nScreenWidth = mWaveView.getWidth();
            int nMaxWidth = (int)(nScreenWidth * mWaveView.getZoom());
            int nLeft = (int) (nMaxWidth * nPos / nLength);
            if(nLeft < nScreenWidth / 2)
                mWaveView.setPivotX(0.0f);
            else if(nScreenWidth / 2 <= nLeft && nLeft < nMaxWidth - nScreenWidth / 2)
                mWaveView.setPivotX(0.5f);
            else
                mWaveView.setPivotX(1.0f);
            mWaveView.setScaleX(mWaveView.getZoom());
            mHandler.postDelayed(this, 10);
        }
    };

    @Override
    public boolean onLongClick(View v) {
        if (v.getId() == R.id.relativeZoomOut) {
            if(MainActivity.sStream == 0) return false;
            mContinue = true;
            mHandler.post(repeatZoomOut);
            return true;
        }
        else if (v.getId() == R.id.relativeZoomIn) {
            if(MainActivity.sStream == 0) return false;
            mContinue = true;
            mHandler.post(repeatZoomIn);
            return true;
        }
        else if(v.getId() == R.id.btnRewind5Sec || v.getId() == R.id.btnRewind5Sec2) {
            final BottomMenu menu = new BottomMenu(mActivity);
            menu.setTitle(getString(R.string.chooseRewindButton));
            menu.addMenu(getString(R.string.rewind1Sec), R.drawable.ic_actionsheet_01sec_prev, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mBtnRewind5Sec.setImageResource(R.drawable.ic_abloop_01sec_prev);
                    mBtnRewind5Sec2.setImageResource(R.drawable.ic_abloop_01sec_prev);
                    mBtnRewind5Sec.setContentDescription(getString(R.string.rewind1Sec));
                    mBtnRewind5Sec2.setContentDescription(getString(R.string.rewind1Sec));
                    mBtnRewind5Sec.setTag(1);
                    mBtnRewind5Sec2.setTag(1);
                    menu.dismiss();
                }
            });
            menu.addMenu(getString(R.string.rewind2Sec), R.drawable.ic_actionsheet_02sec_prev, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mBtnRewind5Sec.setImageResource(R.drawable.ic_abloop_02sec_prev);
                    mBtnRewind5Sec2.setImageResource(R.drawable.ic_abloop_02sec_prev);
                    mBtnRewind5Sec.setContentDescription(getString(R.string.rewind2Sec));
                    mBtnRewind5Sec2.setContentDescription(getString(R.string.rewind2Sec));
                    mBtnRewind5Sec.setTag(2);
                    mBtnRewind5Sec2.setTag(2);
                    menu.dismiss();
                }
            });
            menu.addMenu(getString(R.string.rewind3Sec), R.drawable.ic_actionsheet_03sec_prev, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mBtnRewind5Sec.setImageResource(R.drawable.ic_abloop_03sec_prev);
                    mBtnRewind5Sec2.setImageResource(R.drawable.ic_abloop_03sec_prev);
                    mBtnRewind5Sec.setContentDescription(getString(R.string.rewind3Sec));
                    mBtnRewind5Sec2.setContentDescription(getString(R.string.rewind3Sec));
                    mBtnRewind5Sec.setTag(3);
                    mBtnRewind5Sec2.setTag(3);
                    menu.dismiss();
                }
            });
            menu.addMenu(getString(R.string.rewind5Sec), R.drawable.ic_actionsheet_05sec_prev, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mBtnRewind5Sec.setImageResource(R.drawable.ic_abloop_05sec_prev);
                    mBtnRewind5Sec2.setImageResource(R.drawable.ic_abloop_05sec_prev);
                    mBtnRewind5Sec.setContentDescription(getString(R.string.rewind5Sec));
                    mBtnRewind5Sec2.setContentDescription(getString(R.string.rewind5Sec));
                    mBtnRewind5Sec.setTag(5);
                    mBtnRewind5Sec2.setTag(5);
                    menu.dismiss();
                }
            });
            menu.addMenu(getString(R.string.rewind10Sec), R.drawable.ic_actionsheet_10sec_prev, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mBtnRewind5Sec.setImageResource(R.drawable.ic_abloop_10sec_prev);
                    mBtnRewind5Sec2.setImageResource(R.drawable.ic_abloop_10sec_prev);
                    mBtnRewind5Sec.setContentDescription(getString(R.string.rewind10Sec));
                    mBtnRewind5Sec2.setContentDescription(getString(R.string.rewind10Sec));
                    mBtnRewind5Sec.setTag(10);
                    mBtnRewind5Sec2.setTag(10);
                    menu.dismiss();
                }
            });
            menu.setCancelMenu();
            menu.show();
        }
        else if(v.getId() == R.id.btnForward5Sec || v.getId() == R.id.btnForward5Sec2) {
            final BottomMenu menu = new BottomMenu(mActivity);
            menu.setTitle(getString(R.string.chooseForwardButton));
            menu.addMenu(getString(R.string.forward1Sec), R.drawable.ic_actionsheet_01sec_next, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mBtnForward5Sec.setImageResource(R.drawable.ic_abloop_01sec_next);
                    mBtnForward5Sec2.setImageResource(R.drawable.ic_abloop_01sec_next);
                    mBtnForward5Sec.setContentDescription(getString(R.string.forward1Sec));
                    mBtnForward5Sec2.setContentDescription(getString(R.string.forward1Sec));
                    mBtnForward5Sec.setTag(1);
                    mBtnForward5Sec2.setTag(1);
                    menu.dismiss();
                }
            });
            menu.addMenu(getString(R.string.forward2Sec), R.drawable.ic_actionsheet_02sec_next, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mBtnForward5Sec.setImageResource(R.drawable.ic_abloop_02sec_next);
                    mBtnForward5Sec2.setImageResource(R.drawable.ic_abloop_02sec_next);
                    mBtnForward5Sec.setContentDescription(getString(R.string.forward2Sec));
                    mBtnForward5Sec2.setContentDescription(getString(R.string.forward2Sec));
                    mBtnForward5Sec.setTag(2);
                    mBtnForward5Sec2.setTag(2);
                    menu.dismiss();
                }
            });
            menu.addMenu(getString(R.string.forward3Sec), R.drawable.ic_actionsheet_03sec_next, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mBtnForward5Sec.setImageResource(R.drawable.ic_abloop_03sec_next);
                    mBtnForward5Sec2.setImageResource(R.drawable.ic_abloop_03sec_next);
                    mBtnForward5Sec.setContentDescription(getString(R.string.forward3Sec));
                    mBtnForward5Sec2.setContentDescription(getString(R.string.forward3Sec));
                    mBtnForward5Sec.setTag(3);
                    mBtnForward5Sec2.setTag(3);
                    menu.dismiss();
                }
            });
            menu.addMenu(getString(R.string.forward5Sec), R.drawable.ic_actionsheet_05sec_next, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mBtnForward5Sec.setImageResource(R.drawable.ic_abloop_05sec_next);
                    mBtnForward5Sec2.setImageResource(R.drawable.ic_abloop_05sec_next);
                    mBtnForward5Sec.setContentDescription(getString(R.string.forward5Sec));
                    mBtnForward5Sec2.setContentDescription(getString(R.string.forward5Sec));
                    mBtnForward5Sec.setTag(5);
                    mBtnForward5Sec2.setTag(5);
                    menu.dismiss();
                }
            });
            menu.addMenu(getString(R.string.forward10Sec), R.drawable.ic_actionsheet_10sec_next, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mBtnForward5Sec.setImageResource(R.drawable.ic_abloop_10sec_next);
                    mBtnForward5Sec2.setImageResource(R.drawable.ic_abloop_10sec_next);
                    mBtnForward5Sec.setContentDescription(getString(R.string.forward10Sec));
                    mBtnForward5Sec2.setContentDescription(getString(R.string.forward10Sec));
                    mBtnForward5Sec.setTag(10);
                    mBtnForward5Sec2.setTag(10);
                    menu.dismiss();
                }
            });
            menu.setCancelMenu();
            menu.show();
        }
        return false;
    }

    @Override
    public void onFocusChange(View v, boolean hasFocus)
    {
        if(hasFocus)
        {
            if(v.getId() == R.id.textAValue)
                showABLoopPicker(true);
            else if(v.getId() == R.id.textBValue)
                showABLoopPicker(false);
            else if(v.getId() == R.id.textCurValue)
                showCurPicker();
        }
    }

    private void showABLoopPicker(boolean bAPos) {
        LayoutInflater inflater = mActivity.getLayoutInflater();
        View view = inflater.inflate(R.layout.ablooppicker, (ViewGroup)mActivity.findViewById(R.id.layout_root), false);
        final NumberPicker hourPicker = view.findViewById(R.id.abLoopHourPicker);
        final NumberPicker minutePicker = view.findViewById(R.id.abLoopMinutePicker);
        final NumberPicker secondPicker = view.findViewById(R.id.abLoopSecondPicker);
        final NumberPicker decimalPicker = view.findViewById(R.id.abLoopDecimalPicker);

        double dValue;
        if(bAPos) dValue = mActivity.getLoopAPos();
        else dValue = mActivity.getLoopBPos();
        int nMinute = (int)(dValue / 60);
        int nSecond = (int)(dValue % 60);
        int nHour = nMinute / 60;
        nMinute = nMinute % 60;
        int nDec = (int)((dValue * 100) % 100);
        String strHour = String.format(Locale.getDefault(), "%02d", nHour);
        String strMinute = String.format(Locale.getDefault(), "%02d", nMinute);
        String strSecond = String.format(Locale.getDefault(), "%02d", nSecond);
        String strDec = String.format(Locale.getDefault(), "%02d", nDec);

        final String[] arInts = {"59", "58", "57", "56", "55", "54", "53", "52", "51", "50", "49", "48", "47", "46", "45", "44", "43", "42", "41", "40", "39", "38", "37", "36", "35", "34", "33", "32", "31", "30", "29", "28", "27", "26", "25", "24", "23", "22", "21", "20", "19", "18", "17", "16", "15", "14", "13", "12", "11", "10", "09", "08", "07", "06", "05", "04", "03", "02", "01", "00"};
        hourPicker.setDisplayedValues(arInts);
        hourPicker.setMinValue(0);
        hourPicker.setMaxValue(59);
        for(int i = 0; i < arInts.length; i++)
        {
            if(arInts[i].equals(strHour))
                hourPicker.setValue(i);
        }

        minutePicker.setDisplayedValues(arInts);
        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(59);
        for(int i = 0; i < arInts.length; i++)
        {
            if(arInts[i].equals(strMinute))
                minutePicker.setValue(i);
        }
        secondPicker.setDisplayedValues(arInts);
        secondPicker.setMinValue(0);
        secondPicker.setMaxValue(59);
        for(int i = 0; i < arInts.length; i++)
        {
            if(arInts[i].equals(strSecond))
                secondPicker.setValue(i);
        }

        final String[] arDecimals = {"99", "98", "97", "96", "95", "94", "93", "92", "91", "90", "89", "88", "87", "86", "85", "84", "83", "82", "81", "80", "79", "78", "77", "76", "75", "74", "73", "72", "71", "70", "69", "68", "67", "66", "65", "64", "63", "62", "61", "60", "59", "58", "57", "56", "55", "54", "53", "52", "51", "50", "49", "48", "47", "46", "45", "44", "43", "42", "41", "40", "39", "38", "37", "36", "35", "34", "33", "32", "31", "30", "29", "28", "27", "26", "25", "24", "23", "22", "21", "20", "19", "18", "17", "16", "15", "14", "13", "12", "11", "10", "09", "08", "07", "06", "05", "04", "03", "02", "01", "00"};
        decimalPicker.setDisplayedValues(arDecimals);
        decimalPicker.setMinValue(0);
        decimalPicker.setMaxValue(99);
        for(int i = 0; i < arDecimals.length; i++)
        {
            if(arDecimals[i].equals(strDec))
                decimalPicker.setValue(i);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        if(bAPos) builder.setTitle(R.string.adjustAPos);
        else builder.setTitle(R.string.adjustBPos);
        final boolean f_bAPos = bAPos;
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                LayoutInflater inflater = mActivity.getLayoutInflater();
                inflater.inflate(R.layout.ablooppicker, (ViewGroup)mActivity.findViewById(R.id.layout_root), false);
                int nHour = Integer.parseInt(arInts[hourPicker.getValue()]);
                int nMinute = Integer.parseInt(arInts[minutePicker.getValue()]);
                int nSecond = Integer.parseInt(arInts[secondPicker.getValue()]);
                double dDec = Double.parseDouble(arDecimals[decimalPicker.getValue()]);
                double dPos = nHour * 3600 + nMinute * 60 + nSecond + dDec / 100.0;

                if(f_bAPos) setLoopA(dPos);
                else setLoopB(dPos);
                clearFocus();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                clearFocus();
            }
        });
        builder.setView(view);
        builder.show();
    }

    private void showCurPicker() {
        LayoutInflater inflater = mActivity.getLayoutInflater();
        View view = inflater.inflate(R.layout.ablooppicker, (ViewGroup)mActivity.findViewById(R.id.layout_root), false);
        final NumberPicker hourPicker = view.findViewById(R.id.abLoopHourPicker);
        final NumberPicker minutePicker = view.findViewById(R.id.abLoopMinutePicker);
        final NumberPicker secondPicker = view.findViewById(R.id.abLoopSecondPicker);
        final NumberPicker decimalPicker = view.findViewById(R.id.abLoopDecimalPicker);

        String strCurPos = mTextCurValue.getText().toString();
        String strHour = strCurPos.substring(0, 2);
        String strMinute = strCurPos.substring(3, 5);
        String strSecond = strCurPos.substring(6, 8);
        String strDec = strCurPos.substring(9, 11);

        final String[] arInts = {"59", "58", "57", "56", "55", "54", "53", "52", "51", "50", "49", "48", "47", "46", "45", "44", "43", "42", "41", "40", "39", "38", "37", "36", "35", "34", "33", "32", "31", "30", "29", "28", "27", "26", "25", "24", "23", "22", "21", "20", "19", "18", "17", "16", "15", "14", "13", "12", "11", "10", "09", "08", "07", "06", "05", "04", "03", "02", "01", "00"};
        final String[] arDecimals = {"99", "98", "97", "96", "95", "94", "93", "92", "91", "90", "89", "88", "87", "86", "85", "84", "83", "82", "81", "80", "79", "78", "77", "76", "75", "74", "73", "72", "71", "70", "69", "68", "67", "66", "65", "64", "63", "62", "61", "60", "59", "58", "57", "56", "55", "54", "53", "52", "51", "50", "49", "48", "47", "46", "45", "44", "43", "42", "41", "40", "39", "38", "37", "36", "35", "34", "33", "32", "31", "30", "29", "28", "27", "26", "25", "24", "23", "22", "21", "20", "19", "18", "17", "16", "15", "14", "13", "12", "11", "10", "09", "08", "07", "06", "05", "04", "03", "02", "01", "00"};

        hourPicker.setDisplayedValues(arInts);
        hourPicker.setMinValue(0);
        hourPicker.setMaxValue(59);
        hourPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker numberPicker, int nOldValue, int nNewValue) {
                int nHour = Integer.parseInt(arInts[nNewValue]);
                int nMinute = Integer.parseInt(arInts[minutePicker.getValue()]);
                int nSecond = Integer.parseInt(arInts[secondPicker.getValue()]);
                double dDec = Double.parseDouble(arDecimals[decimalPicker.getValue()]);
                double dPos = nHour * 3600 + nMinute * 60 + nSecond + dDec / 100.0;
                setCurPos(dPos);
            }
        });
        for(int i = 0; i < arInts.length; i++)
        {
            if(arInts[i].equals(strHour))
                hourPicker.setValue(i);
        }
        minutePicker.setDisplayedValues(arInts);
        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(59);
        minutePicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker numberPicker, int nOldValue, int nNewValue) {
                int nHour = Integer.parseInt(arInts[hourPicker.getValue()]);
                int nMinute = Integer.parseInt(arInts[nNewValue]);
                int nSecond = Integer.parseInt(arInts[secondPicker.getValue()]);
                double dDec = Double.parseDouble(arDecimals[decimalPicker.getValue()]);
                double dPos = nHour * 3600 + nMinute * 60 + nSecond + dDec / 100.0;
                setCurPos(dPos);
            }
        });
        for(int i = 0; i < arInts.length; i++)
        {
            if(arInts[i].equals(strMinute))
                minutePicker.setValue(i);
        }
        secondPicker.setDisplayedValues(arInts);
        secondPicker.setMinValue(0);
        secondPicker.setMaxValue(59);
        secondPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker numberPicker, int nOldValue, int nNewValue) {
                int nHour = Integer.parseInt(arInts[hourPicker.getValue()]);
                int nMinute = Integer.parseInt(arInts[minutePicker.getValue()]);
                int nSecond = Integer.parseInt(arInts[nNewValue]);
                double dDec = Double.parseDouble(arDecimals[decimalPicker.getValue()]);
                double dPos = nHour * 3600 + nMinute * 60 + nSecond + dDec / 100.0;
                setCurPos(dPos);
            }
        });
        for(int i = 0; i < arInts.length; i++)
        {
            if(arInts[i].equals(strSecond))
                secondPicker.setValue(i);
        }

        decimalPicker.setDisplayedValues(arDecimals);
        decimalPicker.setMinValue(0);
        decimalPicker.setMaxValue(99);
        decimalPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker numberPicker, int nOldValue, int nNewValue) {
                int nHour = Integer.parseInt(arInts[hourPicker.getValue()]);
                int nMinute = Integer.parseInt(arInts[minutePicker.getValue()]);
                int nSecond = Integer.parseInt(arInts[secondPicker.getValue()]);
                double dDec = Double.parseDouble(arDecimals[nNewValue]);
                double dPos = nHour * 3600 + nMinute * 60 + nSecond + dDec / 100.0;
                setCurPos(dPos);
            }
        });
        for(int i = 0; i < arDecimals.length; i++)
        {
            if(arDecimals[i].equals(strDec))
                decimalPicker.setValue(i);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setTitle(R.string.adjustCurPos);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

            @Override
            public void onClick(DialogInterface dialog, int which) {
                clearFocus();
            }
        });
        builder.setView(view);
        builder.show();
    }

    private void clearFocus()
    {
        mTextAValue.clearFocus();
        mTextBValue.clearFocus();
        mTextCurValue.clearFocus();
    }

    private void setLoopA(double dLoopA)
    {
        setLoopA(dLoopA, true);
    }

    public void setLoopA(double dLoopA, boolean bSave)
    {
        if(MainActivity.sStream == 0) return;

        double dLength = BASS.BASS_ChannelBytes2Seconds(MainActivity.sStream, BASS.BASS_ChannelGetLength(MainActivity.sStream, BASS.BASS_POS_BYTE));
        if(dLoopA >= dLength)
            dLoopA = dLength;

        if(mActivity.isLoopB() && dLoopA >= mActivity.getLoopBPos())
            dLoopA = mActivity.getLoopBPos() - 1.0;

        mActivity.setLoopAPos(dLoopA);
        mActivity.setLoopA(true);
        mBtnA.setSelected(true);
        mBtnA.setImageResource(R.drawable.ic_abloop_a_on);

        long nLength = BASS.BASS_ChannelGetLength(MainActivity.sStream, BASS.BASS_POS_BYTE);
        long nPos = BASS.BASS_ChannelSeconds2Bytes(MainActivity.sStream, dLoopA);
        int nScreenWidth = mWaveView.getWidth();
        int nMaxWidth = (int)(nScreenWidth * mWaveView.getZoom());
        int nLeft = (int) (mViewCurPos.getX() - nMaxWidth * nPos / nLength);
        if(nLeft > 0) nLeft = 0;
        mViewMaskA.getLayoutParams().width = nLeft;
        mViewMaskA.setVisibility(View.VISIBLE);
        mViewMaskA.requestLayout();

        int nMinute = (int)(mActivity.getLoopAPos() / 60);
        int nSecond = (int)(mActivity.getLoopAPos() % 60);
        int nHour = nMinute / 60;
        nMinute = nMinute % 60;
        int nDec = (int)((mActivity.getLoopAPos() * 100) % 100);
        mTextAValue.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d.%02d", nHour, nMinute, nSecond, nDec));

        if(bSave) mActivity.playlistFragment.updateSavingEffect();
    }

    private void setLoopB(double dLoopB)
    {
        setLoopB(dLoopB, true);
    }

    public void setLoopB(double dLoopB, boolean bSave)
    {
        if(MainActivity.sStream == 0) return;

        double dLength = BASS.BASS_ChannelBytes2Seconds(MainActivity.sStream, BASS.BASS_ChannelGetLength(MainActivity.sStream, BASS.BASS_POS_BYTE));
        if(dLoopB >= dLength)
            dLoopB = dLength;

        if(mActivity.isLoopA() && dLoopB <= mActivity.getLoopAPos())
            dLoopB = mActivity.getLoopAPos() + 1.0;

        mActivity.setLoopBPos(dLoopB);
        mActivity.setLoopB(true);
        mBtnB.setSelected(true);
        mBtnB.setImageResource(R.drawable.ic_abloop_b_on);
        long nLength = BASS.BASS_ChannelGetLength(MainActivity.sStream, BASS.BASS_POS_BYTE);
        long nPos = BASS.BASS_ChannelSeconds2Bytes(MainActivity.sStream, dLoopB);
        int nScreenWidth = mWaveView.getWidth();
        int nMaxWidth = (int)(nScreenWidth * mWaveView.getZoom());
        int nLeft = (int) (mViewCurPos.getX() - nMaxWidth * nPos / nLength);
        if(nLeft < 0) nLeft = 0;
        else if(nLeft > mWaveView.getWidth()) nLeft = mWaveView.getWidth();
        mViewMaskB.setTranslationX(nLeft);
        mViewMaskB.getLayoutParams().width = mWaveView.getWidth() - nLeft;
        mViewMaskB.setVisibility(View.VISIBLE);
        mViewMaskB.requestLayout();
        BASS.BASS_ChannelSetPosition(MainActivity.sStream, BASS.BASS_ChannelSeconds2Bytes(MainActivity.sStream, mActivity.getLoopAPos()), BASS.BASS_POS_BYTE);

        int nMinute = (int)(mActivity.getLoopBPos() / 60);
        int nSecond = (int)(mActivity.getLoopBPos() % 60);
        int nHour = nMinute / 60;
        nMinute = nMinute % 60;
        int nDec = (int)((mActivity.getLoopBPos() * 100) % 100);
        mTextBValue.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d.%02d", nHour, nMinute, nSecond, nDec));

        mActivity.setSync();

        if(bSave) mActivity.playlistFragment.updateSavingEffect();
    }

    public void setCurPos(double dPos)
    {
        if(MainActivity.sStream == 0) return;

        double dLength = BASS.BASS_ChannelBytes2Seconds(MainActivity.sStream, BASS.BASS_ChannelGetLength(MainActivity.sStream, BASS.BASS_POS_BYTE));
        boolean bReverse = mActivity.effectFragment.isReverse();
        if(bReverse) {
            if(dPos <= 0.0f) {
                if(BASS.BASS_ChannelIsActive(MainActivity.sStream) == BASS.BASS_ACTIVE_PLAYING) {
                    mActivity.onEnded(true);
                    return;
                }
                dPos = 0.0f;
            }
        }
        else {
            if(dLength <= dPos) {
                if(BASS.BASS_ChannelIsActive(MainActivity.sStream) == BASS.BASS_ACTIVE_PLAYING) {
                    mActivity.onEnded(true);
                    return;
                }
                dPos = dLength;
            }
        }
        BASS.BASS_ChannelSetPosition(MainActivity.sStream, BASS.BASS_ChannelSeconds2Bytes(MainActivity.sStream, dPos), BASS.BASS_POS_BYTE);
        mActivity.setSync();

        double dCurPos = dPos;
        int i = 0;
        for( ; i < mMarkerTimes.size(); i++) {
            dPos = mMarkerTimes.get(i);
            if((!bReverse && dCurPos < dPos) || (bReverse && dCurPos < dPos - 1.0))
                break;
        }
        mMarker = i - 1;

        mActivity.setSync();

        mActivity.playlistFragment.updateSavingEffect();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View v, MotionEvent event)
    {
        if(v.getId() == R.id.relativeZoomOut) {
            if(event.getAction() == MotionEvent.ACTION_UP) {
                if(MainActivity.sStream == 0) return false;
                mContinue = false;
                setZoomOut();
                mWaveView.redrawWaveForm();
            }
        }
        else if(v.getId() == R.id.relativeZoomIn) {
            if(event.getAction() == MotionEvent.ACTION_UP) {
                if(MainActivity.sStream == 0) return false;
                mContinue = false;
                setZoomIn();
                mWaveView.redrawWaveForm();
            }
        }
        else if(v.getId() == R.id.viewBtnRewindLeft || v.getId() == R.id.viewBtnRewindRight || v.getId() == R.id.btnRewind5Sec || v.getId() == R.id.btnRewind5Sec2)
        {
            if (event.getAction() == MotionEvent.ACTION_UP)
            {
                if (MainActivity.sStream != 0)
                {
                    double dLength = BASS.BASS_ChannelBytes2Seconds(MainActivity.sStream, BASS.BASS_ChannelGetLength(MainActivity.sStream, BASS.BASS_POS_BYTE));
                    double dPos = BASS.BASS_ChannelBytes2Seconds(MainActivity.sStream, BASS.BASS_ChannelGetPosition(MainActivity.sStream, BASS.BASS_POS_BYTE));
                    if((Integer)mBtnRewind5Sec.getTag() == 1) dPos -= 1.0;
                    else if((Integer)mBtnRewind5Sec.getTag() == 2) dPos -= 2.0;
                    else if((Integer)mBtnRewind5Sec.getTag() == 3) dPos -= 3.0;
                    else if((Integer)mBtnRewind5Sec.getTag() == 5) dPos -= 5.0;
                    else if((Integer)mBtnRewind5Sec.getTag() == 10) dPos -= 10.0;
                    boolean bReverse = mActivity.effectFragment.isReverse();
                    if(bReverse) {
                        if(dPos <= 0.0f) {
                            if(BASS.BASS_ChannelIsActive(MainActivity.sStream) == BASS.BASS_ACTIVE_PLAYING) {
                                mActivity.onEnded(true);
                                return true;
                            }
                            dPos = 0.0f;
                        }
                    }
                    else {
                        if(dLength <= dPos) {
                            if(BASS.BASS_ChannelIsActive(MainActivity.sStream) == BASS.BASS_ACTIVE_PLAYING) {
                                mActivity.onEnded(true);
                                return true;
                            }
                            dPos = dLength;
                        }
                    }
                    if(mABButton.getVisibility() == View.VISIBLE && ((mActivity.isLoopA() && dPos < mActivity.getLoopAPos()) || (mActivity.isLoopB() && mActivity.getLoopBPos() < dPos)))
                        dPos = BASS.BASS_ChannelBytes2Seconds(MainActivity.sStream, BASS.BASS_ChannelSeconds2Bytes(MainActivity.sStream, mActivity.getLoopAPos()));
                    BASS.BASS_ChannelSetPosition(MainActivity.sStream, BASS.BASS_ChannelSeconds2Bytes(MainActivity.sStream, dPos), BASS.BASS_POS_BYTE);
                    mActivity.setSync();
                    mTouching = true;
                    updateCurPos();
                    mTouching = false;
                }
            }
        }
        else if(v.getId() == R.id.viewBtnForwardLeft || v.getId() == R.id.viewBtnForwardRight || v.getId() == R.id.btnForward5Sec || v.getId() == R.id.btnForward5Sec2)
        {
            if (event.getAction() == MotionEvent.ACTION_UP)
            {
                if (MainActivity.sStream != 0)
                {
                    double dLength = BASS.BASS_ChannelBytes2Seconds(MainActivity.sStream, BASS.BASS_ChannelGetLength(MainActivity.sStream, BASS.BASS_POS_BYTE));
                    double dPos = BASS.BASS_ChannelBytes2Seconds(MainActivity.sStream, BASS.BASS_ChannelGetPosition(MainActivity.sStream, BASS.BASS_POS_BYTE));
                    if((Integer)mBtnForward5Sec.getTag() == 1) dPos += 1.0;
                    else if((Integer)mBtnForward5Sec.getTag() == 2) dPos += 2.0;
                    else if((Integer)mBtnForward5Sec.getTag() == 3) dPos += 3.0;
                    else if((Integer)mBtnForward5Sec.getTag() == 5) dPos += 5.0;
                    else if((Integer)mBtnForward5Sec.getTag() == 10) dPos += 10.0;
                    boolean bReverse = mActivity.effectFragment.isReverse();
                    if(bReverse) {
                        if(dPos <= 0.0f) {
                            if(BASS.BASS_ChannelIsActive(MainActivity.sStream) == BASS.BASS_ACTIVE_PLAYING) {
                                mActivity.onEnded(true);
                                return true;
                            }
                            dPos = 0.0f;
                        }
                    }
                    else {
                        if(dLength <= dPos) {
                            if(BASS.BASS_ChannelIsActive(MainActivity.sStream) == BASS.BASS_ACTIVE_PLAYING) {
                                mActivity.onEnded(true);
                                return true;
                            }
                            dPos = dLength;
                        }
                    }
                    if(mABButton.getVisibility() == View.VISIBLE && ((mActivity.isLoopA() && dPos < mActivity.getLoopAPos()) || (mActivity.isLoopB() && mActivity.getLoopBPos() < dPos)))
                        dPos = BASS.BASS_ChannelBytes2Seconds(MainActivity.sStream, BASS.BASS_ChannelSeconds2Bytes(MainActivity.sStream, mActivity.getLoopAPos()));
                    BASS.BASS_ChannelSetPosition(MainActivity.sStream, BASS.BASS_ChannelSeconds2Bytes(MainActivity.sStream, dPos), BASS.BASS_POS_BYTE);
                    mActivity.setSync();
                    mTouching = true;
                    updateCurPos();
                    mTouching = false;
                }
            }
        }
        else if(v.getId() == R.id.viewBtnALeft || v.getId() == R.id.viewBtnARight || v.getId() == R.id.btnA)
        {
            if(event.getAction() == MotionEvent.ACTION_UP)
            {
                if(MainActivity.sStream != 0)
                {
                    if(mBtnA.isSelected()) {
                        mActivity.setLoopAPos(0.0);
                        mActivity.setLoopA(false);
                        mBtnA.setSelected(false);
                        mBtnA.setImageResource(R.drawable.ic_abloop_a);
                        mViewMaskA.setVisibility(View.INVISIBLE);
                        mTextAValue.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d.%02d", 0, 0, 0, 0));
                    }
                    else {
                        mActivity.setLoopAPos(BASS.BASS_ChannelBytes2Seconds(MainActivity.sStream, BASS.BASS_ChannelGetPosition(MainActivity.sStream, BASS.BASS_POS_BYTE)));
                        mActivity.setLoopA(true);
                        mBtnA.setSelected(true);
                        mBtnA.setImageResource(R.drawable.ic_abloop_a_on);
                        long nLength = BASS.BASS_ChannelGetLength(MainActivity.sStream, BASS.BASS_POS_BYTE);
                        long nPos = BASS.BASS_ChannelGetPosition(MainActivity.sStream, BASS.BASS_POS_BYTE);
                        int nBkWidth = mWaveView.getWidth();
                        mViewMaskA.getLayoutParams().width = (int) (nBkWidth * nPos / nLength);
                        mViewMaskA.setVisibility(View.VISIBLE);
                        mViewMaskA.requestLayout();
                        if(mActivity.effectFragment.isReverse())
                            BASS.BASS_ChannelSetPosition(MainActivity.sStream, BASS.BASS_ChannelSeconds2Bytes(MainActivity.sStream, mActivity.getLoopBPos()), BASS.BASS_POS_BYTE);

                        int nMinute = (int)(mActivity.getLoopAPos() / 60);
                        int nSecond = (int)(mActivity.getLoopAPos() % 60);
                        int nHour = nMinute / 60;
                        nMinute = nMinute % 60;
                        int nDec = (int)((mActivity.getLoopAPos() * 100) % 100);
                        mTextAValue.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d.%02d", nHour, nMinute, nSecond, nDec));
                    }
                    mActivity.playlistFragment.updateSavingEffect();
                }
            }
        }
        else if(v.getId() == R.id.viewBtnBLeft || v.getId() == R.id.viewBtnBRight || v.getId() == R.id.btnB)
        {
            if(event.getAction() == MotionEvent.ACTION_UP)
            {
                if(MainActivity.sStream != 0)
                {
                    if(mBtnB.isSelected()) {
                        mActivity.setLoopBPos(0.0);
                        mActivity.setLoopB(false);
                        mBtnB.setSelected(false);
                        mBtnB.setImageResource(R.drawable.ic_abloop_b);
                        mViewMaskB.setVisibility(View.INVISIBLE);

                        double dLength = BASS.BASS_ChannelBytes2Seconds(MainActivity.sStream, BASS.BASS_ChannelGetLength(MainActivity.sStream, BASS.BASS_POS_BYTE));
                        int nMinute = (int)(dLength / 60);
                        int nSecond = (int)(dLength % 60);
                        int nHour = nMinute / 60;
                        nMinute = nMinute % 60;
                        int nDec = (int)((dLength * 100) % 100);
                        mTextBValue.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d.%02d", nHour, nMinute, nSecond, nDec));
                    }
                    else {
                        mActivity.setLoopBPos(BASS.BASS_ChannelBytes2Seconds(MainActivity.sStream, BASS.BASS_ChannelGetPosition(MainActivity.sStream, BASS.BASS_POS_BYTE)));
                        mActivity.setLoopB(true);
                        mBtnB.setSelected(true);
                        mBtnB.setImageResource(R.drawable.ic_abloop_b_on);
                        long nLength = BASS.BASS_ChannelGetLength(MainActivity.sStream, BASS.BASS_POS_BYTE);
                        long nPos = BASS.BASS_ChannelGetPosition(MainActivity.sStream, BASS.BASS_POS_BYTE);
                        int nBkWidth = mWaveView.getWidth();
                        int nLeft = (int) (nBkWidth * nPos / nLength);
                        mViewMaskB.setTranslationX(nLeft);
                        mViewMaskB.getLayoutParams().width = nBkWidth - nLeft;
                        mViewMaskB.setVisibility(View.VISIBLE);
                        mViewMaskB.requestLayout();
                        if(!mActivity.effectFragment.isReverse())
                            BASS.BASS_ChannelSetPosition(MainActivity.sStream, BASS.BASS_ChannelSeconds2Bytes(MainActivity.sStream, mActivity.getLoopAPos()), BASS.BASS_POS_BYTE);

                        int nMinute = (int)(mActivity.getLoopBPos() / 60);
                        int nSecond = (int)(mActivity.getLoopBPos() % 60);
                        int nHour = nMinute / 60;
                        nMinute = nMinute % 60;
                        int nDec = (int)((mActivity.getLoopBPos() * 100) % 100);
                        mTextBValue.setText(String.format(Locale.getDefault(), "%02d:%02d:%02d.%02d", nHour, nMinute, nSecond, nDec));
                    }
                    mActivity.setSync();

                    mActivity.playlistFragment.updateSavingEffect();
                }
            }
        }
        else if(v.getId() == R.id.btnPrevmarker)
        {
            if(event.getAction() == MotionEvent.ACTION_UP)
            {
                if(MainActivity.sStream != 0)
                {
                    boolean bReverse = mActivity.effectFragment.isReverse();
                    double dCurPos = BASS.BASS_ChannelBytes2Seconds(MainActivity.sStream, BASS.BASS_ChannelGetPosition(MainActivity.sStream, BASS.BASS_POS_BYTE));
                    int i = mMarkerTimes.size() - 1;
                    for( ; i >= 0; i--)
                    {
                        double dPos = mMarkerTimes.get(i);
                        if((!bReverse && dCurPos >= dPos + 1.0) || (bReverse && dCurPos >= dPos))
                        {
                            BASS.BASS_ChannelSetPosition(MainActivity.sStream, BASS.BASS_ChannelSeconds2Bytes(MainActivity.sStream, dPos), BASS.BASS_POS_BYTE);
                            break;
                        }
                    }
                    mMarker = i;
                    mActivity.setSync();

                    mActivity.playlistFragment.updateSavingEffect();
                }
            }
        }
        else if(v.getId() == R.id.btnNextmarker)
        {
            if(event.getAction() == MotionEvent.ACTION_UP)
            {
                if(MainActivity.sStream != 0)
                {
                    boolean bReverse = mActivity.effectFragment.isReverse();
                    double dCurPos = BASS.BASS_ChannelBytes2Seconds(MainActivity.sStream, BASS.BASS_ChannelGetPosition(MainActivity.sStream, BASS.BASS_POS_BYTE));
                    int i = 0;
                    for( ; i < mMarkerTimes.size(); i++)
                    {
                        double dPos = mMarkerTimes.get(i);
                        if((!bReverse && dCurPos < dPos) || (bReverse && dCurPos < dPos - 1.0))
                        {
                            BASS.BASS_ChannelSetPosition(MainActivity.sStream, BASS.BASS_ChannelSeconds2Bytes(MainActivity.sStream, dPos), BASS.BASS_POS_BYTE);
                            break;
                        }
                    }
                    mMarker = i;
                    mActivity.setSync();

                    mActivity.playlistFragment.updateSavingEffect();
                }
            }
        }
        else if(v.getId() == R.id.btnAddmarker)
        {
            if(event.getAction() == MotionEvent.ACTION_UP)
            {
                if(MainActivity.sStream != 0)
                {
                    int nScreenWidth = mWaveView.getWidth();
                    int nMaxWidth = (int)(nScreenWidth * mWaveView.getZoom());
                    double dCurPos = BASS.BASS_ChannelBytes2Seconds(MainActivity.sStream, BASS.BASS_ChannelGetPosition(MainActivity.sStream, BASS.BASS_POS_BYTE));
                    double dLength = BASS.BASS_ChannelBytes2Seconds(MainActivity.sStream, BASS.BASS_ChannelGetLength(MainActivity.sStream, BASS.BASS_POS_BYTE));
                    TextView textView = new TextView(mActivity);
                    textView.setText("▼");
                    mRelativeLoop.addView(textView);
                    textView.measure(View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED), View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
                    int nLeft = (int) (mViewCurPos.getX() - nMaxWidth * dCurPos / dLength - textView.getMeasuredWidth() / 2.0f);
                    int nTop = mRelativeWave.getTop() - textView.getMeasuredHeight();
                    textView.setTranslationX(nLeft);
                    textView.setTranslationY(nTop);
                    textView.requestLayout();
                    boolean bAdded = false;
                    int i = 0;
                    for( ; i < mMarkerTimes.size(); i++)
                    {
                        double dPos = mMarkerTimes.get(i);
                        if(dCurPos < dPos)
                        {
                            bAdded = true;
                            mMarkerTimes.add(i, dCurPos);
                            mMarkerTexts.add(i, textView);
                            break;
                        }
                    }
                    if(!bAdded)
                    {
                        mMarkerTimes.add(dCurPos);
                        mMarkerTexts.add(textView);
                    }
                    mMarker = i;

                    mActivity.playlistFragment.updateSavingEffect();
                }
            }
        }
        else if (v.getId() == R.id.btnDelmarker)
        {
            if(event.getAction() == MotionEvent.ACTION_UP)
            {
                if(MainActivity.sStream != 0)
                {
                    double dCurPos = BASS.BASS_ChannelBytes2Seconds(MainActivity.sStream, BASS.BASS_ChannelGetPosition(MainActivity.sStream, BASS.BASS_POS_BYTE));
                    for(int i = mMarkerTimes.size() - 1; i >= 0; i--)
                    {
                        double dPos = mMarkerTimes.get(i);
                        if(dCurPos >= dPos)
                        {
                            mMarkerTimes.remove(i);
                            TextView textView = mMarkerTexts.get(i);
                            mRelativeLoop.removeView(textView);
                            mMarkerTexts.remove(i);
                            break;
                        }
                    }
                    mActivity.playlistFragment.updateSavingEffect();
                }
            }
        }
        else if (v.getId() == R.id.btnLoopmarker)
        {
            if(event.getAction() == MotionEvent.ACTION_UP)
            {
                if(mBtnLoopmarker.isSelected())
                {
                    mBtnLoopmarker.setSelected(false);
                    mBtnLoopmarker.setImageResource(R.drawable.ic_abloop_marker_loop);
                }
                else
                {
                    mBtnLoopmarker.setSelected(true);
                    mBtnLoopmarker.setImageResource(R.drawable.ic_abloop_marker_loop_on);
                }

                if(MainActivity.sStream != 0)
                {
                    double dCurPos = BASS.BASS_ChannelBytes2Seconds(MainActivity.sStream, BASS.BASS_ChannelGetPosition(MainActivity.sStream, BASS.BASS_POS_BYTE));
                    int i = 0;
                    for( ; i < mMarkerTimes.size(); i++) {
                        double dPos = mMarkerTimes.get(i);
                        if(dCurPos < dPos)
                        {
                            break;
                        }
                    }
                    mMarker = i - 1;

                    mActivity.setSync();

                    mActivity.playlistFragment.updateSavingEffect();
                }
            }
        }
        else if(v.getId() == R.id.waveView)
        {
            if(event.getAction() == MotionEvent.ACTION_DOWN) mTouching = true;
            else if(event.getAction() == MotionEvent.ACTION_UP)  mTouching = false;

            float fX = event.getX();
            int nBkWidth = mWaveView.getWidth();
            double dLength = BASS.BASS_ChannelBytes2Seconds(MainActivity.sStream, BASS.BASS_ChannelGetLength(MainActivity.sStream, BASS.BASS_POS_BYTE));
            double dPos = dLength * fX / nBkWidth;
            if(mABButton.getVisibility() == View.VISIBLE && ((mActivity.isLoopA() && dPos < mActivity.getLoopAPos()) || (mActivity.isLoopB() && mActivity.getLoopBPos() < dPos)))
                dPos = mActivity.getLoopAPos();
            setCurPos(dPos);
            updateCurPos();
            return true;
        }

        return false;
    }

    public void clearLoop(boolean bSave)
    {
        if(mActivity == null) return;

        if(mBtnA != null) {
            mBtnA.setSelected(false);
            mBtnA.setImageResource(R.drawable.ic_abloop_a);
        }

        if(mViewMaskA != null) mViewMaskA.setVisibility(View.INVISIBLE);

        if(mBtnB != null)
        {
            mBtnB.setSelected(false);
            mBtnB.setImageResource(R.drawable.ic_abloop_b);
        }

        if(mViewMaskB != null) mViewMaskB.setVisibility(View.INVISIBLE);

        for(int i = 0; i < mMarkerTexts.size(); i++)
        {
            TextView textView = mMarkerTexts.get(i);
            mRelativeLoop.removeView(textView);
        }

        mTextAValue.setText(getString(R.string.zeroHMS));
        mTextBValue.setText(getString(R.string.zeroHMS));

        mMarkerTimes.clear();
        mMarkerTexts.clear();

        mWaveView.clearWaveForm(true);

        if(bSave) mActivity.playlistFragment.updateSavingEffect();
    }

    public void drawWaveForm(String strPath)
    {
        double dLength = BASS.BASS_ChannelBytes2Seconds(MainActivity.sStream, BASS.BASS_ChannelGetLength(MainActivity.sStream, BASS.BASS_POS_BYTE));
        int nMinute = (int)(dLength / 60);
        int nSecond = (int)(dLength % 60);
        int nHour = nMinute / 60;
        nMinute = nMinute % 60;
        int nDec = (int)((dLength * 100) % 100);
        String strTextBValue = (nHour < 10 ? "0" : "") + nHour + (nMinute < 10 ? ":0" : ":") + nMinute + (nSecond < 10 ? ":0" : ":") + nSecond + (nDec < 10 ? ".0" : ".") + nDec;
        mTextBValue.setText(strTextBValue);
        mWaveView.drawWaveForm(strPath);
    }

    public double getMarkerSrcPos()
    {
        double dPos = 0.0;

        if(mActivity.effectFragment.isReverse()) {
            if(mMarkerButton.getVisibility() == View.VISIBLE && mBtnLoopmarker.isSelected()) // マーカー再生中
            {
                dPos = BASS.BASS_ChannelBytes2Seconds(MainActivity.sStream, BASS.BASS_ChannelGetLength(MainActivity.sStream, BASS.BASS_POS_BYTE));
                if(mMarker >= 0 && mMarker < mMarkerTimes.size()) {
                    dPos = mMarkerTimes.get(mMarker);
                }
            }
        }
        else {
            if(mMarkerButton.getVisibility() == View.VISIBLE && mBtnLoopmarker.isSelected()) // マーカー再生中
            {
                if(mMarker >= 0 && mMarker < mMarkerTimes.size()) {
                    dPos = mMarkerTimes.get(mMarker);
                }
            }
        }
        return dPos;
    }

    public double getMarkerDstPos()
    {
        double dPos = 0.0;

        if(mActivity.effectFragment.isReverse()) {
            if(mMarkerButton.getVisibility() == View.VISIBLE && mBtnLoopmarker.isSelected()) // マーカー再生中
            {
                if(mMarker - 1 >= 0 && mMarker - 1 < mMarkerTimes.size()) {
                    dPos = mMarkerTimes.get(mMarker - 1);
                }
            }
        }
        else {
            if(mMarkerButton.getVisibility() == View.VISIBLE && mBtnLoopmarker.isSelected()) // マーカー再生中
            {
                dPos = BASS.BASS_ChannelBytes2Seconds(MainActivity.sStream, BASS.BASS_ChannelGetLength(MainActivity.sStream, BASS.BASS_POS_BYTE));
                if(mMarker + 1 >= 0 && mMarker + 1 < mMarkerTimes.size()) {
                    dPos = mMarkerTimes.get(mMarker + 1);
                }
            }
        }
        return dPos;
    }
}
