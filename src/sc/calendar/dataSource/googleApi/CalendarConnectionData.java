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

package sc.calendar.dataSource.googleApi;

import java.io.Serializable;

public class CalendarConnectionData implements Serializable {
	private static final long serialVersionUID = 1L;
	private final String accountName;
	private final String authToken;

	public CalendarConnectionData(String accountName, String authToken) {
		super();
		this.accountName = accountName;
		this.authToken = authToken;
	}

	public String getAccountName() {
		return accountName;
	}

	public String getAuthToken() {
		return authToken;
	}

}