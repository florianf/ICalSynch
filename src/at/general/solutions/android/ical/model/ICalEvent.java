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
package at.general.solutions.android.ical.model;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public class ICalEvent implements Comparable<ICalEvent>, Serializable{
	private static final long serialVersionUID = -36824366619833585L;
	
	private Date start;
	private Date end;
	private String summary;
	private String description;
	private String location;
	private TimeZone timeZone;
	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd.MM.yyyy HH:mm");

	
	public ICalEvent(TimeZone timeZone) {
		super();
		this.timeZone = timeZone;
		DATE_FORMAT.setTimeZone(timeZone);
	}
	public TimeZone getTimeZone() {
		return timeZone;
	}
	public void setTimeZone(TimeZone timeZone) {
		this.timeZone = timeZone;
	}

	public Date getStart() {
		return start;
	}
	public void setStart(Date start) {
		this.start = start;
		if (this.end == null) {
			this.end = this.start;
		}
	}
	public Date getEnd() {
		return end;
	}
	public void setEnd(Date end) {
		this.end = end;
	}

	public String getSummary() {
		return summary;
	}
	public void setSummary(String summary) {
		this.summary = summary;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getLocation() {
		return location;
	}
	public void setLocation(String location) {
		this.location = location;
	}
	public String toString() {
		return (start != null ? DATE_FORMAT.format(start) : "UNDEF") + " to " + (end != null ? DATE_FORMAT.format(end) : "UNDEF") + " / " + summary + " / " + description;
	}
	@Override
	public int compareTo(ICalEvent another) {
		if (this.start == null || another.getStart() == null) {
			return 0;
		}
		
		return -1 * this.start.compareTo(another.getStart());
	}
	
	public boolean isWholeDayEvent() {
		return start.getHours() == 0 && start.getMinutes() == 0 && start.getSeconds() == 0
			&& end.getHours() == 0 && end.getMinutes() == 0 && end.getSeconds() == 0;
	}
	
}
