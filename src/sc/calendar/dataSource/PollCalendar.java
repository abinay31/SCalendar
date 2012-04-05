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

package sc.calendar.dataSource;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;
import java.util.Set;

import sc.calendar.SCalConstants;
import sc.calendar.SCalendarActivity.AuthTokenRenewer;
import sc.calendar.dataSource.googleApi.CalendarAuthTokenResolver;
import sc.calendar.db.DbInterface;
import android.os.AsyncTask;
import android.util.Log;

import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.CalendarList;
import com.google.api.services.calendar.model.CalendarListEntry;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;
import com.google.common.collect.Sets;

public class PollCalendar extends AsyncTask<String, Integer, Set<AndroidCalendar>> {
	private final AuthTokenRenewer atr;
	
	private Calendar client;
	private final String authToken;
	private final DbInterface db;
	

	public PollCalendar(String authToken, DbInterface db, AuthTokenRenewer atr) {
		this.authToken = authToken;
		this.db = db;
		this.atr = atr;
	}

	@Override
	protected Set<AndroidCalendar> doInBackground(String... params) {
		client = CalendarAuthTokenResolver.getClient(authToken);

		Set<AndroidCalendar> results = Sets.newHashSet();
		CalendarList feed;
		try {
			feed = client.calendarList().list().execute();
			if (feed.getItems() != null) {
				for (CalendarListEntry calendar : feed.getItems()) {
					Set<CalendarEvent> e = getEvents(calendar.getId());
					AndroidCalendar ac = new AndroidCalendar(calendar.getId(), e);
					results.add(ac);
					db.syncDb(ac);
				}
			}
		} catch (IOException e) {
			Log.e(this.getClass().getName(), e.getMessage(), e);
			//auth failed, request new token
			atr.renewAuthToken();
		}

		return results;
	}

	Set<CalendarEvent> getEvents(String id) {

		Set<CalendarEvent> eventsOut = Sets.newTreeSet();
		try {
			com.google.api.services.calendar.Calendar.Events.List query = client.events().list(id);

			String date = SCalConstants.rfc3339.format(new Date(System.currentTimeMillis() - SCalConstants.BUFFER_PERIOD_MS));
			query.setTimeMin(date);
			Events events = query.execute();

			if (events.getItems() != null) {
				for (Event e : events.getItems()) {

					DateTime start = e.getStart().getDateTime();
					if (start == null)
						try {
							start = new DateTime(SCalConstants.simpleDateFormat.parse(e.getStart().getDate()));
						} catch (ParseException e1) {
							Log.e(this.getClass().getName(), e1.getMessage(), e1);
						}

					DateTime end = e.getEnd().getDateTime();
					if (end == null)
						try {
							end = new DateTime(SCalConstants.simpleDateFormat.parse(e.getEnd().getDate()));
						} catch (ParseException e1) {
							Log.e(this.getClass().getName(), e1.getMessage(), e1);
						}

					CalendarEvent ce = new CalendarEvent(start.getValue(), end.getValue(), e.getSummary(), e.getId(), false, id);

					eventsOut.add(ce);
				}
			}
		} catch (IOException e) {
			Log.e(this.getClass().getName(), e.getMessage(), e);
		}
		return eventsOut;
	}
}