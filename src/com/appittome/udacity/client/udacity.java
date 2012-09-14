package com.appittome.udacity.client;

import android.app.Activity;
import android.os.Bundle;
import android.net.*;
import android.content.Context;
import android.os.AsyncTask;
import java.net.*;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.BufferedOutputStream;
import java.io.Reader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
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
import org.json.JSONObject;
import org.json.JSONException;

public class udacity extends Activity
{

  public static class AlertDialogFragment extends DialogFragment {
    public static AlertDialogFragment newInstance(int title) {
      AlertDialogFragment frag = new AlertDialogFragment();
      Bundle args = new Bundle();
      args.putInt("title", title);
      frag.setArguments(args);
      return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
      int title = getArguments().getInt("title");

      return new AlertDialog.Builder(getActivity())
	      //.setIcon(R.drawable.alert_dialog_icon)
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

  private class AuthenticateTask extends AsyncTask<URL, Integer, String>
  {
    @Override
    protected String doInBackground(URL... urls){
      String retVal = "";
      try{
	retVal = doAuth(urls[0]);
      } catch (IOException e) {
	retVal =  "Unable to retreive webpage.  URL may be invalid";
      } finally {
	return retVal;
      }
    }
    @Override
    protected void onPostExecute(String result) {
      try {
	JSONObject JSONResp = new JSONObject(result);
	try {
	  String reply = JSONResp.getString("win");
	  showMessage(reply);
	  if( reply.compareTo("loaded cookie") == 0 ) {
	    showDialog();
	    try {
	      new ReloadWithCredentials().execute(new URL("http://192.168.1.11:3000"));
	    } catch  (MalformedURLException e) {
	      Log.w("AuthenticateTask", e);
	    }
	  }
	} catch (JSONException e) {
	  showMessage(JSONResp.toString(2));
	}
      } catch (JSONException e) {
	showMessage("Invalid JSON returned::" + e);
      }
    }
  }
  private class ReloadWithCredentials extends AsyncTask<URL, Integer, String>
  {
    @Override 
    protected String doInBackground(URL... urls){
     return "THis is FAKES";
    }
    @Override
    protected void onPostExecute(String result) {
      showMessage(result);
    }
  }

/*** Activity Init ***/
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.main);


      Button signInBut = (Button)findViewById(R.id.sign_in);
      signInBut.setOnClickListener( new OnClickListener() {
	public void onClick(View v) {
	  //spawn async task
	  if (getConnection() != null) {
	    try {
	      URL testURL = new URL("http://192.168.1.11:3000/ajax");
	      new AuthenticateTask().execute(testURL);
	    } catch (MalformedURLException e){
	      showMessage("Malformed URL");
	    }
	  } else {
	    showMessage("No available connection");
	  }
	}
      });
    }
/*** ACTIVITY ***/
  public JSONObject collectForSubmission() {
    JSONObject retObj = new JSONObject();
    JSONObject data = new JSONObject();
    try {
      //User relavant attibutes
      data.put("email",
	       ((EditText)findViewById(R.id.email)).getText().toString());
      data.put("password", 
	       ((EditText)findViewById(R.id.pass)).getText().toString());
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

/*** Dailog ***/
    void showDialog() {
      DialogFragment newFragment = AlertDialogFragment.newInstance( 1 );
      newFragment.show(getFragmentManager(), "Like a Rose");
    }

    public void doPositiveClick() {
      Log.i("FragmentAlertDialog", "POSITIVE CLICK!");
    }
    public void showMessage( String msg ){
      ((TextView)findViewById(R.id.title)).setText(msg);
    }

/*** Network ***/
    public String doAuth(URL url) throws IOException {
      InputStream is = null;
      int len = 500;

      try{ 
	String message = collectForSubmission().toString();

	HttpURLConnection conn = (HttpURLConnection) url.openConnection();
	conn.setReadTimeout( 10000 /*milliseconds*/ );
	conn.setConnectTimeout( 15000 /* milliseconds */ );
	conn.setRequestMethod("POST");
	conn.setDoInput(true);
	
	conn.setDoOutput(true);
	Log.i("doAuth", "message:" + message);
	Log.i("doAuth", "StringLength:" + message.length());
	Log.i("doAuth", "byteLength:" + message.getBytes().length);
	conn.setFixedLengthStreamingMode(message.getBytes().length);
	//conn.setChunkedStreamingMode(0);
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
	//Log.i("udactiy::doAuth:", "Request:" + request);

	int response = conn.getResponseCode();
	Log.i("Udacity::AsyncNetwork:", "Response is: " + response);
	is = conn.getInputStream();

	String contentAsString = readIt(is,len);
	return contentAsString;
      } finally {
	if (is !=null) {
	  is.close();
	}
      }
    }

    public String readIt(InputStream stream, int len) throws IOException, 
						 UnsupportedEncodingException {
      Reader reader = null;
      reader = new InputStreamReader(stream, "UTF-8");
      char[] buffer = new char[len];
      reader.read(buffer);
      return new String(buffer);
    }

    public NetworkInfo getConnection()
    {
      ConnectivityManager connMgr = (ConnectivityManager)
	getSystemService(Context.CONNECTIVITY_SERVICE);
      NetworkInfo netfo = connMgr.getActiveNetworkInfo();
      if( netfo != null && !netfo.isConnected() )
	netfo = null;

      return netfo;
    }
}
