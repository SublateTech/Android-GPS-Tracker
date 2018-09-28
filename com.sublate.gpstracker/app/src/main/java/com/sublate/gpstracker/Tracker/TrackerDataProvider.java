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

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.util.Log;

/**
 * Content provider for location tracking.
 *
 * It is recommended to use the TrackerDataHelper class to access this data
 * rather than this class directly
 */
public class TrackerDataProvider extends ContentProvider {

    private static final String AUTHORITY = "com.sublate.gpstracker.Tracker.TrackerDataProvider";

    private static final String DB_NAME = "/sdcard/tracking.db";
    private static final String TRACKING_TABLE_NAME = "tracking";
    private static final String SCHEDULE_TABLE_NAME = "schedule";
    private static final String ROUTE_TABLE_NAME = "route";
    private static final String VISIT_TABLE_NAME = "visit";


    public static final Uri CONTENT_URI_TRACKING = Uri.parse("content://" + AUTHORITY + "/" + TRACKING_TABLE_NAME);
    public static final Uri CONTENT_URI_SCHEDULE = Uri.parse("content://" + AUTHORITY + "/" + SCHEDULE_TABLE_NAME);
    public static final Uri CONTENT_URI_ROUTE = Uri.parse("content://" + AUTHORITY + "/" + ROUTE_TABLE_NAME);
    public static final Uri CONTENT_URI_VISIT = Uri.parse("content://" + AUTHORITY + "/" + VISIT_TABLE_NAME);



    // ------- setup UriMatcher
    private static final int TRACKING = 10;
    private static final int TRACKING_ID = 20;
    private static final int SCHEDULE = 30;
    private static final int SCHEDULE_ID = 40;
    private static final int ROUTE = 50;
    private static final int ROUTE_ID = 60;
    private static final int VISIT = 70;
    private static final int VISIT_ID = 80;



    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        sURIMatcher.addURI(AUTHORITY, TRACKING_TABLE_NAME, TRACKING);
        sURIMatcher.addURI(AUTHORITY, TRACKING_TABLE_NAME + "/#", TRACKING_ID);

        sURIMatcher.addURI(AUTHORITY, SCHEDULE_TABLE_NAME, SCHEDULE);
        sURIMatcher.addURI(AUTHORITY, SCHEDULE_TABLE_NAME + "/#", SCHEDULE_ID);

        sURIMatcher.addURI(AUTHORITY, ROUTE_TABLE_NAME, ROUTE);
        sURIMatcher.addURI(AUTHORITY, ROUTE_TABLE_NAME + "/#", ROUTE_ID);

