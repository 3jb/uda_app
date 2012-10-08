package com.appittome.udacity.client;

import android.app.Activity;
import android.os.Bundle;

import android.support.v4.app.DialogFragment;

import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.view.View.OnClickListener;

import android.widget.Button;
import android.widget.TextView;
import android.widget.EditText;

import android.util.Log;

public class UserInterface 
{
  private static final boolean DEBUG = true;
  public static class CredentialsDialog extends DialogFragment {
    private static OnNewCredentialsListener cL;
    private static String email;
    private static String password;

    public interface OnNewCredentialsListener
    {
      public void onNewCredentials(String email, String pass);
    }

    @Override 
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
    }
    @Override
    public View onCreateView(LayoutInflater inflater, 
			      ViewGroup container, Bundle savedInstanceState) {
      View v = inflater.inflate(R.layout.credential_request, container, false);
      if(DEBUG)((EditText)v.findViewById(R.id.email)).setText("ejbrunner@gmail.com");
      if(DEBUG)((EditText)v.findViewById(R.id.pass)).setText("moxyu4S");
      Button b = (Button)v.findViewById(R.id.sign_in);
      b.setOnClickListener( new OnClickListener() {
	      public void onClick(View v) {
		View rv = v.getRootView();
		cL.onNewCredentials(
		      ((EditText)rv.findViewById(R.id.email)).getText().toString(),
		      ((EditText)rv.findViewById(R.id.pass)).getText().toString());
	      }});
      return v;
    }
    @Override
    public void onAttach(Activity activity) {
      super.onAttach(activity);
      try{
	this.cL = (OnNewCredentialsListener) activity;
      } catch (ClassCastException e) {
	throw new ClassCastException( 
		    activity.toString() + " must implement OnNewCredentialsListener.");
      }
    }
  }

  public static DialogFragment getCredentialsDialog() {
    return (DialogFragment)new CredentialsDialog();
  }

  public void showMessage(String msg) {
  }

}
