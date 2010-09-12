/*
*    Copyright (C) 2010  Florian Falkner - ICal Synch for Android Smartphones
*
*    This program is free software: you can redistribute it and/or modify
*    it under the terms of the GNU General Public License as published by
*    the Free Software Foundation, either version 3 of the License, or
*    (at your option) any later version.
*
*    This program is distributed in the hope that it will be useful,
*    but WITHOUT ANY WARRANTY; without even the implied warranty of
*    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
*    GNU General Public License for more details.
*
*    You should have received a copy of the GNU General Public License
*    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*
*/
package at.general.solutions.android.ical.activity;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.util.EntityUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;
import at.general.solutions.android.ical.calendar.UpdateCalendarThread;
import at.general.solutions.android.ical.model.ICalEvent;
import at.general.solutions.android.ical.parser.ICalParserThread;
import at.general.solutions.android.ical.parser.ICalTag;
import at.general.solutions.android.ical.remote.HttpDownloadThread;
import at.general.solutions.android.ical.remote.HttpDownloadProgressListener;
import at.general.solutions.android.ical.utility.ProgressHandler;

public class ICalSynchronizeActivity extends Activity {
	private static final String LOG_TAG = "ICalSynchronizeActivity";

	/** cached version of getResources() webdav xml GET request */
    private static StringEntity GET_RESOURCES = null;
        
    static final int DOWNLOAD_PROGRESS_DIALOG = 0;
    static final int REMOVE_CALENDAR_PROGRESS_DIALOG = 1;
    
