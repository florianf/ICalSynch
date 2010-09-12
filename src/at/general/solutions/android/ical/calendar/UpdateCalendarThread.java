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
*    Code below takes some methods and ides from:
*    http://code.google.com/p/android-calendar-provider-tests/
*    
*    See original copright notice below.
*
*/

/*
 * Copyright (c) 2010, Lauren Darcey and Shane Conder
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are 
 * permitted provided that the following conditions are met:
 * 
 * * Redistributions of source code must retain the above copyright notice, this list of 
 *   conditions and the following disclaimer.
 *   
 * * Redistributions in binary form must reproduce the above copyright notice, this list 
 *   of conditions and the following disclaimer in the documentation and/or other 
 *   materials provided with the distribution.
 *   
 * * Neither the name of the <ORGANIZATION> nor the names of its contributors may be used
 *   to endorse or promote products derived from this software without specific prior 
 *   written permission.
 *   
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY 
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES 
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT 
 * SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, 
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED 
 * TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR 
 * BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, 
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF 
 * THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * <ORGANIZATION> = Mamlambo
 */

/*
 * ******* WARNING WARNING WARNING ************
 * 
 * As stated above, this code is supplied AS-IS. Any damage, loss of data, or other harm 
 * is not our liability. You use this code at your own risk.
 * 
 * You've been warned. 
 * 
 * Since this code has to be run on a handset, you may break your handset. We have not tested it on *your* handset.
 * 
 * You've been warned.
 * 
 * This code is subject to change. In fact, it has changed. Android SDK 2.1 -> 2.2 update changed it.
 * 
 * Please see the article at http://bit.ly/c2kYWk for more information. 
 * 
 */



package at.general.solutions.android.ical.calendar;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;
import at.general.solutions.android.ical.activity.R;
import at.general.solutions.android.ical.model.ICalEvent;
import at.general.solutions.android.ical.utility.ProgressThread;

public class UpdateCalendarThread extends ProgressThread {
	public static String LOG_TAG = "UpdateCalendarThread";
	
	private String CALENDAR_NAME = "icalRemoteCalendar";
	
	private Activity activity;
	private String calendarDisplayName;
	private List<ICalEvent> events;
	private boolean removeCalendar = false;
	
	public UpdateCalendarThread(Activity activity, String calendarDisplayName, List<ICalEvent> events) {
		this.activity = activity;
		this.calendarDisplayName = calendarDisplayName;
		this.events = events;
	}
	
	@Override
	public void run() {
		if (removeCalendar) {
			super.sendInitMessage(R.string.removingCalendarEntries);
			removeExistingCalendar();
			super.sendFinishedMessage();
			return;
		}
		
		super.sendInitMessage(R.string.writingCalendarEntries);
		
		int calendarId;
		removeExistingCalendar();
		
		try {
			createNewCalendar(CALENDAR_NAME, calendarDisplayName);	
		}
		catch (IllegalArgumentException e) {
			Log.e(LOG_TAG, "Error when trying to create calendar!", e);
			super.sendErrorMessage(R.string.errorWhenCreatingCalendar);
			return;
		}
		calendarId = findUpdateCalendar();
		
		if (calendarId == -1) {
			Log.e(LOG_TAG, "Couldn' find and create calendar for updating!");
			super.sendErrorMessage(R.string.couldNotAccessLocalCalendar);
			return;
		}
		
		super.sendMaximumMessage(events.size());
		
		int i = 0;
		for (ICalEvent event : events) {
			Log.d(LOG_TAG,"Creating event: " + event);
			createEvent(calendarId, event);
			i++;
			super.sendProgressMessage(i);
		}
		
		super.sendFinishedMessage();
	}

	public void removeExistingCalendar() {
		int calendarId = findUpdateCalendar();
		
		if (calendarId != -1) {
			deleteCalendar(calendarId);
		}
	}
	
	public void createNewCalendar(String name, String displayName) {
		ContentValues calendar = new ContentValues();
		//calendar.put("_", 3);
		calendar.put("_sync_account","local");
		calendar.put("_sync_account_type","local");
		calendar.put("_sync_id",1);
		calendar.put("name", name);
		calendar.put("displayName",displayName);
		calendar.put("hidden",0);
		calendar.put("color",0xFF008080);
		calendar.put("access_level", 700);
		calendar.put("selected", 1);
		calendar.put("sync_events", 1);
		//calendar.put("createdByCategory", 0); //Doesn't work on Motorola Droid / Milestone
		
		Uri calendarUri = Uri.parse(getCalendarUriBase() + "calendars");
		Uri insertedUri = activity.getContentResolver().insert(calendarUri, calendar);		
	}
	
	public int deleteCalendar(int calendarId) {
        int iNumRowsDeleted = 0;

        Uri eventsUri = Uri.parse(getCalendarUriBase()+"calendars");
        Uri eventUri = ContentUris.withAppendedId(eventsUri, calendarId);
        iNumRowsDeleted = activity.getContentResolver().delete(eventUri, null, null);

        Log.i(LOG_TAG, "Deleted " + iNumRowsDeleted + " calendar entry.");

        return iNumRowsDeleted;
    }
	
