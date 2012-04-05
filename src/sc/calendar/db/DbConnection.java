/* 
   Copyright 2012 John Oliver

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package sc.calendar.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

class DbConnection extends SQLiteOpenHelper {

	private static final int DATABASE_VERSION = 5;
	private static final String DATABASE_NAME = "scalendar";
	

	static final String ID = "id";

	static final String CALENDAR_TABLE_NAME = "calenders";
	private static final String CALENDAR_TABLE_CREATE = "CREATE TABLE " + CALENDAR_TABLE_NAME + " (" + ID + " TEXT PRIMARY KEY);";
	
	
	static final String EVENTS_TABLE_NAME = "events";
	static final String EVENTS_START_NAME = "start";
	static final String EVENTS_END_NAME = "end";
	static final String EVENTS_CLEARED_NAME = "cleared";
	static final String EVENTS_NAME_NAME = "name";
	
	static final String CALENDER_FK = "idCal";
	private static final String EVENTS_TABLE_CREATE = "CREATE TABLE " + EVENTS_TABLE_NAME + " (" + 
			ID + " TEXT UNIQUE, " +
			EVENTS_START_NAME + " INTEGER, "+
			EVENTS_END_NAME + " INTEGER, "+
			EVENTS_CLEARED_NAME + " NUMERIC, "+
			EVENTS_NAME_NAME + " TEXT, "+
			CALENDER_FK+ " TEXT, FOREIGN KEY ("+CALENDER_FK+") REFERENCES "+CALENDAR_TABLE_NAME+"( "+ID+" ));";

	
	private static final String REMOVE_EVENTS_TABLE = "DROP TABLE "+EVENTS_TABLE_NAME;
	private static final String REMOVE_CALENDAR_TABLE = "DROP TABLE "+CALENDAR_TABLE_NAME;

	public DbConnection(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL(CALENDAR_TABLE_CREATE);
		db.execSQL(EVENTS_TABLE_CREATE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		if (db.isReadOnly()) {
			db = this.getWritableDatabase();
		}
		try {
			db.execSQL(REMOVE_EVENTS_TABLE);
		} catch (SQLiteException e) {
			Log.e(this.getClass().getName(), e.getMessage(), e);
		}

		try {
			db.execSQL(REMOVE_CALENDAR_TABLE);
		} catch (SQLiteException e) {
			Log.e(this.getClass().getName(), e.getMessage(), e);
		}
		
		onCreate(db);
	}

}