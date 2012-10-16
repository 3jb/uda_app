package com.appittome.udacity.client;

import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;

import android.support.v4.view.ViewPager;

import android.os.Bundle;
import android.content.Context;

import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ListView;
import android.widget.ImageView;
import android.widget.Toast;      

import android.util.Log;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import java.net.URL;
import java.net.MalformedURLException;

import org.json.JSONObject;
import org.json.JSONException;
import org.json.JSONArray;
/**
 *  <b>Start here</b>:: This is the Activity Class of the Application, 
 *  most of the C from MVC is intended to end up here.
 * <h3>User Interface</h3>
 * The user design constists of 5 screens that appears as follows::
 * <div style="overflow:hidden;width:100%;">
 * <table style="border:0px;padding:0px;height:550px;width:100%;">
 * <tbody style="display:block;overflow:auto;width:840px;height:100%;">
 * <tr>
 * <td>Sign In</td>
 * <td>Course Selection</td>
 * <td>Unit selection</td>
 * <td>Video Selection</td>
 * <td>Youtube video play</td>
 * </tr>
 * <tr>
 * <td><img src="../../../../img/20121011_screen_00.png"></td>
 * <td><img src="../../../../img/20121011_screen_01.png"></td>
 * <td><img src="../../../../img/20121011_screen_02.png"></td>
 * <td><img src="../../../../img/20121011_screen_03.png"></td>
 * <td><img src="../../../../img/20121011_screen_04.png"></td>
 * </tr>
 * </tbody>
 * </table></div>
 * <p>
 * This class naturally attempts to organize much of the communication
 * between the functional parts of application and the android OS.
 * two main parts of the app require interaction with various parts of the 
 * android os:</p>
 * <p>
 * <b>User Interface</b> requires access to the activity mainly to "inflate" XML resources
 * to create new views that will then be linked back to the appropriate 
 * parent in the display graph.  A seperate UI class ({@link UserInterface}) holds
 * a set of utilites for the UI that access the activity through a subclass 
 * <code>UdacityUserInterface</code> that populates a set of abstract methods to 
 * access critical methods of the acivity, thereby limiting direct dependance and
 * enabling modularity.</p>
 * <p>The second portion of the UI, the {@link SwipeAdapter} is a bit more conventional
 * as it implements a FragmentStatePagerAdapter, that requires the activity (or context)
 * as an argument.</p> 
 * <p>This implementation is disjoint, but came about as a result of itteratively adding
 * complexity.</p>
 * <p>
 * <b>Network Connection</b> required access to a Network oriented Android
 * services available only through the activity.  The method of abstracting the necessary
 * parts to be extended as a subclass of Udacity was used to allow similar benifits to 
 * the User Interface.</p>
 * <p>The third portion included here is intended to act as part of the model:</p>
 * <b>Credentials</b> are included as an extended subclass neededing access between methods 
 * of the credentials, the connection (to test the credentials), and the UI (to request
 * new credentials from the user).  This does not really belong here and should at some
 * time be removed.
 * </p><p>
 * The way it stands the progression though: checking credentials &#62 possibly getting new 
 * credentials &#62 checking credentials &#62 fetch "account.courses_of_interest" &#62
 * bouncing on the course path redirects &#62 "course.get" is all asyncronous and rather 
 * impossible to stop once started as one thing often leads directly to the next.
 * The next evolution should involve listeners (probably here for now, near the
 * [controler/activity]) at each joint to allow more interesting control of the progression.
 * In anycase, a progression usually consists of a static path through methods, and 
 * thereby new paths should be easily enough generated from subclasses of the Async classes
 * in {@link Connection} (JSONPost, JSONGet, and grabPage) wherein onPostExecute can be 
 * implemented to the desired next turn.</p>
 *
 * @author Evan J Brunner
 */