    public void listAllCalendarEntries(int calendarId) {

        Cursor managedCursor = getCalendarManagedCursor(null, "calendar_id="
                + calendarId, "events");

        if (managedCursor != null && managedCursor.moveToFirst()) {

            Log.i(LOG_TAG, "Listing Calendar Event Details");

            do {

                Log.i(LOG_TAG, "**START Calendar Event Description**");

                for (int i = 0; i < managedCursor.getColumnCount(); i++) {
                    Log.i(LOG_TAG, managedCursor.getColumnName(i) + "="
                            + managedCursor.getString(i));
                }
                Log.i(LOG_TAG, "**END Calendar Event Description**");
            } while (managedCursor.moveToNext());
        } else {
            Log.i(LOG_TAG, "No Calendars");
        }

    }


	
    private Uri createEvent(int calId, ICalEvent event) {
        ContentValues eventValues = new ContentValues();

        eventValues.put("calendar_id", calId);
        eventValues.put("title", event.getSummary());
        eventValues.put("description", event.getDescription());
        //eventValues.put("eventLocation", "");

        long startTime = event.getStart().getTime();//System.currentTimeMillis() + 1000 * 60 * 60;
        long endTime = event.getEnd().getTime();//System.currentTimeMillis() + 1000 * 60 * 60 * 2;

        eventValues.put("dtstart", (event.isWholeDayEvent() ? endTime : startTime));
        eventValues.put("dtend", endTime);

        eventValues.put("allDay", (event.isWholeDayEvent() ? 1 : 0)); // 0 for false, 1 for true
        eventValues.put("eventStatus", 1);
        eventValues.put("visibility", 0);
        eventValues.put("transparency", 0);
        eventValues.put("hasAlarm", 0); // 0 for false, 1 for true
        eventValues.put("_sync_account_type", "local");

        Uri eventsUri = Uri.parse(getCalendarUriBase()+"events");

        Uri insertedUri = activity.getContentResolver().insert(eventsUri, eventValues);
        return insertedUri;
    }

	
	public void listAllCalendarDetails() {
        Cursor managedCursor = getCalendarManagedCursor(null, null, "calendars");

        if (managedCursor != null && managedCursor.moveToFirst()) {

            Log.i(LOG_TAG, "Listing Calendars with Details");

            do {

                Log.i(LOG_TAG, "**START Calendar Description**");

                for (int i = 0; i < managedCursor.getColumnCount(); i++) {
                    Log.i(LOG_TAG, managedCursor.getColumnName(i) + "="
                            + managedCursor.getString(i));
                }
                Log.i(LOG_TAG, "**END Calendar Description**");
            } while (managedCursor.moveToNext());
        } else {
            Log.i(LOG_TAG, "No Calendars");
        }

    }
	
	public int findUpdateCalendar() {
        int result = -1;
        
        String[] projection = new String[] { "_id", "name" };
        String selection = "selected=1";
        String path = "calendars";

        Cursor managedCursor = getCalendarManagedCursor(projection, selection,
                path);

        if (managedCursor != null && managedCursor.moveToFirst()) {

            Log.i(LOG_TAG, "Listing Selected Calendars Only");

            int nameColumn = managedCursor.getColumnIndex("name");
            int idColumn = managedCursor.getColumnIndex("_id");

            do {
                String calName = managedCursor.getString(nameColumn);
                String calId = managedCursor.getString(idColumn);
                Log.i(LOG_TAG, "Found Calendar '" + calName + "' (ID=" + calId + ")");
                if (calName != null && calName.equals(CALENDAR_NAME)) {
                	result = Integer.parseInt(calId);
                }
            } while (managedCursor.moveToNext());
        } else {
            Log.i(LOG_TAG, "No Calendars");
        }

        return result;

    }

    private Cursor getCalendarManagedCursor(String[] projection, String selection, String path) {
        Uri calendars = Uri.parse("content://calendar/" + path);

        Cursor managedCursor = null;
        try {
            managedCursor = activity.managedQuery(calendars, projection, selection, null, null);
        } catch (IllegalArgumentException e) {
            Log.w(LOG_TAG, "Failed to get provider at [" + calendars.toString() + "]");
        }

        if (managedCursor == null) {
            // try again
            calendars = Uri.parse("content://com.android.calendar/" + path);
            try {
                managedCursor = activity.managedQuery(calendars, projection, selection,  null, null);
            } catch (IllegalArgumentException e) {
                Log.w(LOG_TAG, "Failed to get provider at ["  + calendars.toString() + "]");
            }
        }
        return managedCursor;
    }
    

    /*
     * Determines if it's a pre 2.1 or a 2.2 calendar Uri, and returns the Uri
     */
    private String getCalendarUriBase() {
   	
        String calendarUriBase = null;
        Uri calendars = Uri.parse("content://calendar/calendars");
        Cursor managedCursor = null;
        try {
            managedCursor = activity.managedQuery(calendars, null, null, null, null);
        } catch (Exception e) {
            // eat
        }

        if (managedCursor != null) {
            calendarUriBase = "content://calendar/";
        } else {
            calendars = Uri.parse("content://com.android.calendar/calendars");
            try {
                managedCursor = activity.managedQuery(calendars, null, null, null, null);
            } catch (Exception e) {
                // eat
            }

            if (managedCursor != null) {
                calendarUriBase = "content://com.android.calendar/";
            }

        }

        return calendarUriBase;
    }
    
	public void setRemoveCalendar(boolean remove) {
		this.removeCalendar = remove;
	}


}
