package com.appittome.udacity.client;

import android.app.Activity;
import android.content.Context;
import android.net.*;
import android.os.AsyncTask;
import java.net.*;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.BufferedOutputStream;
import java.io.Reader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import android.util.Log;
import org.json.JSONObject;
import org.json.JSONException;


public class Connection
{
  private static String AJAX_SPEC = "/ajax";
  private URL url;

  public Connection(URL url) {
    this.url = url; 
    CookieHandler.setDefault(new CookieManager());
  }

  protected boolean connectionAvailable() {
    return false;
  }
  protected void showMessage(String msg) {
  }
  protected JSONObject getJSONCredentials() {
    return new JSONObject();
  }
  public void checkCredentials() {
    new LoadWithCredentialsTask().execute(url);
  }
  public void fetchNewCookie() {
    new FetchNewCookieTask().execute(url);
  }
  private boolean loggedInto(String page) {
    return page.contains("Sign Out");
  }


//--- ASYNCHRONOUS TASKS ---//
  private class LoadWithCredentialsTask extends AsyncTask<URL, Integer, String>
  {
    @Override
    protected String doInBackground(URL... urls){
      String result ="";
      try {
        result = grabPage(urls[0]);
      } catch (IOException e) {
	//TODO create notification / action
      }

      return result;
    }
    @Override
    protected void onPostExecute(String result) {
      if(!loggedInto(result)){
	fetchNewCookie();
      } else {
	//TODO possible nasty loop here…
	showMessage("Logged In");
	//TODO grab class list 
      }
    }
  }

  private class FetchNewCookieTask extends AsyncTask<URL, Integer, String>
  {
    public static final String MISSING_CREDENTIALS = "001";
    @Override
    protected String doInBackground(URL... urls) {
      String retVal = "";
      try{
	retVal = sendJSON(getJSONCredentials(), new URL(urls[0], AJAX_SPEC) );
      }catch (SocketTimeoutException e){
	Log.w("Udacity.Connection.FetchNewCookieTask::",
		"Readtimeout - the server is slow");
      } catch (IOException e) {
	//TODO create notification / action
	Log.w("Udacity.Connection.FetchNewCookieTask::",e);
      } catch (NullPointerException e){
	retVal = MISSING_CREDENTIALS;
      }catch (Exception e){
	Log.w("Udacity.Connection.FetchNewCookieTask::",e);
      }
      Log.w("Udacity.Connection.FetchNewCookieTask ->", retVal);
      return retVal;
    }
    @Override
    protected void onPostExecute(String result) {
      if (!result.equals(MISSING_CREDENTIALS)){
	try {
	  JSONObject JSONResp = new JSONObject(result);
	  try {
	    String reply = JSONResp.getString("win");
	    if( reply.compareTo("loaded cookie") == 0 ) {
	      checkCredentials();
	    }
	  } catch (JSONException e) {
	    showMessage(JSONResp.toString(2));
	  }
	} catch (JSONException e) {
	  showMessage("Invalid JSON returned::" + e);
	}
      }
    }
  }
//---CONNECTION TYPES---//
  private String grabPage(URL url) throws IOException
  {
    InputStream is = null;
    String retString = "";
    int len = 0;

    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setReadTimeout( 10000 /*milliseconds*/ );
    conn.setConnectTimeout( 15000 /* milliseconds */ );
    conn.setDoInput(true);
    conn.setRequestProperty("Accept", "*/*");
    
    conn.connect();
    try {
      len = Integer.parseInt(conn.getHeaderField("Content-Length"));
    } catch (NumberFormatException e) {
      len = 500;
    }
    try {
      is = conn.getInputStream();
      retString = readIt(is,len);
    } finally {
      if (is !=null) 
	is.close();
      conn.disconnect();
    }
    return retString;
  }
  private String sendJSON(JSONObject jObj, URL url) throws IOException 
  {
    String message = jObj.toString();
    String retString = "";
    InputStream is = null;
    int len = 0;

    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setReadTimeout( 15*1000 /*1000 milliseconds = 1 sec */ );
    conn.setConnectTimeout( 15000 /* milliseconds */ );
    conn.setRequestMethod("POST");
    conn.setDoInput(true);
    conn.setDoOutput(true);
    conn.setFixedLengthStreamingMode(message.getBytes().length);
    conn.setRequestProperty("Accept", "*/*");
    conn.setRequestProperty("Content-Type", 
			    "application/json;charset=utf-8");
    conn.setRequestProperty("X-Requested-With", "XMLHttpRequest");
    
    //Start the querry
    conn.connect();
    OutputStream os = new BufferedOutputStream(conn.getOutputStream());
    os.write(message.getBytes());
    os.flush();
    os.close();

    //int response = conn.getResponseCode();
    //Log.i("Udacity::AsyncNetwork:", "Response is: " + response);
    try {
      len = Integer.parseInt(conn.getHeaderField("Content-Length"));
    } catch (NumberFormatException e) {
      len = 50;
    }
    try {
      is = conn.getInputStream();
      retString = readIt(is,len);
    } finally {
      if (is !=null) 
	is.close();
      conn.disconnect();
    }
      
    return retString;
  }

  public String readIt(InputStream stream, int len) throws IOException, 
					       UnsupportedEncodingException {
    Reader reader = null;
    reader = new InputStreamReader(stream, "UTF-8");
    char[] buffer = new char[len];
    reader.read(buffer);
    return new String(buffer);
  }

}
