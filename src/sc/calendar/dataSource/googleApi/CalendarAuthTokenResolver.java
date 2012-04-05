/*
 * Copyright (c) 2010 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package sc.calendar.dataSource.googleApi;

import java.io.IOException;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import sc.calendar.SCalConstants;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import com.google.api.client.extensions.android2.AndroidHttp;
import com.google.api.client.googleapis.GoogleHeaders;
import com.google.api.client.googleapis.extensions.android2.auth.GoogleAccountManager;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.json.JsonHttpRequest;
import com.google.api.client.http.json.JsonHttpRequestInitializer;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarRequest;

/**
 * Heavily based on CalendarSample.java sample code by Yaniv Inbar, under Apache
 * 2 license
 */

public class CalendarAuthTokenResolver extends Activity {

	private static final String TAG = "CalendarInterface";

	private static final String AUTH_TOKEN_TYPE = "cl";

	private static final int REQUEST_AUTHENTICATE = 0;

	private static final String PREF_ACCOUNT_NAME = "accountName";

	private static final String PREF_AUTH_TOKEN = "authToken";

	public static final String CALENDAR_RESULTS = "CALENDAR_RESULTS";

	private GoogleAccountManager accountManager;

	private SharedPreferences settings;

	private String accountName;

	private String authToken;

	public static com.google.api.services.calendar.Calendar getClient(final String authToken) {

		HttpTransport transport = AndroidHttp.newCompatibleTransport();

		JsonFactory jsonFactory = new JacksonFactory();

		return com.google.api.services.calendar.Calendar.builder(transport, jsonFactory).setApplicationName("SCalendar")
				.setHttpRequestInitializer(new HttpRequestInitializer() {
					public void initialize(HttpRequest request) throws IOException {
						request.getHeaders().setAuthorization(GoogleHeaders.getGoogleLoginValue(authToken));
					}
				}).setJsonHttpRequestInitializer(new JsonHttpRequestInitializer() {

					public void initialize(JsonHttpRequest request) throws IOException {
						CalendarRequest calendarRequest = (CalendarRequest) request;
						calendarRequest.setKey(SCalConstants.KEY);
					}
				}).build();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		// DO NOT SHIP THIS!!!
		//trustAll();

		settings = getPreferences(MODE_PRIVATE);
		accountName = settings.getString(PREF_ACCOUNT_NAME, null);
		authToken = settings.getString(PREF_AUTH_TOKEN, null);
		accountManager = new GoogleAccountManager(this);
		//accountManager.invalidateAuthToken(authToken);
		//authToken= null;
		gotAccount();
	}

	void gotAccount() {
		Account account = accountManager.getAccountByName(accountName);
		if (account == null) {
			chooseAccount();
			return;
		}
		if (authToken != null) {
			onAuthToken();
			return;
		}
		accountManager.manager.getAuthToken(account, AUTH_TOKEN_TYPE, true, new AccountManagerCallback<Bundle>() {

			public void run(AccountManagerFuture<Bundle> future) {
				try {
					Bundle bundle = future.getResult();
					if (bundle.containsKey(AccountManager.KEY_INTENT)) {
						Intent intent = bundle.getParcelable(AccountManager.KEY_INTENT);
						intent.setFlags(intent.getFlags() & ~Intent.FLAG_ACTIVITY_NEW_TASK);
						startActivityForResult(intent, REQUEST_AUTHENTICATE);
					} else if (bundle.containsKey(AccountManager.KEY_AUTHTOKEN)) {
						setAuthToken(bundle.getString(AccountManager.KEY_AUTHTOKEN));
						onAuthToken();
					}
				} catch (Exception e) {
					Log.e(TAG, e.getMessage(), e);
				}
			}
		}, null);
	}

	private void chooseAccount() {
		accountManager.manager.getAuthTokenByFeatures(GoogleAccountManager.ACCOUNT_TYPE, AUTH_TOKEN_TYPE, null, CalendarAuthTokenResolver.this, null, null,
				new AccountManagerCallback<Bundle>() {

					public void run(AccountManagerFuture<Bundle> future) {
						Bundle bundle;
						try {
							bundle = future.getResult();
							setAccountName(bundle.getString(AccountManager.KEY_ACCOUNT_NAME));
							setAuthToken(bundle.getString(AccountManager.KEY_AUTHTOKEN));
							onAuthToken();
						} catch (OperationCanceledException e) {
							// user canceled
							Log.i(TAG, e.getMessage(), e);
						} catch (AuthenticatorException e) {
							Log.e(TAG, e.getMessage(), e);
						} catch (IOException e) {
							Log.e(TAG, e.getMessage(), e);
						}
					}
				}, null);
	}

	void setAccountName(String accountName) {
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(PREF_ACCOUNT_NAME, accountName);
		editor.commit();
		this.accountName = accountName;
	}

	void setAuthToken(String authToken) {
		SharedPreferences.Editor editor = settings.edit();
		editor.putString(PREF_AUTH_TOKEN, authToken);
		editor.commit();
		this.authToken = authToken;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		switch (requestCode) {
		case REQUEST_AUTHENTICATE:
			if (resultCode == RESULT_OK) {
				gotAccount();
			} else {
				chooseAccount();
			}
			break;
		}
	}

	void onAuthToken() {
		
		//test connection
		Calendar client = CalendarAuthTokenResolver.getClient(authToken);
		try {
			client.calendarList().list().execute();
		}			
		catch (IOException e) {
			handleGoogleException(e);
			return;
		}
		
		Intent intent = new Intent();
		intent.putExtra(CALENDAR_RESULTS, new CalendarConnectionData(accountName, authToken));
		setResult(RESULT_OK, intent);
		finish();

	}

	void trustAll() {
		// Create a trust manager that does not validate certificate chains
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
			}

			public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
			}
		} };

		// Install the all-trusting trust manager
		try {
			SSLContext sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		} catch (Exception e) {
			Log.e(TAG, e.getMessage(), e);
		}
	}

	void handleGoogleException(IOException e) {
		if (e instanceof GoogleJsonResponseException) {
			GoogleJsonResponseException exception = (GoogleJsonResponseException) e;

			// TODO(yanivi): should only try this once to avoid infinite loop
			if (exception.getStatusCode() == 401) {
				accountManager.invalidateAuthToken(authToken);
				authToken = null;
				SharedPreferences.Editor editor2 = settings.edit();
				editor2.remove(PREF_AUTH_TOKEN);
				editor2.commit();
				gotAccount();
				return;
			}
		}
		Log.e(TAG, e.getMessage(), e);
	}

}
