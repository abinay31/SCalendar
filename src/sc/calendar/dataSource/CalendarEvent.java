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
import java.text.SimpleDateFormat;
import java.util.Date;

public class CalendarEvent implements Comparable<CalendarEvent>, Serializable {
	private static final long serialVersionUID = 1L;
	private final Date startTime;
	private final Date endTime;
	private final String title;
	private final String id;
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm");
	private boolean cleared;
	private final String calId;

	public CalendarEvent(long startTime, long endTime, String title, String id, boolean cleared, String calId) {
		super();
		this.startTime = new Date(startTime);
		this.endTime = new Date(endTime);
		this.title = title;
		this.id = id;
		this.cleared = cleared;
		this.calId = calId;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((calId == null) ? 0 : calId.hashCode());
		result = prime * result + (cleared ? 1231 : 1237);
		result = prime * result + ((endTime == null) ? 0 : endTime.hashCode());
		result = prime * result + ((id == null) ? 0 : id.hashCode());
		result = prime * result + ((startTime == null) ? 0 : startTime.hashCode());
		result = prime * result + ((title == null) ? 0 : title.hashCode());
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
		CalendarEvent other = (CalendarEvent) obj;
		if (calId == null) {
			if (other.calId != null)
				return false;
		} else if (!calId.equals(other.calId))
			return false;
		if (cleared != other.cleared)
			return false;
		if (endTime == null) {
			if (other.endTime != null)
				return false;
		} else if (!endTime.equals(other.endTime))
			return false;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (startTime == null) {
			if (other.startTime != null)
				return false;
		} else if (!startTime.equals(other.startTime))
			return false;
		if (title == null) {
			if (other.title != null)
				return false;
		} else if (!title.equals(other.title))
			return false;
		return true;
	}

	public String getFormattedStartTime() {
		return dateFormat.format(startTime);
	}

	public String getFormattedEndTime() {
		return dateFormat.format(endTime);
	}

	public String getTitle() {
		return title;
	}

	public Date getStart() {
		return startTime;
	}

	@Override
	public int compareTo(CalendarEvent another) {
		long diff = (startTime.getTime() - another.startTime.getTime());
		if (diff < 0)
			return -1;
		if (diff > 0)
			return 1;
		return id.compareTo(another.id);
	}

	public String getId() {
		return id;
	}

	public boolean getCleared() {
		return cleared;
	}

	public Date getEnd() {
		return endTime;
	}

	public String getSummary() {
		return this.title;
	}

	// merges two events and returns true if something has changed
	public boolean mergeEvents(CalendarEvent old) {
		this.cleared = cleared || old.getCleared();
		return !this.equals(old);
	}

	public void setCleared(boolean b) {
		cleared = b;
	}

	public String getCalId() {
		return calId;
	}

}
