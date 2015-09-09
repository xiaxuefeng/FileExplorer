/*
 * Copyright (c) 2010-2011, The MiCode Open Source Community (www.micode.net)
 *
 * This file is part of FileExplorer.
 *
 * FileExplorer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * FileExplorer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with SwiFTP.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.android.fileexplorer;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.mediatek.storage.StorageManagerEx;
public class FavoriteDatabaseHelper extends SQLiteOpenHelper {

    private final static String DATABASE_NAME = "file_explorer";

    private final static int DATABASE_VERSION = 1;

    private final static String TABLE_NAME = "favorite";

    public final static String FIELD_ID = "_id";

    public final static String FIELD_TITLE = "title";

    public final static String FIELD_LOCATION = "location";

    private boolean firstCreate;

    private FavoriteDatabaseListener mListener;

    private static FavoriteDatabaseHelper instance;

    public interface FavoriteDatabaseListener {
        void onFavoriteDatabaseChanged();
    }

    public FavoriteDatabaseHelper(Context context, FavoriteDatabaseListener listener) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        instance = this;
        mListener = listener;
    }

    public static FavoriteDatabaseHelper getInstance() {
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sql = "Create table " + TABLE_NAME + "(" + FIELD_ID + " integer primary key autoincrement,"
                + FIELD_TITLE + " text, " + FIELD_LOCATION + " text );";
        db.execSQL(sql);
        firstCreate = true;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String sql = " DROP TABLE IF EXISTS " + TABLE_NAME;
        db.execSQL(sql);
        onCreate(db);
    }

    public boolean isFirstCreate() {
        return firstCreate;
    }

    public boolean isFavorite(String path) {
        String selection = FIELD_LOCATION + "=?";
        String strs[] = path.split("/");
		String outSD = StorageManagerEx.getExternalStoragePath();
		String inSD = StorageManagerEx.getInternalStoragePath();
		String inSD_end = inSD != null ? inSD.substring(inSD.lastIndexOf("/") + 1, inSD.length()) : null;
		String outSD_end = outSD != null ? outSD.substring(outSD.lastIndexOf("/") + 1, outSD.length()) : null;
		StringBuffer lastPath = new StringBuffer();
		for (int i = 0; i < strs.length; i++) {
			if (i > 1) {
				if(inSD_end != null && strs[i].equals(inSD_end) || strs[i].equals("emulated")) {
					if (lastPath.length() > 0) {
						lastPath.delete(0, lastPath.length());
					}
					lastPath.append("0:");
				}else if (outSD_end != null && strs[i].equals(outSD_end)){
					lastPath.append("1:");
				}else {
					lastPath.append("/" + strs[i]);
				}
			}
		}
        String[] selectionArgs = new String[] {
        		lastPath.toString()
        };
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, null, selection, selectionArgs, null, null, null);
        if (cursor == null)
            return false;
        boolean ret = cursor.getCount() > 0;
        cursor.close();
        return ret;
    }

    public Cursor query() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_NAME, null, null, null, null, null, null);
        return cursor;
    }

    public long insert(String title, String location) {
        //if (isFavorite(location))
        //    return -1;
        SQLiteDatabase db = this.getWritableDatabase();
        long ret = db.insert(TABLE_NAME, null, createValues(title, location));
        mListener.onFavoriteDatabaseChanged();
        return ret;
    }

    public void delete(long id, boolean notify) {
        SQLiteDatabase db = this.getWritableDatabase();
        String where = FIELD_ID + "=?";
        String[] whereValue = {
            Long.toString(id)
        };
        db.delete(TABLE_NAME, where, whereValue);

        if (notify)
            mListener.onFavoriteDatabaseChanged();
    }

    public void delete(String location) {
        SQLiteDatabase db = this.getWritableDatabase();
        String where = FIELD_LOCATION + "=?";
        String[] whereValue = {
            location
        };
        db.delete(TABLE_NAME, where, whereValue);
        mListener.onFavoriteDatabaseChanged();
    }

    public void update(int id, String title, String location) {
        SQLiteDatabase db = this.getWritableDatabase();
        String where = FIELD_ID + "=?";
        String[] whereValue = {
            Integer.toString(id)
        };
        db.update(TABLE_NAME, createValues(title, location), where, whereValue);
        mListener.onFavoriteDatabaseChanged();
    }

    private ContentValues createValues(String title, String location) {
        ContentValues cv = new ContentValues();
        cv.put(FIELD_TITLE, title);
        cv.put(FIELD_LOCATION, location);
        return cv;
    }
}
