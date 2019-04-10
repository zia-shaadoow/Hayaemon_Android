/*
 * EqualizerItem
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

import java.util.ArrayList;

class EqualizerItem {
    private String strEqualizerName;
    private final ArrayList<Integer> arPresets;
    private boolean bSelected = false;

    void setEqualizerName(String strEqualizerName) { this.strEqualizerName = strEqualizerName; }
    String getEqualizerName() { return strEqualizerName; }
    ArrayList<Integer> getArPresets() { return arPresets; }
    void setSelected(boolean bSelected) { this.bSelected = bSelected; }
    public boolean isSelected() { return bSelected; }

    EqualizerItem(String strEqualizerName, ArrayList<Integer> arPresets)
    {
        this.strEqualizerName = strEqualizerName;
        this.arPresets = arPresets;
    }
}
