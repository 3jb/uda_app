package com.appittome.udacity.client;

import android.app.Activity;
import android.os.Bundle;

import android.support.v4.app.DialogFragment;
import android.support.v4.app.ListFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.FragmentStatePagerAdapter;


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

import org.json.JSONObject;

public class UserInterface 
{
  private static final int SWIPE_DEPTH = 3;
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
      //if(DEBUG)((EditText)v.findViewById(R.id.pass)).setText("");
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
/**Swipe view**/
  public static class SwipeAdapter extends FragmentStatePagerAdapter {
    public SwipeAdapter(FragmentManager fm) {
      super(fm);
    }
    @Override
    public int getCount() {
      return SWIPE_DEPTH;
    }

    @Override public Fragment getItem(int position) {
      return ArrayListFragment.newInstance(position);
    }
  }

  public static class ArrayListFragment extends ListFragment {
    
    private String[] values;

    static ArrayListFragment newInstance(int num) {
      ArrayListFragment f = new ArrayListFragment();

      Bundle args = new Bundle();
      args.putInt("num", num);
      f.setArguments(args);

      return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      int num = getArguments() != null ? getArguments().getInt("num") : 1;
      if (num%2 != 0) {
	values = new String[] {};
      } else {
	values = new String[] {"one", "two"};
      }
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
			      Bundle savedInstanceState) {
      return inflater.inflate(R.layout.fragment_pager, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
      super.onActivityCreated(savedInstanceState);
      setListAdapter( new CourseArrayAdapter(getActivity(), new JSONObject[1]));
		//new ArrayAdapter<String>(getActivity(),
                          //R.layout.fragment_class_item, 
			  //R.id.class_name, values));
    }
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
      Log.i("FragmentList", "Item clicked: " + id);
    }
  }
}
