package com.appittome.udacity.client;

import android.app.Activity;
import android.os.Bundle;

import android.support.v4.app.DialogFragment;
import android.support.v4.view.ViewPager;

import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.view.View.OnClickListener;

import android.widget.Button;
import android.widget.TextView;
import android.widget.EditText;

import android.util.Log;
/**
 * Utility class for managing parts of the user interface.
 *
 */
public abstract class UserInterface 
{
  /** debug log switch*/
  private static final boolean DEBUG = true;
  /** root view for this UI*/
  private View udacityView;
  /** Adapter managing page changes on swipes etc*/
  protected SwipeAdapter swipeAdapter;
  /** 
   * Dialog used to prompt user for new credentials.  On button press this 
   * dialog notifies the registered listener of a credentials update.
   */
  public static class CredentialsDialog extends DialogFragment {
    private static OnNewCredentialsListener cL;
    private static String email;
    private static String password;
    /**
     * Interface to notify a single listener of a credentials update
     * from the user.
     */
    public interface OnNewCredentialsListener
    {
      /**
       * When the dialog button is pressed with a new set of credentials
       * this listener will be updated with the current credentials.
       * @param email new user email
       * @param pass new user password
       */
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
  /**
   * The swipe adapter needs activity resources to get up and going,
   * so this is expected to be implemented in an activity subclass.
   * @see android.support.v4.app.FragmentActivity
   */
  abstract public SwipeAdapter getSwipeAdapter();
  /**
   * Gets a new credentials dialog to prompt the user for fresh 
   * credentials.
   * @return a fragment ready to be loaded into a transaction.
   */
  public static DialogFragment getCredentialsDialog() {
    return (DialogFragment)new CredentialsDialog();
  }
  /**
   * Sets the root view of this UI.
   * @param v root view of this object
   */
  public void setRootView(View v) {
    this.udacityView = v;
  }
  /**
   * Gets the root view of this UI.
   * @return root view of this object
   */
  public View getRootView() {
    return udacityView;
  }
  /**
   * Gets the pager view from the root view of this object.
   * @return pager view of this UI
   */
  public ViewPager getPager() {
    return  (ViewPager)getRootView().findViewById(R.id.pager);
  }
  /** 
   * Sets a pointer to the current swipeAdapter for this UI
   * @param sa the swipeAdapter to hold reference to.
   */
  public void setSwipeAdapter(SwipeAdapter sa) {
    this.swipeAdapter = sa;
    getPager().setAdapter(this.swipeAdapter);
  }
}
