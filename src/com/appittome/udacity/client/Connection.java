package com.appittome.udacity.client;

import android.util.Log;
import android.os.AsyncTask;

import java.net.URL;
import java.net.URLEncoder;
import java.net.HttpURLConnection;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.SocketTimeoutException;
import java.net.MalformedURLException;

import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;

import org.json.JSONObject;
import org.json.JSONException;

import org.apache.commons.io.IOUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HashSet;
import java.util.Iterator;

public class Connection
{
  public interface OnNewCsrfTokenListener
  {
    public void onNewCsrfToken(String token);
  }
  static final boolean DEBUG = true;
  private static OnNewCsrfTokenListener cTokenListener;
  private static HashSet<OnConnectionReadyListener> connectionReadyListeners = 
					      new HashSet<OnConnectionReadyListener>();
  private static String AJAX_SPEC = "/ajax";
  private boolean ready;
  protected static URL url;

  public Connection() {
    this.url = null;
    this.ready = false;
  }

  public Connection(URL url) {
    this.url = url; 
    CookieHandler.setDefault(new CookieManager());
  }

//--- OVERRIDE METHODS ----//
  protected boolean connectionAvailable() {
    return false;
  }
  public boolean isReady() {
    return ready;
  }
  public void fetchCourseList() {
  }
  protected JSONObject getJSONCredentials() {
    return new JSONObject();
  }

  public boolean addOnConnectionReadyListener(OnConnectionReadyListener L) {
    return this.connectionReadyListeners.add(L);
  }

  public void notifyConnectionReadyListeners() {
    Iterator<OnConnectionReadyListener> iter = connectionReadyListeners.iterator();
    while (iter.hasNext()) {
      iter.next().onConnectionReady(this);
    }
    ready = true;
  }

  public interface OnConnectionReadyListener {
    public void onConnectionReady(Connection c);
  }

//---- CREDENTIAL BOUNCE ---//
  public void checkCredentials() {
    new GrabPageTask(){
      @Override
      protected void onPostExecute(HttpURLResponse resp) {
	String page = resp.getResponse();
	parseCsrfToken(page);
	if(!loggedInto(page)){
	  fetchNewCookie();
	} else {
	  //TODO possible nasty loop here…
	  if(DEBUG) Log.i("Udacity.Connection.LoadWithCredentialsTask", "Logged In");
	  notifyConnectionReadyListeners();
	  //fetchCourseList();
	}
      }
    }.execute((String)null);
  }
  public void fetchNewCookie() {
    try {
      new AsyncJSONPostTask() {
	@Override
	protected void onPostExecute(JSONObject JSONResp) {
	  try {
	    JSONObject payload = JSONResp.getJSONObject("payload");
	    if( payload.getBoolean("reload") ) {
	      checkCredentials();
	    }
	  } catch (JSONException e) {
	    if(DEBUG) 
	      Log.w("Udacity.Connection.FetchNewCookieTask","Invalid JSON response" + e);
	  }
	}
      }.execute(getJSONCredentials());
    } catch (NullPointerException e){
      //Expect to catch NullCredentialsException here - thus killing this, asking 
      //for new credentials, and then re-spawning the task after the dialog is closed
      if(DEBUG) Log.w("Udacity.Connection.fetchNewCookie",e.toString());
    }
  }

//--- misc  utilities ---//
  protected boolean loggedInto(String page) {
    return page.contains("Sign Out");
  }

  private void parseCsrfToken(String page) {
    String retString;
    Pattern csrf_pattern = Pattern.compile("csrf_token = \\\".{60}\\\"");
    Matcher csrf_matcher = csrf_pattern.matcher(page);
    retString = csrf_matcher.find() ? csrf_matcher.group().substring(14,74) : null;//
    if(DEBUG) Log.i("Udacity.Connection.parseCsrfToken(String)",
		(retString== null ? "NULL" : retString));
    if (cTokenListener != null && retString != null) 
      cTokenListener.onNewCsrfToken(retString);
  }

  public void setCsrfTokenListener(OnNewCsrfTokenListener cTokenListener) {
    this.cTokenListener = cTokenListener;
  }

//--- ASYNCHRONOUS TASKS ---//
  public class GrabPageTask extends AsyncTask<String, Integer, HttpURLResponse>
  {
    @Override
    protected HttpURLResponse doInBackground(String... specs){
      HttpURLResponse resp = null;
      URL queryURL = url;
      try {
	if(specs[0] != null) queryURL = new URL(url, specs[0]);
        resp = grabPage(queryURL);
      } catch (MalformedURLException e) {
	if(DEBUG) Log.w("Udacity.Connection.grapPageTask", "invalid URL: "+e);
      } catch (IOException e) {
	if(DEBUG) Log.w("Udacity.Connection.grapPageTask", "can't fetch page: "+e);
      }
      return resp;
    }
  }