        sURIMatcher.addURI(AUTHORITY, VISIT_TABLE_NAME, VISIT);
        sURIMatcher.addURI(AUTHORITY, VISIT_TABLE_NAME + "/#", VISIT_ID);
    }

    private static final int DB_VERSION = 1;

    private static final String LOG_TAG = "TrackerDataProvider";

    private DatabaseHelper mOpenHelper;

    @Override
    public boolean onCreate() {
        mOpenHelper = new DatabaseHelper(getContext());
        return true;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int result = db.delete(TRACKING_TABLE_NAME, selection, selectionArgs);
        getContext().getContentResolver().notifyChange(uri, null);
        return result;
    }

    @Override
    public String getType(Uri uri) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        long rowId = 0;

        int uriType = sURIMatcher.match(uri);
        switch (uriType) {
            case TRACKING:
                rowId = db.insert(TRACKING_TABLE_NAME, null, values);
                if (rowId > 0) {
                    Uri addedUri = ContentUris.withAppendedId(CONTENT_URI_TRACKING, rowId);
                    getContext().getContentResolver().notifyChange(addedUri, null);
                    return addedUri;
                }
                break;
            case ROUTE:
                rowId = db.insert(ROUTE_TABLE_NAME, null, values);
                if (rowId > 0) {
                    Uri addedUri = ContentUris.withAppendedId(CONTENT_URI_ROUTE, rowId);
                    getContext().getContentResolver().notifyChange(addedUri, null);
                    return addedUri;
                }
                break;
            case SCHEDULE:
                rowId = db.insert(SCHEDULE_TABLE_NAME, null, values);
                if (rowId > 0) {
                    Uri addedUri = ContentUris.withAppendedId(CONTENT_URI_SCHEDULE, rowId);
                    getContext().getContentResolver().notifyChange(addedUri, null);
                    return addedUri;
                }
                break;
            case VISIT:
                rowId = db.insert(VISIT_TABLE_NAME, null, values);
                if (rowId > 0) {
                    Uri addedUri = ContentUris.withAppendedId(CONTENT_URI_VISIT, rowId);
                    getContext().getContentResolver().notifyChange(addedUri, null);
                    return addedUri;
                }
                break;

            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }


        return null;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();

        // Using SQLiteQueryBuilder instead of query() method
        SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();

        Cursor cursor = null;

        int uriType = sURIMatcher.match(uri);
        switch (uriType) {
            case TRACKING_ID:
                // Adding the ID to the original query
                queryBuilder.appendWhere("Id" + "=" + uri.getLastPathSegment());
                //$FALL-THROUGH$
            case TRACKING:
                queryBuilder.setTables(TRACKING_TABLE_NAME);
                break;
            case ROUTE_ID:
                // Adding the ID to the original query
                queryBuilder.appendWhere("Id" + "=" + uri.getLastPathSegment());
                //$FALL-THROUGH$
            case ROUTE:
                queryBuilder.setTables(ROUTE_TABLE_NAME);
                break;
            case SCHEDULE_ID:
                // Adding the ID to the original query
                queryBuilder.appendWhere("Id" + "=" + uri.getLastPathSegment());
                //$FALL-THROUGH$
            case SCHEDULE:
                queryBuilder.setTables(SCHEDULE_TABLE_NAME);
                break;
            case VISIT_ID:
                // Adding the ID to the original query
                queryBuilder.appendWhere("Id" + "=" + uri.getLastPathSegment());
                //$FALL-THROUGH$
            case VISIT:
                queryBuilder.setTables(VISIT_TABLE_NAME);
                break;

            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        /*
        // TODO: extract limit from URI ?
        cursor = db.query(TRACKING_TABLE_NAME, projection, selection,
                selectionArgs, null, null, sortOrder);
        getContext().getContentResolver().notifyChange(uri, null);
        */

        cursor = queryBuilder.query(db, projection, selection, selectionArgs, null, null,sortOrder);
        return cursor;

    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    /**
     * This class helps open, create, and upgrade the database file.
     */
    private static class DatabaseHelper extends SQLiteOpenHelper {

        DatabaseHelper(Context context) {
            super(context, DB_NAME, null, DB_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            StringBuilder queryBuilder = new StringBuilder();
            queryBuilder.append(String.format("CREATE TABLE %s (", TRACKING_TABLE_NAME));
            TrackerEntry.buildCreationString(queryBuilder);
            queryBuilder.append(");");
            db.execSQL(queryBuilder.toString());
            db.execSQL(
                    "CREATE TABLE route (id INTEGER PRIMARY KEY AUTOINCREMENT , timeStart INTEGER, distance NUMBER, name String, minHeight NUMBER DEFAULT -1, maxHeight NUMBER DEFAULT -1, timeEnd INTEGER, pointCount INTEGER)");
            db.execSQL(
                    "CREATE TABLE schedule (id INTEGER PRIMARY KEY AUTOINCREMENT , name String, timeStart String, timeEnd String)");
            db.execSQL(
                    "INSERT INTO schedule (name, timeStart, timeEnd) " +
                            "VALUES ('Horario Dia','08:30', '12:00')");
            db.execSQL(
                    "INSERT INTO schedule (name, timeStart, timeEnd) " +
                            "VALUES ('Horario Tarde','13:00', '18:00')");
            db.execSQL(
                    "CREATE TABLE visit (id INTEGER PRIMARY KEY AUTOINCREMENT , id_route INTEGER, time INTEGER, timeStart INTEGER,timeEnd INTEGER)");
            db.setVersion(DB_VERSION);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // TODO: reimplement this when dB version changes
            Log.w(LOG_TAG, "Upgrading database from version " + oldVersion
                    + " to " + newVersion
                    + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + TRACKING_TABLE_NAME);
            onCreate(db);
        }
    }
}