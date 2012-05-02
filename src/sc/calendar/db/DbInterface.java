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

import java.util.Set;
import java.util.SortedSet;

import sc.calendar.dataSource.AndroidCalendar;
import sc.calendar.dataSource.CalendarEvent;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import com.google.common.collect.Sets;

public class DbInterface {

	private static final String ADD_CAL = "INSERT INTO " + DbConnection.CALENDAR_TABLE_NAME + " ( " + DbConnection.ID + ") VALUES (?);";

	private static final String ADD_EVENT = "INSERT INTO "  + DbConnection.EVENTS_TABLE_NAME + " ( " 
															+ DbConnection.ID + ", " 
															+ DbConnection.EVENTS_START_NAME + ", "
															+ DbConnection.EVENTS_END_NAME + ", " 
															+ DbConnection.EVENTS_CLEARED_NAME + ", "
															+ DbConnection.EVENTS_NAME_NAME + ", " 
															+ DbConnection.CALENDER_FK 
															+ " ) VALUES (?,?,?,?,?,?);";
	
	private static final String UPDATE_EVENT = "UPDATE " + DbConnection.EVENTS_TABLE_NAME + " SET  " 
																+ DbConnection.ID + " = ?, " 
																+ DbConnection.EVENTS_START_NAME + " = ?, "
																+ DbConnection.EVENTS_END_NAME + " = ?, " 
																+ DbConnection.EVENTS_CLEARED_NAME + " = ?, "
																+ DbConnection.EVENTS_NAME_NAME + " = ?, " 
																+ DbConnection.CALENDER_FK + " = ? "
																+ " WHERE "+ DbConnection.ID+ "= ?";
	
	private static final String REMOVE_ALL_EVENTS = "DELETE FROM "+DbConnection.EVENTS_TABLE_NAME+" WHERE "+DbConnection.CALENDER_FK+ " = ? AND ("+DbConnection.EVENTS_CLEARED_NAME+" = 1 OR "+DbConnection.EVENTS_START_NAME+" > ?) ";

	private static final String GET_EVENTS_FOR_CAL = "SELECT * FROM " + DbConnection.EVENTS_TABLE_NAME +" WHERE "+DbConnection.CALENDER_FK+" = ?";
	
	private final DbConnection db;

	public DbInterface(Context context) {
		db = new DbConnection(context);
	}

	public Set<AndroidCalendar> getCalenders() {
		SQLiteDatabase con = db.getReadableDatabase();
		Set<AndroidCalendar> out = Sets.newHashSet();
		Cursor cursor = null;
		try {
			cursor = con.query(DbConnection.CALENDAR_TABLE_NAME, new String[] { DbConnection.ID }, null, null, null, null, null);
			if (cursor.moveToFirst()) {
				int idIndex = cursor.getColumnIndex(DbConnection.ID);
				do {
					String id = cursor.getString(idIndex);
					SortedSet<CalendarEvent> events = getEvents(id);
					out.add(new AndroidCalendar(id, events));
				} while (cursor.moveToNext());
			}
			return out;
		} finally {
			if (cursor != null)
				cursor.close();
			con.close();
		}
	}

	public AndroidCalendar getCalender(String calId) {

		SQLiteDatabase con = db.getReadableDatabase();
		Cursor cursor = null;
		try {
			cursor = con.query(DbConnection.CALENDAR_TABLE_NAME, new String[] { DbConnection.ID }, DbConnection.ID + " = ?", new String[] { calId }, null, null,
					null);
			if (cursor.moveToFirst()) {
				// cal does exist
				int idIndex = cursor.getColumnIndex(DbConnection.ID);
				String id = cursor.getString(idIndex);
				SortedSet<CalendarEvent> events = getEvents(id);
				return new AndroidCalendar(id, events);
			}
		} finally {
			if (cursor != null)
				cursor.close();
			con.close();
		}
		return null;
	}