  public class AsyncJSONPostTask extends AsyncTask<JSONObject, Integer, JSONObject>
  {
    @Override
    protected JSONObject doInBackground(JSONObject... jsonArray) {
      JSONObject retVal = null;
      for (JSONObject json : jsonArray) { 
	try{
	  retVal = new JSONObject(postJSON(json, url ).getResponse());
	}catch (SocketTimeoutException e){
	  if(DEBUG) Log.w("Udacity.Connection.AsyncJSONPostTask::",
			  "Readtimeout - the server is slow");
	}catch (Exception e){
	  if(DEBUG) Log.w("Udacity.Connection.AsyncJSONPostTask::",e);
	}
      }
      //TODO If you ever actaully post an array - the return won't work.
      return retVal;
    }
  }

  public class AsyncJSONGetTask extends AsyncTask<JSONObject, Integer, JSONObject>
  {
    @Override
    protected JSONObject doInBackground(JSONObject... jsonArray) {
      JSONObject retVal = null;
      for (JSONObject json : jsonArray) { 
	try{
	  retVal = new JSONObject(getJSON(json, url ).getResponse());
	}catch (SocketTimeoutException e){
	  if(DEBUG) Log.w("Udacity.Connection.AsyncJSONGetTask::",
			  "Readtimeout - the server is slow");
	}catch (Exception e){
	  if(DEBUG) Log.w("Udacity.Connection.AsyncJSONGetTask::",e);
	}
      }
      //TODO If you ever actaully post an array - the return won't work.
      return retVal;
    }
  }


//---CONNECTION TYPES---//
  private HttpURLResponse grabPage(URL url) throws IOException
  {
    HttpURLResponse resp = null;

    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    setConnectionTimeouts(conn);
    conn.setDoInput(true);
    conn.setRequestProperty("Accept", "*/*");

    conn.connect();
    
    resp = new HttpURLResponse(conn);
    conn.disconnect();

    return resp;
  }

  private HttpURLResponse getJSON(JSONObject jObj, URL mUrl) throws IOException
  {
    //going to fake-parse the message.  Udacity has a strange way of accepting
    //JSON parameters to a GET request:
    // GET /ajax?{%22data%22:{%22path%22:%22Course/cs313/CourseRev/1%22}…
    String message = jObj.toString().replace("\"", "%22").replace("\\", "");
    HttpURLResponse resp = null;
    
    URL qUrl = new URL(mUrl, AJAX_SPEC+"?"+message);
    HttpURLConnection conn = (HttpURLConnection) qUrl.openConnection();

    setJSONConnection(conn);
    setConnectionTimeouts(conn);

    if(DEBUG) Log.i("Udacity.Connection.getJSON","URL:"+qUrl);
    //Start the querry
    conn.connect();

    resp = new HttpURLResponse(conn);
    conn.disconnect();
    return resp;
  }

  private HttpURLResponse postJSON(JSONObject jObj, URL mUrl) throws IOException 
  {
    String message = jObj.toString();
    HttpURLResponse resp = null;

    URL qUrl = new URL(mUrl, AJAX_SPEC);
    HttpURLConnection conn = (HttpURLConnection) qUrl.openConnection();

    setJSONConnection(conn);
    setConnectionTimeouts(conn);
    //POST
    conn.setFixedLengthStreamingMode(message.getBytes().length);
    conn.setDoOutput(true);
   
    if(DEBUG) Log.i("Udacity.Connection.postJSON","URL:"+qUrl);
    if(DEBUG) Log.i("Udacity.Connection.postJSON","JSON:"+message);
    //Start the querry
    conn.connect();
    OutputStream os = new BufferedOutputStream(conn.getOutputStream());
    os.write(message.getBytes());
    os.flush();
    os.close();

    resp = new HttpURLResponse(conn);
    conn.disconnect();
    return resp;
  }

  private HttpURLConnection setConnectionTimeouts(HttpURLConnection c) {
    c.setReadTimeout( 15*1000 /*1000 milliseconds = 1 sec */ );
    c.setConnectTimeout( 15000 /* milliseconds */ );
    return c;
  }
  private HttpURLConnection setJSONConnection(HttpURLConnection c) {
    c.setDoInput(true);
    c.setRequestProperty("Accept", "*/*");
    c.setRequestProperty("Content-Type", 
			    "application/json;charset=utf-8");
    c.setRequestProperty("X-Requested-With", "XMLHttpRequest");
    c.setFollowRedirects(false);
    return c;
  }
}
