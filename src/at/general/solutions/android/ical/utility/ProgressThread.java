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
package at.general.solutions.android.ical.utility;

import java.io.Serializable;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import at.general.solutions.android.ical.activity.R;

public class ProgressThread extends Thread {
    public static final String MAXIMUM_MESSAGE = "maximum";
    public static final String PROGRESS_MESSAGE = "progress";
    public static final String PROGRESS_MESSAGE_OBJECT = "progressObject";
    public static final String FINISHED_MESSAGE = "finished";
    public static final String ERROR_MESSAGE = "error";
    public static final String INIT_MESSAGE = "init";
    public static final String THROWABLE_MESSAGE_OBJECT = "throwableObject";
    
    private Handler handler;

	public ProgressThread() {
		
	}
       
    public void sendMaximumMessage(int maximum) {
    	sendIntegerMessage(maximum, MAXIMUM_MESSAGE);
    }
    
    public void sendProgressMessage(int progress) {
    	sendIntegerMessage(progress, PROGRESS_MESSAGE);
    }
    
    public void sendProgressMessage(int progress, Serializable progressObject) {
    	sendCombinedMessage(progress, PROGRESS_MESSAGE, progressObject, PROGRESS_MESSAGE_OBJECT);
    }
    
    public void sendFinishedMessage() {
    	sendFinishedMessage(null);
    }
    
    public void sendFinishedMessage(String infoText) {
    	sendStringMessage(infoText, FINISHED_MESSAGE);
    }
    
    public void sendErrorMessage(int errorTextResource) {
    	sendIntegerMessage(errorTextResource, ERROR_MESSAGE);
    }
    
    public void sendErrorMessage(int errorTextResource, Throwable t) {
    	sendCombinedMessage(errorTextResource, ERROR_MESSAGE, t, THROWABLE_MESSAGE_OBJECT);
    }
    
    public void sendInitMessage(int infoTextResource) {   	
    	sendIntegerMessage(infoTextResource, INIT_MESSAGE);
    	
    	//Give UI time to redraw
    	try {
			Thread.currentThread().sleep(500);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
    }
    
    public void sendInitMessage() {
    	sendInitMessage(R.string.pleaseWait);
    }
    
    protected void sendCombinedMessage(int payload1, String messageType1, Serializable payload2, String messageType2) {
        if (handler == null) {
        	return;
        }
        
		Message msg = handler.obtainMessage();
        Bundle b = new Bundle();
        b.putInt(messageType1, payload1);
        b.putSerializable(messageType2, payload2);
        
        msg.setData(b);					
		
		handler.sendMessage(msg);
    }
    
    protected void sendIntegerMessage(int progress, String messageType) {
    	sendIntegerMessage(progress, messageType, null);
    }
    
	protected void sendIntegerMessage(int progress, String messageType, Throwable t) {
        if (handler == null) {
        	return;
        }
		
		Message msg = handler.obtainMessage();
        Bundle b = new Bundle();
        b.putInt(messageType, progress);
        
        msg.setData(b);					
		
		handler.sendMessage(msg);
	}
	
	protected void sendStringMessage(String message, String messageType) {
        if (handler == null) {
        	return;
        }
        
		Message msg = handler.obtainMessage();
        Bundle b = new Bundle();
        b.putString(messageType, message);
        msg.setData(b);					
		
		handler.sendMessage(msg);
	}
	
    public Handler getHandler() {
		return handler;
	}

	public void setHandler(Handler handler) {
		this.handler = handler;
	}
    
    
}
