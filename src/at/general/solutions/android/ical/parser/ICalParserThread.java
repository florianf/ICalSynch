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
package at.general.solutions.android.ical.parser;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.apache.commons.lang.StringUtils;

import android.util.Log;
import at.general.solutions.android.ical.activity.R;
import at.general.solutions.android.ical.model.ICalEvent;
import at.general.solutions.android.ical.utility.PreferencesUtility;
import at.general.solutions.android.ical.utility.ProgressThread;

public class ICalParserThread extends ProgressThread{
	private static final String LOG_TAG = "IcalParser";
	
    /* Ical Dateformat */
    private static SimpleDateFormat ICAL_DATETIME_FORMAT = new SimpleDateFormat("yyyyMMdd'T'HHmmss");
    private static SimpleDateFormat ICAL_DATE_FORMAT = new SimpleDateFormat("yyyyMMdd");
	
	private TimeZone icalDefaultTimeZone;
	private TimeZone userTimeZone;
	private String toParse;
	
	public ICalParserThread(String icalDefaultTimeZone, String userTimeZone, String toParse) {
		if (icalDefaultTimeZone.equalsIgnoreCase(PreferencesUtility.DEFAULT_TIMEZONE_PREF_VALUE)) {
			this.icalDefaultTimeZone = TimeZone.getTimeZone("UTC");
		}
		else {
			this.icalDefaultTimeZone = TimeZone.getTimeZone(icalDefaultTimeZone);
		}
		
		if (userTimeZone.equals(PreferencesUtility.DEFAULT_TIMEZONE_PREF_VALUE)) {
			this.userTimeZone = TimeZone.getDefault();
		}
		else {
			this.userTimeZone = TimeZone.getTimeZone(userTimeZone);
		}
		
        ICAL_DATETIME_FORMAT.setTimeZone(this.icalDefaultTimeZone);
        ICAL_DATE_FORMAT.setTimeZone(this.icalDefaultTimeZone);
		
		this.toParse = toParse;
	}
	
	@Override
	public void run() {
		super.sendInitMessage(R.string.parsingIcalFile);
		
		String[] lines = toParse.split("\n");
		
		super.sendMaximumMessage(lines.length);
		
		ICalEvent event = null;
		
		int i = 0;
		
		boolean inDescription = false;
		String description = "";
		
		for (String line : lines) {
			line = StringUtils.chomp(line);
			
			if (event == null) {
				if (line.contains(ICalTag.EVENT_START)) {
					event = new ICalEvent(userTimeZone);
				}
				inDescription = false;
			}
			else  if (line.contains(ICalTag.EVENT_END)) {
				event.setDescription(cleanText(description));
				
				if (event != null && event.getStart() != null) {
					super.sendProgressMessage(i, event);
				}
				event = null;
				description = "";
				inDescription = false;
			}
			else {
				if (line.contains(ICalTag.EVENT_SUMMARY)) {
					event.setSummary(cleanText(line.substring(ICalTag.EVENT_SUMMARY.length())));
				}
				else if (line.contains(ICalTag.EVENT_DATE_START)) {
					String dateLine = line.substring(ICalTag.EVENT_DATE_START.length());
					event.setStart(parseIcalDate(dateLine));
				}
				else if (line.contains(ICalTag.EVENT_DATE_END)) {
					String dateLine = line.substring(ICalTag.EVENT_DATE_END.length());
					event.setEnd(parseIcalDate(dateLine));
				}
				
				if (inDescription) {
					if (line.charAt(0) == ' ') {
						description += line.substring(1);
					}
				}
				else if (line.contains(ICalTag.EVENT_DESCRIPTION)) {
					description = line.substring(ICalTag.EVENT_DESCRIPTION.length());
					inDescription = true;
				}
				else {
					inDescription = false;
				}
			}
			i++;
		}
		
		super.sendFinishedMessage();
	}
	
	private String cleanText(String text) {
		text = StringUtils.replace(text, "\\n", "\n");
		text = StringUtils.replace(text, "\\,", ",");
		text = StringUtils.replace(text, "\\\"", "\"");
		return text;
	}
	
	private Date parseIcalDate(String dateLine)  {
		try {
			dateLine = StringUtils.replace(dateLine, ";", "");
			Date date = null;
			if (dateLine.contains(ICalTag.DATE_TIMEZONE)) {
				String[] parts = StringUtils.split(dateLine,":");
				ICAL_DATETIME_FORMAT.setTimeZone(TimeZone.getTimeZone(parts[0].substring(ICalTag.DATE_TIMEZONE.length(), parts[0].length())));
				date = ICAL_DATETIME_FORMAT.parse(parts[1]);
				ICAL_DATETIME_FORMAT.setTimeZone(icalDefaultTimeZone);
			}
			else if (dateLine.contains(ICalTag.DATE_VALUE)) {
				String[] parts = StringUtils.split(dateLine,":");
				date = ICAL_DATE_FORMAT.parse(parts[1]);
				date.setHours(0);
				date.setMinutes(0);
				date.setSeconds(0);
			}
			else {
				dateLine = StringUtils.replace(dateLine, ":", "");
				date = ICAL_DATETIME_FORMAT.parse(dateLine);
			}
			return date;
		}
		catch (ParseException e) {
			Log.e(LOG_TAG, "Cant't parse date!", e);
			return null;
		}
    }
	
}
