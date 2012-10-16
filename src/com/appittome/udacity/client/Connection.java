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
/**
 * A generic class to manage asynchronous HttpUrlConnections.  Asycn connections
 * can be generally implemented by extending the Task subclasses of this class.
 * After a onConnectionReadyListener event Task can be executed by simply adding
 * a onPostExecute implementation and .execute()
 *
 */
public abstract class Connection
{
  /** a debug switch */
  static final boolean DEBUG = true;
  /** for now there is only one CSRF_TOKEN listener */
  private static OnNewCsrfTokenListener cTokenListener;
  /** a list of objects interestined in the connection being ready*/
  private static HashSet<OnConnectionReadyListener> connectionReadyListeners = 
					      new HashSet<OnConnectionReadyListener>();
  /** the ajax execution path on the server*/
  private static String AJAX_SPEC = "/ajax";
  /** the root URL of the server*/
  protected static URL url;

  /**
   * Initializes the root URL to null. This object won't work until
   * the url is defined.
   */
  public Connection() {
    this(null);
  }
  /**
   * Instantiates a working Connection object.
   * Cookies are managed by the anrdoid OS.
   * @param url the root url of the server this connection will interact with.
   */
  public Connection(URL url) {
    this.url = url; 
    CookieHandler.setDefault(new CookieManager());
  }

//--- OVERRIDE METHODS ----//
  /**
   * Should be implemented within the activity to query the CONNECTIVITY_SERVICE.
   * @return boolean true when the android os has a route for WAN connections 
   *         (WiFi, cellular, etc…)
   */
  abstract boolean connectionAvailable();
  /**
   * Implemented in the main activity to give access to the 
   * {@link SignInCredentials} Object.
   * @return JSONObject representing valid credentials for login
   */
  abstract JSONObject getJSONCredentials();
//--- LISTENER INTERFACES ---//
  /**
   * Implement to listen for new CSRF_TOKENS.  Each time a new token is found
   * (regex "csrf_token = \\\".{60}\\\"") the most recent page, the token is 
   * passed out via the onNewCsrfToken method.
   */
  public interface OnNewCsrfTokenListener {
    /**
     * Passed a CSRF_TOKEN everytime the regex pattern 
     * "csrf_token = \\\".{60}\\\"" is found on the most recent page loaded
     * from the server.
     * @param token CSRF_TOKEN as string
     */
    public void onNewCsrfToken(String token);
  }
  /**
   * Sets the one and only CSRF_TOKEN Listener.
   * @param cTokenListener the listener to be notifed on the event..
   */
  public void setCsrfTokenListener(OnNewCsrfTokenListener cTokenListener) {
    this.cTokenListener = cTokenListener;
  }
  /**
   * Implement this interface to be notified that the connection is functional.
   * To be functional the conection must have an active network connection, 
   * and valid credentials.
   */
  public interface OnConnectionReadyListener {
    /** 
     * Called when the connection is prepared to properly send messages
     * to the server. This means that the credentials have been validated.
     * @param c  the connection that has been properly setup.
     */
    public void onConnectionReady(Connection c);
  }
  /**
   * Add another member to the list of listeners for this event.
   * @param L - the listener to add
   * @return boolean true if this set did not already contain the specified listener 
   *                 (@see java.util.HashSet)
   */
  public boolean addOnConnectionReadyListener(OnConnectionReadyListener L) {
    return this.connectionReadyListeners.add(L);
  }
  /**
   * Notifies all listeners that this connection object has a network 
   * connection and successfully validated credentials.
   */
  public void notifyConnectionReadyListeners() {
    Iterator<OnConnectionReadyListener> iter = connectionReadyListeners.iterator();
    while (iter.hasNext()) {
      iter.next().onConnectionReady(this);
    }
  }
//---- CREDENTIAL BOUNCE ---//
  /**
   * Attempts to verify both that there are credentials, and that 
   * the server accepts them (ie that this connection object already
   * carries a valid session cookie). Note that there is no return: this
   * is an asyc process that will <code>notifyConnectionReadyListeners</code>.
   */
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
	}
      }
    }.execute((String)null);
  }
  /**
   * Attempts to fetch a valid cookie credential from the server. The user
   * will be prompted for credentials if none exist, and then this connection
   * will attempt to get a vaild session cookie from the server. Note: 
   * this is an async process that will <code>notifyConnectionReadyListeners</code>.
   */
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
  /**
   * Takes a response and determines if the credentials were successful.
   * In the Udacity case - the credentials are posted, the page refreshed,
   * and if the credentials worked there will be "Sign Out" found somewhere
   * on the page. Override this to provide your own method.
   * @param page the full text of a refresh response after POST of 
   *               credentials JSONObject
   * @return boolean true when the connection has successfully logged in.
   */
  protected boolean loggedInto(String page) {
    return page.contains("Sign Out");
  }
  /**
   * Find the needle in a haystack.  This method runs a regex 
   * ("csrf_token = \\\".{60}\\\"") over the contents of a page and pulls out
   * what matches as the CSRF_TOKEN.  onCsrfTokenListener is then notified, 
   * even if none was found (null).  Override to implement your own regex / method.
   */
  protected void parseCsrfToken(String page) {
    String retString;
    Pattern csrf_pattern = Pattern.compile("csrf_token = \\\".{60}\\\"");
    Matcher csrf_matcher = csrf_pattern.matcher(page);
    retString = csrf_matcher.find() ? csrf_matcher.group().substring(14,74) : null;//
    if(DEBUG) Log.i("Udacity.Connection.parseCsrfToken(String)",
		(retString== null ? "NULL" : retString));
    if (cTokenListener != null && retString != null) 
      cTokenListener.onNewCsrfToken(retString);
  }


