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

package sc.calendar.ui;

import java.util.Collection;
import java.util.Set;

import sc.calendar.R;
import sc.calendar.dataSource.CalendarEvent;
import sc.calendar.db.DbInterface;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.google.common.collect.Sets;

public class CalendarEventAdapter extends ArrayAdapter<CalendarEvent> {

	private final Set<CalendarEvent> items;
	private final Context context;
	private final DbInterface db;

	public CalendarEventAdapter(Context context,
			int textViewResourceId, DbInterface db) {
		super(context, textViewResourceId);
		this.items = Sets.newTreeSet();
		this.context = context;
		this.db = db;
	}
	
	@Override
	public void add(CalendarEvent object) {
		super.add(object);
		items.add(object);
	}
	
	public void addAll(Collection<CalendarEvent> collection) {
		for(CalendarEvent ce : collection) {
			super.add(ce);
		}
		items.addAll(collection);
	}
	
	@Override
	public void remove(CalendarEvent object) {
		super.remove(object);
		items.remove(object);
		object.setCleared(true);
		db.updateEvent(object);
		
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {

		View v = convertView;
		if (v == null) {
			LayoutInflater vi = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			v = vi.inflate(R.layout.list_item, null);
		}
		final CalendarEvent ce = (CalendarEvent) items.toArray()[position];
		if (ce != null) {
			TextView time = (TextView) v.findViewById(R.id.eventTime);
			TextView title = (TextView) v.findViewById(R.id.eventTitle);
			Button clear = (Button) v.findViewById(R.id.buttonClear);

			if (time != null) {
				time.setText(ce.getFormattedStartTime());
			}
			if (title != null) {
				title.setText(ce.getTitle());
			}
			
			clear.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					CalendarEventAdapter.this.remove(ce);
				}
			});
		}
		return v;
	}
}