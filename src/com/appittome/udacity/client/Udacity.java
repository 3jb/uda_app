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

public class Udacity extends FragmentActivity implements 
			      Connection.OnNewCsrfTokenListener,
			      UserInterface.CredentialsDialog.OnNewCredentialsListener
{
  private static final String CREDENTIALS_DIALOG = "00";
  private static final boolean DEBUG = true;

  private static UserInterface ui;
  private static SignInCredentials siCred;
  private static UdacityCourseList course_list;
  private static Connection uConn;
  private static String current_token;
 
  //Test and "production"…
  //private static final String UDACITY_URL = "http://www.udacity.com";
  private static final String UDACITY_URL = "http://10.0.0.7:3000";
  
  private class UdacityCredentials extends SignInCredentials {
    public static final String NULL_EMAIL_MSG = "email";
    public static final String NULL_PASSWORD_MSG = "Password";
    public UdacityCredentials() {
	super();
      }

      @Override
      public String getEmail() {
	if (this.email == null) {
	  promptForCredentials();
	  throw new NullCredentialsException(NULL_EMAIL_MSG);
	}
	return this.email; 
      }
      @Override 
      protected String getPassword() {
	if (this.password == null) {
	  promptForCredentials();
	  throw new NullCredentialsException(NULL_PASSWORD_MSG);
	}
	return this.password;
      }
      @Override
      protected String getCsrf_token() {
	return current_token;
      }
  }

  private class UdacityConnection extends Connection {
    protected final URL url = new URL(UDACITY_URL);
    public UdacityConnection(String url) throws MalformedURLException {
      super(new URL(url));
      if (!connectionAvailable()) 
	//TODO If there is no conneciton available, we should
	// encourage the user to create a connection and retry,
	// or quit.
	if(DEBUG) Log.w("Udacity.UdacityConnection()", "No WAN connection found.");
    }
   
    @Override
    protected boolean connectionAvailable() {
      ConnectivityManager connMgr = 
	(ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
      NetworkInfo netfo = connMgr.getActiveNetworkInfo();
      return ( netfo != null && netfo.isConnected() );
    }
    @Override
    protected JSONObject getJSONCredentials() {
      return siCred.toJSON();
    }
  }

  private class UdacityUserInterface extends UserInterface 
				     implements UdacityCourseList.OnCourseListChangeListener,
					SwipeAdapter.OnListClickListener
  {
    private SwipeAdapter swipeAdapter;

    public UdacityUserInterface() {
      super();
      setContentView(R.layout.udacity);
      addRefreshClickable();
    }
    private void addRefreshClickable(){
      ImageView refreshImg = (ImageView)findViewById(R.id.refresh);
      refreshImg.setClickable(true);
      refreshImg.setOnClickListener( new OnClickListener() {
	@Override
	public void onClick(View v) {
	  createNewUI();
	  createNewCourseList();
	  uConn.addOnConnectionReadyListener(
			      (Connection.OnConnectionReadyListener)course_list);
	  uConn.checkCredentials();
	}
      });
    }
    @Override
    public void showMessage(String msg) {
	Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }
    public ViewPager getPager() {
      return  (ViewPager)findViewById(R.id.pager);
    }
    public void setSwipeAdapter(SwipeAdapter sa) {
      this.swipeAdapter = sa;
    }
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
    public void onListClick(int item, int page) {
      if(DEBUG) Log.i("Udacity.UdacityUserInterface.onListClick",
				  "item clicked:"+item); 
      getPager().setCurrentItem(page+1);
      getSwipeAdapter().notifyDataSetChanged();
    }
	
    public void onCourseListChange() {
      if(DEBUG) Log.i("Udacity.UdacityUserInterface.onCourseChange", 
		      "course_list == nul? " + (course_list == null ? "true":"false"));
      //ViewPager pager = (ViewPager)findViewById(R.id.pager);
      //apparently the query can beat the UI coming up, so make sure
      //this is not null before trying to set pager.
      if(getPager() != null && this.swipeAdapter == null) {
	getPager().setAdapter(getSwipeAdapter());
      } else {
	getSwipeAdapter().notifyDataSetChanged();
      }
    }
  }

/*** Activity Init ***/
  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    createNewUI();
    createNewCredentials();
    createNewCourseList();
    createNewConnection();
    uConn.checkCredentials();
  }
  @Override
  public void onResume()
  {
    super.onResume();
    //TODO this refreshes far too often…
    // should figure out how old the cookie is and 
    // see if refresh necessary 
    //uConn.checkCredentials();
  }

  public void promptForCredentials() {
    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
    DialogFragment cD = UserInterface.getCredentialsDialog();
    cD.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
    cD.show(ft, CREDENTIALS_DIALOG);
  }

  public void onNewCsrfToken(String token) {
    this.current_token = token;
  }

  public void onNewCredentials(String email, String pass) {
    siCred.setEmail(email);
    siCred.setPassword(pass);
    uConn.fetchNewCookie();
    Log.w("Udacity.onNewCredentials()::","email:: "+email+" password:: ");
    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
    DialogFragment prev = 
      (DialogFragment)getSupportFragmentManager().findFragmentByTag(CREDENTIALS_DIALOG);
    //TODO is remove necessary?
    ft.remove(prev);
    //ft.addToBackStack(null);
    prev.dismiss();
  }
  public void createNewUI() {
    this.ui = new UdacityUserInterface();
  }
  public void createNewCredentials() {
    this.siCred = new UdacityCredentials();
  }
  public void createNewCourseList() {
    this.course_list = 
	  new UdacityCourseList((UdacityCourseList.OnCourseListChangeListener)ui);
  }
  public void createNewConnection() {
   try {
      this.uConn = new UdacityConnection(UDACITY_URL);
      this.uConn.setCsrfTokenListener(this);
      if(course_list != null) this.uConn.addOnConnectionReadyListener(
			      (Connection.OnConnectionReadyListener)course_list);
    } catch (MalformedURLException e){
      //This can die queitly because the URL is static
      // and this should never happen.
      if(DEBUG)Log.w("Udacity.Udacity.onCreate()", "Malformed URL:: " + e);
    }
  }
}
