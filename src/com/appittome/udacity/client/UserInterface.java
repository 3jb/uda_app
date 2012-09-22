package com.appittome.udacity.client;

import android.app.Activity;
import android.os.Bundle;

import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.EditText;

import android.view.View.OnClickListener;

import android.util.Log;

import android.widget.ListView;
import android.widget.ArrayAdapter;

public class UserInterface 
{

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
      View v = inflater.inflate(R.layout.cred_req, container, false);
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

    /*ListView listV = (ListView) activity.findViewById(R.id.list);

    String[] values = new String[] {"one", "two"};

    ArrayAdapter<String> adapter = new ArrayAdapter<String>(activity,
     R.layout.classes, R.id.class_name, values);

    listV.setAdapter(adapter); 
/*
    Button signInBut = (Button)activity.findViewById(R.id.sign_in);
    signInBut.setOnClickListener( new OnClickListener() {
      public void onClick(View v) {
	showMessage("CLICK");
      }
    });*/



/*  public JSONObject collectForSubmission() {
    JSONObject retObj = new JSONObject();
    JSONObject data = new JSONObject();
    try {
      //User relavant attibutes
      data.put("email",
	       ((EditText)activity.findViewById(R.id.email)).getText().toString());
      data.put("password", 
	       ((EditText)activity.findViewById(R.id.pass)).getText().toString());
      retObj.put("data", data);
      //Client relavant attibutes
      retObj.put("method","account.sign_in");
      retObj.put("version","dacity-45");
      retObj.put("csrf_token","123123");
    } catch (JSONException e) {
      showMessage("Difficulty assembling JSON object");
      Log.w("collectForSubmission()", e);
    }
    return retObj;
  }
*/
}
