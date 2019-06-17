/*
 * PitchFragmentDialog
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
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetDialog;
import android.view.View;
import android.view.WindowManager;
import android.widget.NumberPicker;
import java.util.Locale;

class PitchFragmentDialog extends BottomSheetDialog {
    private MainActivity mActivity;
    private NumberPicker mIntNumberPicker1;
    private NumberPicker mIntNumberPicker2;
    private NumberPicker mIntNumberPicker3;
    private NumberPicker mDecNumberPicker;

    @SuppressLint("ClickableViewAccessibility")
    PitchFragmentDialog(@NonNull Context context) {
        super(context);
        mActivity = (MainActivity)context;
        View view = getLayoutInflater().inflate(R.layout.dialog_pitch, null);

        float fPitch = mActivity.controlFragment.getPitch();

        String flag;
        if(fPitch >= 0.0f) flag = "♯";
        else flag = "♭";

        String strTemp = String.format(Locale.getDefault(), "%s%04.1f", flag, fPitch >= 0.0f ? fPitch : -fPitch);
        String strInt1 = strTemp.substring(0, 1);
        String strInt2 = strTemp.substring(1, 2);
        String strInt3 = strTemp.substring(2, 3);
        String strDec = strTemp.substring(4, 5);

        mIntNumberPicker1 = view.findViewById(R.id.intPitchPicker1);
        mIntNumberPicker2 = view.findViewById(R.id.intPitchPicker2);
        mIntNumberPicker3 = view.findViewById(R.id.intPitchPicker3);
        mDecNumberPicker = view.findViewById(R.id.decPitchPicker);

        final String[] arInts1 = {"♯", "♭"};
        final String[] arInts2 = {"6", "5", "4", "3", "2", "1", "0"};
        final String[] arInts3 = {"9", "8", "7", "6", "5", "4", "3", "2", "1", "0"};
        final String[] arDecs = {"9", "8", "7", "6", "5", "4", "3", "2", "1", "0"};

        mIntNumberPicker1.setDisplayedValues(arInts1);
        mIntNumberPicker2.setDisplayedValues(arInts2);
        mIntNumberPicker3.setDisplayedValues(arInts3);
        mDecNumberPicker.setDisplayedValues(arDecs);

        mIntNumberPicker1.setMaxValue(1);
        mIntNumberPicker2.setMaxValue(6);
        mIntNumberPicker3.setMaxValue(9);
        mDecNumberPicker.setMaxValue(9);

        mIntNumberPicker1.setWrapSelectorWheel(false);
        mIntNumberPicker2.setWrapSelectorWheel(false);
        mIntNumberPicker3.setWrapSelectorWheel(false);
        mDecNumberPicker.setWrapSelectorWheel(false);

        for(int i = 0; i < arInts1.length; i++) {
            if(arInts1[i].equals(strInt1))
                mIntNumberPicker1.setValue(i);
        }
        for(int i = 0; i < arInts2.length; i++) {
            if(arInts2[i].equals(strInt2))
                mIntNumberPicker2.setValue(i);
        }
        for(int i = 0; i < arInts3.length; i++) {
            if(arInts3[i].equals(strInt3))
                mIntNumberPicker3.setValue(i);
        }
        for(int i = 0; i < arDecs.length; i++) {
            if(arDecs[i].equals(strDec))
                mDecNumberPicker.setValue(i);
        }

        NumberPicker.OnValueChangeListener listener = new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker numberPicker, int i, int i1) {
                String strInt1 = arInts1[mIntNumberPicker1.getValue()];
                strInt1 = strInt1.replace("♯", "");
                strInt1 = strInt1.replace("♭", "-");
                String strInt2 = arInts2[mIntNumberPicker2.getValue()];
                String strInt3 = arInts3[mIntNumberPicker3.getValue()];
                String strDec = arDecs[mDecNumberPicker.getValue()];
                String strPitch = strInt1.trim() + strInt2.trim() + strInt3.trim() + "." + strDec;
                float fPitch = Float.parseFloat(strPitch);
                if(fPitch > 60.0f) {
                    fPitch = 60.0f;
                    mIntNumberPicker1.setValue(0);
                    mIntNumberPicker2.setValue(0);
                    mIntNumberPicker3.setValue(10);
                    mDecNumberPicker.setValue(10);
                }
                else if(fPitch < -60.0f) {
                    fPitch = -60.0f;
                    mIntNumberPicker1.setValue(1);
                    mIntNumberPicker2.setValue(0);
                    mIntNumberPicker3.setValue(10);
                    mDecNumberPicker.setValue(10);
                }

                mActivity.controlFragment.setPitch(fPitch);
            }
        };

        mIntNumberPicker1.setOnValueChangedListener(listener);
        mIntNumberPicker2.setOnValueChangedListener(listener);
        mIntNumberPicker3.setOnValueChangedListener(listener);
        mDecNumberPicker.setOnValueChangedListener(listener);

        setContentView(view);
        if(getWindow() != null) {
            WindowManager.LayoutParams lp = getWindow().getAttributes();
            lp.dimAmount = 0.0f;
            getWindow().setAttributes(lp);
        }

        setOnCancelListener(new OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialogInterface) {
                mActivity.controlFragment.clearFocus();
            }
        });
    }
}