public class Udacity extends FragmentActivity implements 
			      UserInterface.CredentialsDialog.OnNewCredentialsListener
{
  /**Tag for credentials dialog as per {@link FragmentTransaction.add}*/
  private static final String CREDENTIALS_DIALOG = "00";
  /**Enable log noise*/
  private static final boolean DEBUG = true;

  private static UdacityUserInterface ui;
  private static UdacityCredentials siCred;
  private static UdacityCourseList course_list;
  private static UdacityConnection uConn;
 
  /**Test vs "production"…*/
  //private static final String UDACITY_URL = "http://www.udacity.com";
  private static final String UDACITY_URL = "http://10.0.0.100:3000";

 /**  Extension of the SignInCredentials.
   *  This was done to allow the credentials to stop the current ASYNC
   *  waterfall, and bark at the user should they not be valid when 
   *  they're asked for.
   */
  private class UdacityCredentials extends SignInCredentials {
    //These should be an enumeration
    public static final String NULL_EMAIL_MSG = "email";
    public static final String NULL_PASSWORD_MSG = "Password";

    /**
     * Gets email address if there is one, if there is not throws an
     * exception and asks the user to provide one.
     *
     * @return String user's [email address/login]
     * @throws NullCredentialsException if credentials are invalid
     */
    @Override
    public String getEmail() throws NullCredentialsException {
      if (this.email == null) {
	//get new credentials and restart the ASYNC waterfall
	promptForCredentials();
	//Kill the old waterfall
	throw new NullCredentialsException(NULL_EMAIL_MSG);
      }
      return this.email; 
    }
    /**
     * Gets password if there is one, if there is not throws an
     * exception and asks the user to provide one.
     *
     * @return String user's password
     * @throws NullCredentialsException if credentials are invalid
     */
    @Override 
    protected String getPassword() throws NullCredentialsException {
      if (this.password == null) {
	//get new credentials and restart the ASYNC waterfall
	promptForCredentials();
	//Kill the old waterfall
	throw new NullCredentialsException(NULL_PASSWORD_MSG);
      }
      return this.password;
    }
  }

  /**
   *  Connection subclass.  This is here mainly to check the state of 
   *  connectivity before attempting to connect to the target web address.
   *
   */
  private class UdacityConnection extends Connection {
    /**base URL used for all connections*/
    protected final URL url = new URL(UDACITY_URL);
    /**
     * Loads URL from arguments and tests the availability 
     * of any data conneciton(Wifi, 3g, etc)
     * 
     * @throws MalformedURLException this should never happen as url is static in code
     */ 
    public UdacityConnection(String url) throws MalformedURLException {
      super(new URL(url));
      if (!connectionAvailable()) 
	//TODO If there is no conneciton available, we should
	// encourage the user to create a connection and retry,
	// or quit. As it is, this just causes a application fail.
	if(DEBUG) Log.w("Udacity.UdacityConnection()", "No WAN connection found.");
    }
   
    /**
     *  Tests the availability of a vaild date connection
     *  @return boolean true if a connection is available, false otherwise.
     */
    @Override
    protected boolean connectionAvailable() {
      ConnectivityManager connMgr = 
	(ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
      NetworkInfo netfo = connMgr.getActiveNetworkInfo();
      return ( netfo != null && netfo.isConnected() );
    }
    /**
     *  Astracted method to snatch the user credentials for log-in.
     *  This was quicker than implementing a listener interface, and while
     *  Connection was already subclassed here, provided a convinient way to 
     *  snatch the credentials.
     *
     *  @return JSONObject representing the user credentials appropriate for login
     */
    @Override
    protected JSONObject getJSONCredentials() {
      return siCred.toJSON();
    }
  }

  /**
   *  Subclass of UserInterface allows views to build against the activity context.
   *  Added an array of interfaces to listen for events and update the UI appropriately.
   *  
   *
   */
  private class UdacityUserInterface extends UserInterface 
				     implements UdacityCourseList.OnCourseListChangeListener,
					SwipeAdapter.OnListClickListener {
    /**
     * Starts the User Interface.
     * Implemtned in the activity context so that it can just 
     * grab the appropriate layouts from the environment.
     *
     */
    public UdacityUserInterface() {
      super();
      setContentView(R.layout.udacity);
      addRefreshClickable();
      setRootView(findViewById(R.id.main));
    }

    /**
     * Build refresh button (used as part of instantiaion).
     * When the UI is rebuilt this will grab the refresh icon and 
     * attach the appropriate functionality.
     */
    private void addRefreshClickable(){
      ImageView refreshImg = (ImageView)findViewById(R.id.refresh);
      refreshImg.setClickable(true);
      refreshImg.setOnClickListener( new OnClickListener() {
	/**rebuilds the UI but supplanting the appropriate objects*/
	@Override
	public void onClick(View v) {
	  createNewUI();
	  createNewCourseList((UdacityCourseList.OnCourseListChangeListener)getUI());
	  uConn.addOnConnectionReadyListener(
			      (Connection.OnConnectionReadyListener)course_list);
	  uConn.checkCredentials();
	}
      });
    }

    /**
     * Get the swipe adapter for this application, if there isn't one
     * build an new one and return it.  This is done here instead of in the 
     * constructor because the course_list will be null until we query the 
     * server for the account's interested courses, which comes after showing
     * the UI for the first time.  This could me moved around if different 
     * loading schemes are desired (ie incremental)
     */
    public SwipeAdapter getSwipeAdapter() {
      SwipeAdapter retAdapter;
      if((retAdapter = this.swipeAdapter) == null) {
	retAdapter = new SwipeAdapter(getSupportFragmentManager(), course_list); 
	retAdapter.addOnListClickListener(
		    (SwipeAdapter.OnListClickListener)UdacityUserInterface.this);
	setSwipeAdapter(retAdapter);
      }
      return retAdapter;
    }

    /**
     * OnList click interface.
     * When an item on the list is clicked, flip the page.
     */
    public void onListClick(int item, int page) {
      if(DEBUG) Log.i("Udacity.UdacityUserInterface.onListClick",
				  "item clicked:"+item); 
      getPager().setCurrentItem(page+1);
      getSwipeAdapter().notifyDataSetChanged();
    }

    /**
     * OnCourseListChange interface.
     * If a new course list is loaded, notify the swipe adapter. 
     */
    public void onCourseListChange() {
      getSwipeAdapter().notifyDataSetChanged();
    }
  }

/** Activity Init */
  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    createNewUI();
    createNewCredentials();
    createNewCourseList((UdacityCourseList.OnCourseListChangeListener)getUI());
    createNewConnection((Connection.OnConnectionReadyListener)getCourseList(),
			(Connection.OnNewCsrfTokenListener)getCredentials());
    //Start intial async waterfall
    uConn.checkCredentials();
  }
  /**
   * Build and show credentials dialog.
   * When the dialog is closed it will notify a listener with 
   * the new credentials see {@link UserInterface.CredentialsDialog}
   *
   */
  public void promptForCredentials() {
    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
    DialogFragment cD = UserInterface.getCredentialsDialog();
    cD.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
    cD.show(ft, CREDENTIALS_DIALOG);
  }
  /**
   * onNewCredentials interface, called when 
   * {@link UserInterface.CredentialsDialog} returns. New user 
   * credentials from the UI usually mean that there were no
   * previous credentials, or that the previous credentials 
   * failed on the server.  
   *
   * The new credentials are set, and then a the connection is
   * told to fetch a new cookie, which restarts the course populating
   * waterfall.
   *
   * @param email - new email credential
   * @param pass -new password credential
   */
  public void onNewCredentials(String email, String pass) {
    siCred.setEmail(email);
    siCred.setPassword(pass);
    uConn.fetchNewCookie();
    if (DEBUG) Log.w("Udacity.onNewCredentials()::","email:: "+email);
    //Dismiss the dialog.
    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
    DialogFragment prev = 
      (DialogFragment)getSupportFragmentManager().findFragmentByTag(CREDENTIALS_DIALOG);
    if(prev != null) {
      prev.dismiss();
    }
  }
  /**
   * Builds a new user interface for this activity.
   */
  public void createNewUI() {
    this.ui = new UdacityUserInterface();
  }
  /**
   * Builds new credentials for thie activity.
   */
  public void createNewCredentials() {
    this.siCred = new UdacityCredentials();
  }
  /**
   * Builds new course list for this activity.
   * @param cL Listener if anyone cares, null if not.
   *				     	    Called when the course list hierarchy is changed.
   */
  public void createNewCourseList(UdacityCourseList.OnCourseListChangeListener cL) {
    this.course_list = 
	  new UdacityCourseList(cL);
  }
  /**
   * Builds new connection for this activity. 
   * @param cL Listener if anyone cares, null if not.
   *        		 called when connection has successfully validated credentials.
   * @param tL Listener if anyone cares, null if not.
   *			Called when a new CSRF_TOKEN item is found on a loaded page.
   */
  public void createNewConnection(Connection.OnConnectionReadyListener cL, 
				    Connection.OnNewCsrfTokenListener tL) {
   try {
      this.uConn = new UdacityConnection(UDACITY_URL);
      this.uConn.setCsrfTokenListener(tL);
      if(course_list != null) this.uConn.addOnConnectionReadyListener(cL);
    } catch (MalformedURLException e){
      //This can die queitly because the URL is static
      // and this should never happen.
      if(DEBUG)Log.w("Udacity.Udacity.onCreate()", "Malformed URL:: " + e);
    }
  }
  /**
   * Gets the current UI if there is one.
   * @return UdacityUserInterface if there currently is one associated with the activity,
   *         else null.
   */
  public UdacityUserInterface getUI() {
    return this.ui;
  }
  /**
   * Gets the course list if there is one.
   * @return UdacityCourseList if there currently is one associated with the activity,
   *         else null.
   */
  public UdacityCourseList getCourseList() {
    return this.course_list;
  }
  /**
   * Gets the current credentials object if there is one.
   * @return UdacityCredentials if there currently is one associated with the activity,
   *         else null.
   */
  public UdacityCredentials getCredentials() {
    return this.siCred;
  }
}
