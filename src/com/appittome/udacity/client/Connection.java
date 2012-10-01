package com.appittome.udacity.client;

import android.app.Activity;
import android.util.Log;
import android.content.Context;
import android.net.*;
import android.os.AsyncTask;
import java.net.*;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.BufferedOutputStream;
import java.io.BufferedInputStream;
import java.io.Reader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import org.json.JSONObject;
import org.json.JSONException;

import org.apache.commons.io.IOUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Connection
{
  public interface OnNewCsrfTokenListener
  {
    public void onNewCsrfToken(String token);
  }
  static final boolean DEBUG = true;
  private static OnNewCsrfTokenListener cTokenListener;
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
  public void fetchCourseList() {
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
  private void parseCsrfToken(String page) {
    String retString;
    Pattern csrf_pattern = Pattern.compile("csrf_token = \\\".{60}\\\"");
    Matcher csrf_matcher = csrf_pattern.matcher(page);
    retString = csrf_matcher.find() ? csrf_matcher.group().substring(14,74) : null;//
    if(DEBUG) Log.i("Connection.parseCsrfToken(String)",(retString== null ? "NULL" : retString));
    if (cTokenListener != null && retString != null) cTokenListener.onNewCsrfToken(retString);
  }
  public void setCsrfTokenListener(OnNewCsrfTokenListener cTokenListener) {
    this.cTokenListener = cTokenListener;
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
    protected void onPostExecute(String page) {
      //if(DEBUG) Log.i("Connection.LoadWithCredentialsTask::", page);
      parseCsrfToken(page);
      if(!loggedInto(page)){
	fetchNewCookie();
      } else {
	//TODO possible nasty loop here…
	showMessage("Logged In");
	//TODO grab class list 
	fetchCourseList();
      }
    }
  }

  protected class AsyncJSONTask extends AsyncTask<JSONObject, Integer, JSONObject>
  {
    @Override
    protected JSONObject doInBackground(JSONObject... jsonArray) {
      JSONObject retVal = null;
      for (JSONObject json : jsonArray) { 
	try{
	  retVal = new JSONObject(sendJSON(json, new URL(url, AJAX_SPEC) ));
	}catch (SocketTimeoutException e){
	  if(DEBUG) Log.w("Udacity.Connection.AsyncJSONTask::",
			  "Readtimeout - the server is slow");
	}catch (Exception e){
	  if(DEBUG) Log.w("Udacity.Connection.AsyncJSONTask::",e);
	}
      }
      //TODO If you ever actaully post an array - the return won't work.
      return retVal;
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
	if(DEBUG) Log.w("Udacity.Connection.FetchNewCookieTask::",
			"Readtimeout - the server is slow");
      } catch (IOException e) {
	//TODO create notification / action
	if(DEBUG) Log.w("Udacity.Connection.FetchNewCookieTask::",e);
      } catch (NullPointerException e){
	retVal = MISSING_CREDENTIALS;
      }catch (Exception e){
	if(DEBUG) Log.w("Udacity.Connection.FetchNewCookieTask::",e);
      }
      //if(DEBUG) Log.w("Udacity.Connection.FetchNewCookieTask ->", retVal);
      return retVal;
    }
    @Override
    protected void onPostExecute(String result) {
      if (!result.equals(MISSING_CREDENTIALS)){
	try {
	  JSONObject JSONResp = new JSONObject(result);
	  try {
	    JSONObject payload = JSONResp.getJSONObject("payload");
	    if( payload.getBoolean("reload") ) {
	      checkCredentials();
	    }
	  } catch (JSONException e) {
	    if(DEBUG) Log.w("Udacity.Connection.FetchNewCookieTask - unable to build JSONObject", 
			      JSONResp.toString(2) + e.toString());
	  }
	} catch (JSONException e) {
	  if(DEBUG) Log.w("Udacity.Connection.FetchNewCookieTask - Invalid JSON response", 
			    e.toString());
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
    
    return readAndCloseConnection(conn);
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

    return readAndCloseConnection(conn);
  }

//-- CONNECTION UTILITIES --//
  private String readAndCloseConnection(HttpURLConnection c) {
    String retString = "";
    try {
      InputStream is = c.getInputStream();
      retString = IOUtils.toString(is);
      if(is != null) is.close();
    }catch(Exception e) {
      if(DEBUG) Log.w("Udacity.Connection.grabPage(url) - input stream exception", e.toString());
    } finally {
      c.disconnect();
    }
    return retString;
  }

}
