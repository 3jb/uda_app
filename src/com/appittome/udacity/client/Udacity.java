package com.appittome.udacity.client;

import android.app.Activity;
import android.os.Bundle;
import android.net.NetworkInfo;
import android.net.ConnectivityManager;
import android.content.Context;
import android.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Toast;      
import android.view.View.OnClickListener;
import android.content.DialogInterface;
import android.app.DialogFragment;
import android.app.Dialog;
import android.util.Log;
import java.net.URL;
import java.net.MalformedURLException;
import org.json.JSONObject;
import org.json.JSONException;

public class Udacity extends Activity
{
  private static UdaUserInterface ui;
  private static SignInCredentials siCred;
  private static Connection uConn;
  private static String UDACITY_URL = "http://192.168.1.8:3000";
  
  private class UdacityCredentials extends SignInCredentials {
    public UdacityCredentials() {
	super();
      }

      @Override
      public String getEmail() {
	ui.showMessage("");
	return this.email;
      }
      @Override 
      protected String getPassword() {
	return this.password;
      }
      @Override
      protected String getCsrf_token() {
	return this.csrf_token;
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
	Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
    }
    @Override
    protected JSONObject getJSONCredentials() {
      return new JSONObject();
      //return siCred.toJSON();
    }
  }

/*** Activity Init ***/
  @Override
  public void onCreate(Bundle savedInstanceState)
  {
    super.onCreate(savedInstanceState);
    this.ui = new UdaUserInterface(this);
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

}
