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

import java.text.SimpleDateFormat;
import java.util.logging.Level;

public class SCalConstants {

	// simple date format, suitable for decoding dates from google calendar
	public static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");

	// date formatter suitable for encoding a date and time for use in google
	// calendar query
	public static final SimpleDateFormat rfc3339 = new SimpleDateFormat("yyyy-MM-dd'T'h:m:ss.SZ");

	// How far into the past are events requested from google calendar
	// i.e events more than 1 week old will not be checked
	public static final int BUFFER_PERIOD_MS = 1000 * 60 * 60 * 24 * 7;

	// the period with which the calender updates
	static final int CAL_SYNC_PERIOD_MS = 30000;

	// Goole api key
	public static final String KEY = "INSERT APP KEY HERE";
	
	/** Logging level for HTTP requests/responses. */
	static final Level LOGGING_LEVEL = Level.OFF;
}
