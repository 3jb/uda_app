package com.appittome.udacity.client;

import android.app.Activity;
import android.os.Bundle;
import android.content.Context;
import android.app.AlertDialog;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.EditText;
import android.view.View.OnClickListener;
import android.content.DialogInterface;
import android.app.DialogFragment;
import android.app.Dialog;
import android.util.Log;
import java.net.URL;
import java.net.MalformedURLException;
import org.json.JSONObject;
import org.json.JSONException;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import java.lang.Thread;
import java.lang.Runnable;
import java.lang.InterruptedException;

public class UdaUserInterface 
{

  private Activity activity;

  private static class AlertDialogFragment extends DialogFragment {
    public static AlertDialogFragment newInstance(int title) {
      AlertDialogFragment frag = new AlertDialogFragment();
      Bundle args = new Bundle();
      args.putInt("title", title);
      frag.setArguments(args);
      return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState)
    {
      int title = getArguments().getInt("title");
      return new AlertDialog.Builder(getActivity())
	      .setTitle("Lovely")
	      .setPositiveButton("ok",
		new DialogInterface.OnClickListener() {
		  public void onClick(DialogInterface dialog, int whichButton) {
		    ((udacity)getActivity()).doPositiveClick();
		  }
		}
	      )
	      .create();
    }
  }

  public  UdaUserInterface(Activity activity)
  {
    this.activity = activity;
    activity.setContentView(R.layout.class_list);

    ListView listV = (ListView) activity.findViewById(R.id.list);

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
  }



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
/*** Dailog ***/
    void showDialog() {
      DialogFragment newFragment = AlertDialogFragment.newInstance(R.string.alert_title);
      newFragment.show(activity.getFragmentManager(), "Like a Rose");
    }

    public void doPositiveClick() {
      Log.i("FragmentAlertDialog", "POSITIVE CLICK!");
    }
    public void showMessage( String msg ){
      ((TextView)activity.findViewById(R.id.status)).setText(msg);
    }
}
