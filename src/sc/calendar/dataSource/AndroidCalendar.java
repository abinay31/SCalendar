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

import java.io.Serializable;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

public class AndroidCalendar implements Serializable {
	private static final long serialVersionUID = 1L;
	private final String id;

	private final Map<String, CalendarEvent> events = Maps.newHashMap();

	public AndroidCalendar(String id, Set<CalendarEvent> events) {
		super();
		this.id = id;
		for (CalendarEvent e : events) {
			this.events.put(e.getId(), e);
		}
	}

	public AndroidCalendar(String calId) {
		super();
		this.id = calId;
	}

	public String getId() {
		return id;
	}

	public Set<CalendarEvent> getEvents() {
		return Sets.newTreeSet(events.values());
	}

	public Map<String, CalendarEvent> getEventsMap() {
		return events;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AndroidCalendar other = (AndroidCalendar) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		return true;
	}

}