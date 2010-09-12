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
package at.general.solutions.android.ical.remote;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.UnknownHostException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.CharArrayBuffer;
import org.apache.http.util.EntityUtils;

import android.net.SSLCertificateSocketFactory;
import android.util.Log;
import at.general.solutions.android.ical.activity.R;
import at.general.solutions.android.ical.remote.ssl.EasySSLSocketFactory;
import at.general.solutions.android.ical.utility.ProgressThread;


public class HttpDownloadThread extends ProgressThread {
	private static final String LOG_TAG = "HttpDownloadThread";
	private static final String USER_AGENT = "AndroidIcalSynchronizer/0.1";
	
	private String remoteUrl;
	private boolean useAuthentication;
	private String remotePassword;
	private String remoteUsername;
	private String encoding;
	
	public HttpDownloadThread(String remoteUrl, String encoding) {
		super();
		this.remoteUrl = remoteUrl;
		this.useAuthentication = false;
	}
	
	public HttpDownloadThread(String remoteCalendarUrl, String remoteUsername, String remotePassword, String encoding) {
		super();
		this.remoteUrl = remoteCalendarUrl;
		this.useAuthentication = true;
		this.remoteUsername = remoteUsername;
		this.remotePassword = remotePassword;
		this.encoding = encoding;
	}
	
    private static class SimpleX509TrustManager implements X509TrustManager
    {
        public void checkClientTrusted(X509Certificate[] chain, String authType)
        throws CertificateException
        {
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType)
        throws CertificateException
        {
        }

        public X509Certificate[] getAcceptedIssuers()
        {
            return null;
        }
    }
	
	private  class DummyX509TrustManager implements X509TrustManager { 
	     private X509TrustManager standardTrustManager = null; 

	     /** 
	      * Constructor for DummyX509TrustManager. 
	      */ 
	     public DummyX509TrustManager(KeyStore keystore) throws   
	NoSuchAlgorithmException, KeyStoreException { 
	         super(); 
	         String algo = TrustManagerFactory.getDefaultAlgorithm(); 
	         TrustManagerFactory factory =   
	TrustManagerFactory.getInstance(algo); 
	         factory.init(keystore); 
	         TrustManager[] trustmanagers = factory.getTrustManagers(); 
	         if (trustmanagers.length == 0) { 
	             throw new NoSuchAlgorithmException(algo + " trust manager   not supported"); 
	         } 
	         this.standardTrustManager = (X509TrustManager)trustmanagers[0]; 
	     } 

	     /** 
	      * @see   
	javax.net.ssl.X509TrustManager#checkClientTrusted(X509Certificate[],   
	String) 
	      */ 
	     public boolean isClientTrusted(X509Certificate[] certificates) { 
	         return true; 
	     } 

	     /** 
	      * @see   
	javax.net.ssl.X509TrustManager#checkServerTrusted(X509Certificate[],   
	String) 
	      */ 
	     public boolean isServerTrusted(X509Certificate[] certificates) { 
	         return true; 
	     } 

	     /** 
	      * @see javax.net.ssl.X509TrustManager#getAcceptedIssuers() 
	      */ 
	     public X509Certificate[] getAcceptedIssuers() { 
	         return this.standardTrustManager.getAcceptedIssuers(); 
	     } 

	     public void checkClientTrusted(X509Certificate[] arg0, String   
	arg1) throws CertificateException { 
	         // do nothing 

	     } 

	     public void checkServerTrusted(X509Certificate[] arg0, String   
	arg1) throws CertificateException { 
	         // do nothing 

	     } 
	} 


	
	@Override
	public void run() {
		HttpParams params = new BasicHttpParams();
		ConnManagerParams.setMaxTotalConnections(params, 100);
        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
        HttpProtocolParams.setUserAgent(params, USER_AGENT);
        
        SchemeRegistry schemeRegistry = new SchemeRegistry();
        schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));    	
    	schemeRegistry.register(new Scheme("https", new EasySSLSocketFactory(), 443));        
        
        
        ClientConnectionManager cm = new ThreadSafeClientConnManager(params, schemeRegistry);
        DefaultHttpClient client = new DefaultHttpClient(cm, params);
        
        // new UsernamePasswordCredentials("18167-kalender@web.dav", "drx135sat"));
        
        if (useAuthentication) {
	        client.getCredentialsProvider().setCredentials(
	                new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
	                new UsernamePasswordCredentials(remoteUsername, remotePassword));
        }

        HttpGet get = new HttpGet(remoteUrl);

        try {
        	super.sendInitMessage(R.string.downloading);

        	HttpResponse response = client.execute(get);
        	Log.d(LOG_TAG, response.getStatusLine().getReasonPhrase() + " " + isGoodResponse(response.getStatusLine().getStatusCode()));
        	if (isGoodResponse(response.getStatusLine().getStatusCode())) {
        		HttpEntity entity = response.getEntity();

        		InputStream instream = entity.getContent();
        		if (instream == null) {
        			super.sendErrorMessage(R.string.couldnotConnectToRemoteserver);
        			return;
        		}
    			if (entity.getContentLength() > Integer.MAX_VALUE) {
    				super.sendErrorMessage(R.string.remoteFileTooLarge);
    				return;
    			}
    			int i = (int)entity.getContentLength();
    			if (i < 0) {
    				i = 4096;
    			}
    			String charset = EntityUtils.getContentCharSet(entity);
    			if (charset == null) {
    				charset = encoding;
    			}
    			if (charset == null) {
    				charset = HTTP.DEFAULT_CONTENT_CHARSET;
    			}
    			Reader reader = new InputStreamReader(instream, charset);
    			CharArrayBuffer buffer = new CharArrayBuffer(i); 

    			super.sendMaximumMessage(i);

    			try {
    				char[] tmp = new char[1024];
    				int l;
    				while((l = reader.read(tmp)) != -1) {
    					buffer.append(tmp, 0, l);
    					super.sendProgressMessage(buffer.length());
    				}
    			} finally {
    				reader.close();
    			}

    			super.sendFinishedMessage(buffer.toString());
    		}
    		else {
    			int errorMsg = R.string.couldnotConnectToRemoteserver;
    			if (isAccessDenied(response.getStatusLine().getStatusCode())) {
    				errorMsg = R.string.accessDenied;
    			}
    			else if (isFileNotFound(response.getStatusLine().getStatusCode())) {
    				errorMsg = R.string.remoteFileNotFound;
    			}
    			super.sendErrorMessage(errorMsg);
    		}
        } 
        catch (UnknownHostException e) {
        	super.sendErrorMessage(R.string.unknownHostException, e);
        	Log.e(LOG_TAG, "Error occured", e);
        }        
        catch (Throwable e) {
        	super.sendErrorMessage(R.string.couldnotConnectToRemoteserver, e);
        	Log.e(LOG_TAG, "Error occured", e);
        } 

        finally {
        	client.getConnectionManager().shutdown(); 
        }
	}
	

    /**
     * Is the status code 2xx
     */
    private boolean isGoodResponse(int statusCode) {
            return ((statusCode >= 200) && (statusCode <= 299));
    }
    
    private boolean isFileNotFound(int statusCode) {
    	return statusCode == 404;
    }
    
    private boolean isAccessDenied(int statusCode) {
    	return statusCode == 401;
    }
    
    
}