    private ArrayAdapter<ICalEvent> eventListAdapter;
    private List<ICalEvent> parsedEvents;
    private String icalFileContent;
    
	
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.config, menu);
        return true;
    }
    
	// This method is called once the menu is selected
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		// We have only one menu option
		case R.id.configMenuItem:
			// Launch Preference activity
			Intent i = new Intent(ICalSynchronizeActivity.this, ICalPreferencesActicity.class);
			startActivity(i);
			break;

		}
		return true;
	}
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        Button syncBtn = (Button) findViewById(R.id.connectToServer);
        eventListAdapter = new ArrayAdapter<ICalEvent>(this, android.R.layout.simple_list_item_1);
        parsedEvents = new ArrayList<ICalEvent>();
        
        ((ListView)findViewById(R.id.list)).setAdapter(eventListAdapter);
        
        syncBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Log.e(LOG_TAG,"Starting event processing");
				showDialog(DOWNLOAD_PROGRESS_DIALOG);		
				
				/*UpdateCalendarThread uct = new UpdateCalendarThread(ICalSynchronizeActivity.this, "nu", null);
				uct.deleteCalendar(3);
				uct.deleteCalendar(4);
				uct.deleteCalendar(5);*/
			}
        });
        
        Button removeBtn = (Button) findViewById(R.id.removeCalendar);
        removeBtn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				
				new AlertDialog.Builder(ICalSynchronizeActivity.this)
	            .setIcon(android.R.drawable.ic_dialog_alert)
	            .setTitle(getString(R.string.confirmRemoval))
	            .setMessage(getString(R.string.confirmRemovalText))
	            .setPositiveButton(getString(R.string.yes), new DialogInterface.OnClickListener() {
	                @Override
	                public void onClick(DialogInterface dialog, int which) {
	                	showDialog(REMOVE_CALENDAR_PROGRESS_DIALOG);
	                }

	            })
	            .setNegativeButton(getString(R.string.no), null)
	            .show();
			}
		});
        
        if (!checkPreferences()) {
        	Toast.makeText(this, getString(R.string.preferencesIncomplete), Toast.LENGTH_LONG).show();
        }
    }
            
    protected Dialog createSettingsIncompleteDialog(final int dialogId) {
    	AlertDialog.Builder dlgBuilder = new AlertDialog.Builder(this)
		.setTitle(getString(R.string.anErrorOccured)).setMessage(getString(R.string.preferencesIncomplete))
		.setCancelable(false)
		.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
				removeDialog(dialogId);
			}
		});
    	return dlgBuilder.create();
    }
    
    protected Dialog onCreateDialog(int id) {
    	switch(id) {
	        case DOWNLOAD_PROGRESS_DIALOG:
	        	return initDownload();
	        case REMOVE_CALENDAR_PROGRESS_DIALOG:
	        	return removeCalendar();
	        default:
	            return null;
        }
    }

	private Dialog removeCalendar() {
		Log.d(LOG_TAG, "Removing calendar from phone");
		
		UpdateCalendarThread remover = new UpdateCalendarThread(ICalSynchronizeActivity.this, null, null);
		remover.setRemoveCalendar(true);
		
		ProgressHandler removerHandler = new ProgressHandler(remover, this) {
			@Override
			public void onError(String infoText) {
				removeDialog(REMOVE_CALENDAR_PROGRESS_DIALOG);
			}
			@Override
			public void onFinish(String infoText) {
				eventListAdapter.clear();
				removeDialog(REMOVE_CALENDAR_PROGRESS_DIALOG);
				Toast.makeText(ICalSynchronizeActivity.this, getString(R.string.calendarWasRemoved), Toast.LENGTH_LONG).show();
			}
		};
		removerHandler.startWorkerThread();
		return removerHandler.getProgressDialog();
	}
    
    private Dialog initDownload() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		
		HttpDownloadThread downloader;

		String remoteUrl = prefs.getString(getString(R.string.remoteAddressKey), null);
		String fileEncoding = prefs.getString(getString(R.string.icalFileEncodingKey), getString(R.string.icalFileEncodingDefault));	        	
		
		if (!checkPreferences()) {
			return createSettingsIncompleteDialog(DOWNLOAD_PROGRESS_DIALOG);
		}
		
		if (prefs.getBoolean(getString(R.string.useHttpAuthenticationKey), false)) {
			String username = prefs.getString(getString(R.string.usernameKey), null);
			String password = prefs.getString(getString(R.string.passwordKey), null);
			
			if (username == null || password == null) {
				return createSettingsIncompleteDialog(DOWNLOAD_PROGRESS_DIALOG);
			}
			
			downloader = new HttpDownloadThread(remoteUrl, username, password, fileEncoding);
		}
		else {
			downloader = new HttpDownloadThread(remoteUrl, fileEncoding);
		}
		
		ProgressHandler downloadHandler = new ProgressHandler(downloader, this) {
			
			@Override
			public void onFinish(String infoText) {
				Log.i(LOG_TAG, "Finished downloading, got result of size: " + infoText.length());
				icalFileContent = infoText;
				
				initParse(getProgressDialog());
			}
			
			@Override
			public void onError(String infoText) {
				Log.e(LOG_TAG, "Downloading failed because of: " + infoText);	
				removeDialog(DOWNLOAD_PROGRESS_DIALOG);
			}
		};
		downloadHandler.startWorkerThread();
		return downloadHandler.getProgressDialog();
	}

	private void initParse(final ProgressDialog dialog) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		
		String userTimeZone = prefs.getString(getString(R.string.localTimeZoneKey), getString(R.string.localTimeZoneDefault));
		String icalTimeZone = prefs.getString(getString(R.string.icalTimeZoneKey), getString(R.string.icalTimeZoneDefault));
		
		eventListAdapter.clear();
		eventListAdapter.setNotifyOnChange(false);
		parsedEvents.clear();
		
		ICalParserThread parser = new ICalParserThread(icalTimeZone, userTimeZone, icalFileContent);
		ProgressHandler parserHandler = new ProgressHandler(parser, dialog) {
			
			@Override
			public void onFinish(String infoText) {
				Log.i(LOG_TAG, "Finished parsing" + infoText);
				
				eventListAdapter.sort(new Comparator<ICalEvent>() {
					@Override
					public int compare(ICalEvent object1, ICalEvent object2) {
						return object1.compareTo(object2);
					}
				});
								
				eventListAdapter.notifyDataSetChanged();
				initCalendarUpdate(dialog);
			}
			
			@Override
			public void onError(String infoText) {
				Log.e(LOG_TAG, "Parsing failed because of: " + infoText);	
				removeDialog(DOWNLOAD_PROGRESS_DIALOG);
			}
			
			@Override
			public void onProgress(Serializable object) {
				eventListAdapter.add((ICalEvent) object);
				parsedEvents.add((ICalEvent) object);
			}
		};
		
		parserHandler.startWorkerThread();
	}
	
	private void initCalendarUpdate(ProgressDialog dialog) {
		Log.d(LOG_TAG, "Now we're going to update the local calendar...");
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
		
		String calendarName = prefs.getString(getString(R.string.calendarNameKey), getString(R.string.calendarNameDefault));
		
		UpdateCalendarThread updater = new UpdateCalendarThread(ICalSynchronizeActivity.this, calendarName, parsedEvents);
		ProgressHandler updaterHandler = new ProgressHandler(updater, dialog) {
			@Override
			public void onError(String infoText) {
				removeDialog(DOWNLOAD_PROGRESS_DIALOG);
			}
			@Override
			public void onFinish(String infoText) {
				removeDialog(DOWNLOAD_PROGRESS_DIALOG);
				Toast.makeText(ICalSynchronizeActivity.this, getString(R.string.synchronizationComplete), Toast.LENGTH_LONG).show();				
			}
		};
		
		Log.d(LOG_TAG, "Starting the update worker thread...");
		updaterHandler.startWorkerThread();
	}
	
	private boolean checkPreferences() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        String remoteUrl = prefs.getString(getString(R.string.remoteAddressKey), null);
        
        if (remoteUrl == null || (remoteUrl.indexOf("http://") == -1 && remoteUrl.indexOf("https://") == -1)) {
        	return false;
        }
        
        return true;
	}
}