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

package sc.calendar;

import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.logging.Logger;

import sc.calendar.dataSource.AndroidCalendar;
import sc.calendar.dataSource.CalendarEvent;
import sc.calendar.dataSource.PollCalendar;
import sc.calendar.dataSource.googleApi.CalendarAuthTokenResolver;
import sc.calendar.dataSource.googleApi.CalendarConnectionData;
import sc.calendar.db.DbInterface;
import sc.calendar.ui.CalendarEventAdapter;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListView;


/**
 * Main class for the app
 * @author joliver
 *
 */
public class SCalendarActivity extends Activity {

	private static final int CALENDER_INTERFACE_ID = 1;

	private DbInterface db;
	private CalendarEventAdapter m_adapter;

	private final Handler handler = new Handler();

	private TimerTask doAsynchronousTask;
	private AuthTokenRenewer atr;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		Logger.getLogger("sc.calendar").setLevel(SCalConstants.LOGGING_LEVEL);
		
		setContentView(R.layout.main);

		db = new DbInterface(this);
		atr = new AuthTokenRenewer();

		//Create main list view
		ListView lv = ((ListView) this.findViewById(R.id.android_listView));
		m_adapter = new CalendarEventAdapter(this, R.layout.list_item, db);
		lv.setAdapter(m_adapter);

		// do initial display from db
		updateList(db.getCalenders());

		// try to get authtoken
		atr.renewAuthToken();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.menu, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
			case R.id.send_events_to_alerts: {
												createNotifications();
												return true;
											}
			default: return super.onOptionsItemSelected(item);
		}
		
	}
	
	private void createNotifications() {

		String ns = Context.NOTIFICATION_SERVICE;
		NotificationManager mNotificationManager = (NotificationManager) getSystemService(ns);

		mNotificationManager.cancelAll();
		
		int icon = R.drawable.ic_launcher;
		CharSequence tickerText = getText(R.string.new_event);
		long now = System.currentTimeMillis();

		Context context = getApplicationContext();

		Set<AndroidCalendar> cals = db.getCalenders();
		for (AndroidCalendar cal : cals) {
			for (CalendarEvent ce : cal.getEvents()) {
				if (!ce.getCleared() && ce.getStart().getTime() < now) {

					Notification notification = new Notification(icon, tickerText, ce.getStart().getTime());
					notification.flags |= Notification.FLAG_AUTO_CANCEL;
					
					CharSequence contentTitle = getText(R.string.event);
					CharSequence contentText = ce.getSummary();
					Intent notificationIntent = new Intent(this, SCalendarActivity.class);
					PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
					

					notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
					mNotificationManager.notify(ce.getId().hashCode(), notification);
				}
			}
		}
	}

	/** 
	 * Class that can be passed and used to renew the auth token if it is rejected 
	 * @author joliver
	 *
	 */
	public class AuthTokenRenewer {
		public void renewAuthToken() {
			// stop polling until we have a token
			if (doAsynchronousTask != null)
				doAsynchronousTask.cancel();

			// ensure its done on the main thread
			handler.post(new Runnable() {
				public void run() {
					startActivityForResult(new Intent(SCalendarActivity.this, CalendarAuthTokenResolver.class), CALENDER_INTERFACE_ID);
				}
			});

		}
	}

	/**
	 * Updates the main list in the GUI with the events from the calendars.
	 * This should be called from the GUI thread. 
	 * @param calenders
	 */
	private void updateList(final Set<AndroidCalendar> calenders) {
		final long now = System.currentTimeMillis();
		m_adapter.clear();
		for (AndroidCalendar ac : calenders) {
			for (CalendarEvent ce : ac.getEvents()) {
				if (!ce.getCleared() && ce.getStart().getTime() < now) {
					m_adapter.add(ce);
				}
			}
		}
	}

	/**
	 * Creates a timer task that periodically calls the PollCalendar task
	 */
	private void startCalendarPoll(final CalendarConnectionData res) {
		doAsynchronousTask = new TimerTask() {
			@Override
			public void run() {
				//outside the GUI thread
				if (Looper.myLooper() == null) {
					Looper.prepare();
				}

				PollCalendar pc = new PollCalendar(res.getAuthToken(), db, atr);

				try {
					final Set<AndroidCalendar> calenders = pc.execute().get();
					handler.post(new Runnable() {
						public void run() {
							//inside the GUI
							updateList(calenders);
						}
					});
				} catch (InterruptedException e) {
					Log.e(this.getClass().getName(), e.getMessage(), e);
				} catch (ExecutionException e) {
					Log.e(this.getClass().getName(), e.getMessage(), e);
				}
			}
		};
		new Timer().schedule(doAsynchronousTask, 3000, SCalConstants.CAL_SYNC_PERIOD_MS);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		//if we have an auth result, get it and start polling the calender 
		if (requestCode == CALENDER_INTERFACE_ID) {
			CalendarConnectionData res = (CalendarConnectionData) data.getExtras().get(CalendarAuthTokenResolver.CALENDAR_RESULTS);
			startCalendarPoll(res);
		}
	}

}