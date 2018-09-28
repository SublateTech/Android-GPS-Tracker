/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.sublate.gpstracker.Tracker;

import android.content.ContentValues;
import android.database.Cursor;
import android.location.Location;


/**
 * Class that holds a tracker entry. An entry can be either a valid location, or
 * a simple log msg
 *
 * It provides a concrete data structure to represent data stored in the
 * TrackerProvider
 */
class ScheduleEntry {

    static final String ID = "id";
    static final String NAME = "name";
    static final String TIMESTART = "timeStart";
    static final String TIMEEND = "timeEnd";

    static final String STRING_DATA = "STRING";
    static final String INT_DATA = "INTEGER";
    static final String REAL_DATA = "REAL";
    static final String BLOB_DATA = "BLOB";

    static final String[] ATTRIBUTES = {
            ID, NAME,TIMESTART,TIMEEND};
    static final String[] ATTRIBUTES_DATA_TYPE = {
            INT_DATA + " PRIMARY KEY", STRING_DATA, STRING_DATA, STRING_DATA};

    private String mTimeStart;
    private String mTimeEnd;
    private String mName;
    private int mId;

    public ScheduleEntry() {
        mTimeStart = "";
    }

     /**
     * Creates a TrackerEntry from a Location
     */
    static ScheduleEntry createEntry(Location loc, float distFromNetLocation) {
        ScheduleEntry entry = new ScheduleEntry();

        String timestampVal = DateUtils.getCurrentKMLTimestamp();
        //entry.setTimestamp(timestampVal);



        return entry;
    }

    /**
     * Creates a TrackerEntry from a log msg
     */
    static ScheduleEntry createEntry(String tag, String msg) {
        ScheduleEntry entry = new ScheduleEntry();
        String timestampVal = DateUtils.getCurrentKMLTimestamp();
        //entry.setTimestamp(timestampVal);
        return entry;
    }

    public void setTimeStart(String time) {
        mTimeStart = time;
    }
    public void setTimeEnd(String time) {
        mTimeEnd = time;
    }
    public void setName(String name) { mName = name;   }
    public void setId(int id) {
        mId =id;
    }

    public int getId() {
        return mId;
    }
    public String getTimeStart() {
        return mTimeStart;
    }
    public String getTimeEnd() {
        return mTimeEnd;
    }
    public String getName() {
        return mName;
    }





    static void buildCreationString(StringBuilder builder) {
        if (ATTRIBUTES.length != ATTRIBUTES_DATA_TYPE.length) {
            throw new IllegalArgumentException(
                    "Attribute length does not match data type length");
        }
        for (int i = 0; i < ATTRIBUTES_DATA_TYPE.length; i++) {
            if (i != 0) {
                builder.append(", ");
            }
            builder.append(String.format("%s %s", ATTRIBUTES[i],
                    ATTRIBUTES_DATA_TYPE[i]));
        }
    }

    ContentValues getAsContentValues() {
        ContentValues cValues = new ContentValues(ATTRIBUTES.length);
        cValues.put(NAME,mName);
        cValues.put(TIMESTART, mTimeStart);
        cValues.put(TIMEEND, mTimeEnd);

        return cValues;
    }

    static ScheduleEntry createEntry(Cursor cursor) {
        /*
        String timestamp = cursor.getString(cursor.getColumnIndex(DATETIME));
        String tag = cursor.getString(cursor.getColumnIndex(ROUTE_ID));
        String sType = cursor.getString(cursor.getColumnIndex(TOTALTIME));
        */
        ScheduleEntry entry = new ScheduleEntry();
        //entry.setTimestamp(timestamp);

        return entry;
    }

    private static Float getNullableFloat(Cursor cursor, String colName) {
        Float retValue = null;
        int colIndex = cursor.getColumnIndexOrThrow(colName);
        if (!cursor.isNull(colIndex)) {
            retValue = cursor.getFloat(colIndex);
        }
        return retValue;
    }
}