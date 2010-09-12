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

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;
import at.general.solutions.android.ical.activity.ICalSynchronizeActivity;
import at.general.solutions.android.ical.activity.R;

public abstract class ProgressHandler extends Handler {
	private ProgressDialog progressDialog;
	private final static String LOG_TAG = "ProgressHandler";
	
	public ProgressDialog getProgressDialog() {
		return progressDialog;
	}
	private ProgressThread workerThread;
	private Context context;
	
	
	public ProgressHandler(ProgressThread thread, Context context) {
		this.workerThread = thread;
		this.context = context;
        this.workerThread.setHandler(this);

		initDialog();
	}
	
	public ProgressHandler(ProgressThread thread, ProgressDialog dialog) {
		this.workerThread = thread;
		this.progressDialog = dialog;
        this.workerThread.setHandler(this);
        this.context = dialog.getContext();
	}
	
	private void initDialog() {
		progressDialog = new ProgressDialog(context);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setMessage(context.getString(R.string.pleaseWait));
        
	}
	
	public void startWorkerThread() {
		try {
			this.workerThread.start();
		}
		catch (Throwable e) {
			Log.e(LOG_TAG, "Error when executing worker thread!", e);
			Message msg = new Message();
			Bundle bundle = new Bundle();
			bundle.putSerializable(ProgressThread.THROWABLE_MESSAGE_OBJECT, e);
			bundle.putInt(ProgressThread.ERROR_MESSAGE, R.string.internalError);
			handleMessage(msg);
		}
	}
	
	@Override
	public void handleMessage(Message msg) {		
		if (msg.getData().containsKey(ProgressThread.INIT_MESSAGE)) {
    		progressDialog.setMessage(context.getString(msg.getData().getInt(ProgressThread.INIT_MESSAGE)));
    		progressDialog.setMax(100);
    		progressDialog.setProgress(0);
    	}
    	else if (msg.getData().containsKey(ProgressThread.MAXIMUM_MESSAGE)) {
    		progressDialog.setMax(msg.getData().getInt(ProgressThread.MAXIMUM_MESSAGE));
    	}
    	else if (msg.getData().containsKey(ProgressThread.PROGRESS_MESSAGE)) {
    		if (msg.getData().containsKey(ProgressThread.PROGRESS_MESSAGE_OBJECT)) {
    			onProgress(msg.getData().getSerializable(ProgressThread.PROGRESS_MESSAGE_OBJECT));
    		}
    		progressDialog.setProgress(msg.getData().getInt(ProgressThread.PROGRESS_MESSAGE));
    	}
    	else if (msg.getData().containsKey(ProgressThread.FINISHED_MESSAGE)) {
    		String infoText = msg.getData().getString(ProgressThread.FINISHED_MESSAGE);    		
    		this.onFinish(infoText);
    	}
    	else if (msg.getData().containsKey(ProgressThread.ERROR_MESSAGE)) {
    		String errorText = context.getString(msg.getData().getInt(ProgressThread.ERROR_MESSAGE));
    		
    		if (msg.getData().containsKey(ProgressThread.THROWABLE_MESSAGE_OBJECT)) {
    			Throwable t = (Throwable) msg.getData().getSerializable(ProgressThread.THROWABLE_MESSAGE_OBJECT);
    			errorText += "\n" + context.getString(R.string.errorMessage) + " " + t.getLocalizedMessage();
    		}
    		
    		progressDialog.dismiss();
    		
    		AlertDialog.Builder dlgBuilder = new AlertDialog.Builder(context)
    			.setTitle(context.getString(R.string.anErrorOccured)).setMessage(errorText)
    			.setCancelable(false)
    			.setPositiveButton(context.getString(R.string.ok), new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						dialog.cancel();						
					}
				});
    		dlgBuilder.create().show();
    		
    		onError(errorText);
    	}
	}
	
	public abstract void onFinish(String infoText);
	public abstract void onError(String infoText);
	
	public void onProgress(Serializable object) {
		//Ignore progress object
	}
}
