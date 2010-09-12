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

public class ICalTag {
	public static String EVENT_START = "BEGIN:VEVENT";
	public static String EVENT_END = "END:VEVENT";
	public static String EVENT_DATE_START = "DTSTART";
	public static String EVENT_DATE_END = "DTEND";
	public static String EVENT_SUMMARY = "SUMMARY:";
	public static String ICAL_TIMEZONE = "TZID:";
	public static String EVENT_DESCRIPTION = "DESCRIPTION:";
	
	public static String DATE_VALUE = "VALUE=";
	public static String DATE_TIMEZONE = "TZID=";
}
	