//--- ASYNCHRONOUS TASKS ---//
  /**
   * Does a GET on whatever URL is described by URL + specs.
   * Should be extended with a onPostExecute method, and then <code>execute</code>
   */
  public class GrabPageTask extends AsyncTask<String, Integer, HttpURLResponse>
  {
    /**
     * Asynchronously runs a vanilla GET agains URL+spec
     * @param specs the URI to query
     * @return HttpURLResponse the query response
     */
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

  /**
   * POST JSON to URL+AJAX_SPEC.
   * Extended with onPostExecute, and run with <code>execute</code>
   */
  public class AsyncJSONPostTask extends AsyncTask<JSONObject, Integer, JSONObject>
  {
    /**
     * Asynchronously POST JSONObect to URL+AJAX_SPEC
     * @param jsonArray JSON to be POSTed to server on URL+AJAX_SPEC
     *                   Note: don't actually POST an array of objects, this
     *                   will only return the response from the last object.
     * @return JSONObject the json response from URL+AJAX_SPEC
     */
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

  /**
   * Send a JSONObject to the server at URL+AJAX_SPEC via a GET request.
   * Extend with onPostExecute, then run with <code>execute</code>
   */
  public class AsyncJSONGetTask extends AsyncTask<JSONObject, Integer, JSONObject>
  {
    /**
     * Send a JSONObject to URL+AJAX_SPEC via GET.
     * Array parameters will return the last reponse in the array.
     * @param jsonArray object to be sent
     * @return JSONObject response object
     */
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
  /**
   * Connection for grabPageTask.
   * @param url the url to run the GET against
   * @return HttpURLResponse the full response from the server
   * @throws IOException if the response stream couldn't be opened properly
   */
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
  /**
   * Connection for AsycnJSONGetTask. Udacity permits odd characters for get
   * requests, so this may not be very generic.
   * @param jObj Object to be sent via GET request
   * @param url root URL for the request - request will be of the format i
   *            URL+AJAX_SPEC+"?"+message
   * @return HttpURLResponse a response object containting the full state
   *	     		     of the response
   * @throws IOException if the reponse stream could not be properly handled
   */
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
  /**
   * Connection for AsyncJSONPostTask.
   * @param jObj object to POST to server
   * @param url root URL for post, post will be to URL+AJAX_SPEC
   * @return HttpURLResponse Object representing full http reponse 
   * @throws IOExeception if reponse stream cannot be properly handled
   */
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
  /**
   * Utility method to generically setup HttpURLConnection timeouts.
   */
  private HttpURLConnection setConnectionTimeouts(HttpURLConnection c) {
    c.setReadTimeout( 15*1000 /*1000 milliseconds = 1 sec */ );
    c.setConnectTimeout( 15000 /* milliseconds */ );
    return c;
  }
  /**
   * Utility method to generically setup conncection to mimic http ajax JSON message.
   */
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
