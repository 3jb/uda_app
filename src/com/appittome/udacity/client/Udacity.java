package com.appittome.udacity.client;

import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;

import android.os.Bundle;
import android.content.Context;

import android.view.View;
import android.widget.Toast;      

import android.util.Log;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import java.net.URL;
import java.net.MalformedURLException;

import org.json.JSONObject;
import org.json.JSONException;

public class Udacity extends FragmentActivity implements 
			      UserInterface.CredentialsDialog.OnNewCredentialsListener
{
  private static final String CREDENTIALS_DIALOG = "00";

  private static UserInterface ui;
  private static SignInCredentials siCred;
  private static Connection uConn;
  private static String UDACITY_URL = "http://192.168.1.8:3000";
  
  private class UdacityCredentials extends SignInCredentials {
    public static final String NULL_EMAIL_MSG = "email";
    public static final String NULL_PASSWORD_MSG = "Password";
    public UdacityCredentials() {
	super();
      }

      @Override
      public String getEmail() {
	/*ui.showMessage("UdacityCredentials.getEmail():: " + this.email);*/
	if (this.email == null) {
	  promptForCredentials();
	  throw new NullCredentialsException(NULL_EMAIL_MSG);
	}
	return this.email; 
      }
      @Override 
      protected String getPassword() {
	/*ui.showMessage("UdacityCredentials.getPassword():: " + this.password);*/
	if (this.password == null) {
	  promptForCredentials();
	  throw new NullCredentialsException(NULL_PASSWORD_MSG);
	}
	return this.password;
      }
      @Override
      protected String getCsrf_token() {
	return "123123123";
	//this.csrf_token;
      }
  }

  private class UdacityConnection extends Connection {
    public UdacityConnection(String url) throws MalformedURLException {
      super(new URL(url));
      if (!connectionAvailable()) 
	//TODO implement intent
	showMessage("No WAN connection found.");
    }
   
    @Override
    protected boolean connectionAvailable() {
      ConnectivityManager connMgr = 
	(ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
      NetworkInfo netfo = connMgr.getActiveNetworkInfo();
      return ( netfo != null && netfo.isConnected() );
    }
    @Override 
    protected void showMessage(String msg) {
	ui.showMessage(msg);
	//Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }
    @Override
    protected JSONObject getJSONCredentials() {
      return siCred.toJSON();
    }
  }

  private class UdacityUserInterface extends UserInterface 
  {
    public UdacityUserInterface() {
      super();
      setContentView(R.layout.udacity);
    }
    @Override
    public void showMessage(String msg) {
	Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }
  }

/*** Activity Init ***/
  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    this.ui = new UdacityUserInterface();
    this.siCred = new UdacityCredentials();
    
   try {
      this.uConn = new UdacityConnection(UDACITY_URL);
    } catch (MalformedURLException e){
      ui.showMessage("Malformed URL:: " + e.getMessage());
    }
  }
  @Override
  public void onResume()
  {
    super.onResume();
    uConn.checkCredentials();
  }

  public void promptForCredentials() {
    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
    DialogFragment cD = UserInterface.getCredentialsDialog();
    cD.setStyle(DialogFragment.STYLE_NO_TITLE, 0);
    cD.show(ft, CREDENTIALS_DIALOG);
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


}