	public SortedSet<CalendarEvent> getEvents(String calId) {
		SQLiteDatabase con = db.getReadableDatabase();
		Cursor cursor = null;
		try {
			cursor = con.rawQuery(GET_EVENTS_FOR_CAL, new String[] { calId });

			SortedSet<CalendarEvent> out = Sets.newTreeSet();
			if (cursor.moveToFirst()) {
				int startIndex = cursor.getColumnIndex(DbConnection.EVENTS_START_NAME);
				int endIndex = cursor.getColumnIndex(DbConnection.EVENTS_END_NAME);
				int idIndex = cursor.getColumnIndex(DbConnection.ID);
				int clearedIndex = cursor.getColumnIndex(DbConnection.EVENTS_CLEARED_NAME);
				int nameIndex = cursor.getColumnIndex(DbConnection.EVENTS_NAME_NAME);
				int calFkIndex = cursor.getColumnIndex(DbConnection.CALENDER_FK);

				do {
					CalendarEvent ce = new CalendarEvent(cursor.getLong(startIndex), cursor.getLong(endIndex), cursor.getString(nameIndex),
							cursor.getString(idIndex), cursor.getLong(clearedIndex) != 0, cursor.getString(calFkIndex));
					out.add(ce);
				} while (cursor.moveToNext());

			}
			return out;
		} finally {
			if (cursor != null)
				cursor.close();
			con.close();
		}
	}

	public AndroidCalendar getOrCreateCalendar(String calId) {
		AndroidCalendar cal = getCalender(calId);
		if (cal==null) {
			SQLiteDatabase con = db.getWritableDatabase();
			try {
				SQLiteStatement stmt = con.compileStatement(ADD_CAL);
				try {
					stmt.bindString(1, calId);
					stmt.executeInsert();
				} finally {
					stmt.close();
				}
			} catch (Exception e) {
				Log.e(this.getClass().getName(), e.getMessage(), e);
			} finally {
				con.close();
			}
			return new AndroidCalendar(calId);
		}
		else {
			return cal;
		}
	}

	private void removeOldEvents(AndroidCalendar cal) {
		SQLiteDatabase con = db.getWritableDatabase();

		try {
			// Unfortunately will need to build this one
			String stmt = REMOVE_ALL_EVENTS;

			if (cal.getEvents().size() > 0) {
				stmt += " AND " + DbConnection.ID + " NOT IN ";

				String in = null;
				for (CalendarEvent ce : cal.getEvents()) {
					if (in == null) {
						in = "('" + ce.getId() + "'";
					} else {
						in += ",'" + ce.getId() + "'";
					}
				}
				in += ")";

				stmt += in;
			}

			SQLiteStatement stmtE = con.compileStatement(stmt);
			try {
				stmtE.bindString(1, cal.getId());
				stmtE.bindLong(2, System.currentTimeMillis());
				stmtE.execute();
			} finally {
				stmtE.close();
			}
		} finally {
			con.close();
		}
	}
	
	private void addEvent(CalendarEvent ce, String calId, SQLiteDatabase con) {
		SQLiteStatement stmtE = con.compileStatement(ADD_EVENT);
		try {
			stmtE.bindString(1, ce.getId());
			stmtE.bindLong(2, ce.getStart().getTime());
			stmtE.bindLong(3, ce.getEnd().getTime());
			stmtE.bindLong(4, ce.getCleared() ? 1 : 0);
			stmtE.bindString(5, ce.getSummary());
			stmtE.bindString(6, calId);
			stmtE.executeInsert();
		} finally {
			stmtE.close();
		}
	}

	public void syncDb(AndroidCalendar cal) {

		// get old events
		AndroidCalendar oldCal = getOrCreateCalendar(cal.getId());

		// remove old events from db
		removeOldEvents(cal);

		// add new events
		SQLiteDatabase con = db.getWritableDatabase();
		try {
			// merge old events into the new
			for (CalendarEvent ce : cal.getEvents()) {
				CalendarEvent oldEvent = oldCal.getEventsMap().get(ce.getId());
				if (oldEvent != null) {
					// already exists. Merge the events and update the record if
					// something has changed
					if (ce.mergeEvents(oldEvent)) {
						updateEventWithCon(ce, con);
					}
				} else {
					// new event, add it
					addEvent(ce, cal.getId(), con);
				}
			}

		} finally {
			con.close();
		}

	}

	private void updateEventWithCon(CalendarEvent ce, SQLiteDatabase con) {
		con.execSQL(
				UPDATE_EVENT,
				new Object[] { ce.getId(), ce.getStart().getTime(), ce.getEnd().getTime(), ce.getCleared() ? 1 : 0, ce.getSummary(), ce.getCalId(), ce.getId() });

	}

	public void updateEvent(CalendarEvent ce) {
		SQLiteDatabase con = db.getWritableDatabase();
		try {
			updateEventWithCon(ce, con);
		} finally {
			con.close();
		}
	}

}
