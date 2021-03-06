/*
 *  Copyright (C) 2010-2012 Stichting Akvo (Akvo Foundation)
 *
 *  This file is part of Akvo FLOW.
 *
 *  Akvo FLOW is free software: you can redistribute it and modify it under the terms of
 *  the GNU Affero General Public License (AGPL) as published by the Free Software Foundation,
 *  either version 3 of the License or any later version.
 *
 *  Akvo FLOW is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Affero General Public License included below for more details.
 *
 *  The full license text can also be seen at <http://www.gnu.org/licenses/agpl.html>.
 */

package org.akvo.flow.util;

/**
 * simple data structure to return language preference data
 * 
 * @author Mark Westra
 */
public class LangsPreferenceData {
    private String[] langsSelectedNameArray;
    private boolean[] langsSelectedBooleanArray;
    private int[] langsSelectedMasterIndexArray;

    public LangsPreferenceData(String[] langsSelectedNameArray,
            boolean[] langsSelectedBooleanArray, int[] langsSelectedMasterIndexArray) {
        this.setLangsSelectedNameArray(langsSelectedNameArray);
        this.setLangsSelectedBooleanArray(langsSelectedBooleanArray);
        this.setLangsSelectedMasterIndexArray(langsSelectedMasterIndexArray);
    }

    public String[] getLangsSelectedNameArray() {
        return langsSelectedNameArray;
    }

    public void setLangsSelectedNameArray(String[] langsSelectedNameArray) {
        this.langsSelectedNameArray = langsSelectedNameArray;
    }

    public boolean[] getLangsSelectedBooleanArray() {
        return langsSelectedBooleanArray;
    }

    public void setLangsSelectedBooleanArray(boolean[] langsSelectedBooleanArray) {
        this.langsSelectedBooleanArray = langsSelectedBooleanArray;
    }

    public int[] getLangsSelectedMasterIndexArray() {
        return langsSelectedMasterIndexArray;
    }

    public void setLangsSelectedMasterIndexArray(
            int[] langsSelectedMasterIndexArray) {
        this.langsSelectedMasterIndexArray = langsSelectedMasterIndexArray;
    }

}
