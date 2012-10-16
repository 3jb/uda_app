package com.appittome.udacity.client;

import android.os.Bundle;

import android.support.v4.app.Fragment;
import android.support.v4.app.ListFragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.FragmentStatePagerAdapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
  
import android.util.Log;

import android.widget.ListView;
import android.widget.ArrayAdapter;

import java.util.List;
import java.util.HashSet;
import java.util.Iterator;

import com.udacity.api.CourseRev;
/**
 * UI object that manages flipping between lists - this is a pager that 
 * responds to swipe actions.  Each click on a list item in the pager
 * generates the appropriate set of pages for the swipe adapter.
 */
public class SwipeAdapter extends FragmentStatePagerAdapter {
  /** Debug log switch */
  private static final boolean DEBUG = true;
  /** undelying model state*/
  private static UdacityCourseList course_list;
  private static UdacityCourseList.Course current_course;
  private static CourseRev.Unit current_unit;

  private static HashSet<OnListClickListener> listClickListeners = 
					  new HashSet<OnListClickListener>();

  /**
   * Super class requires android.support.v4.app.FragmentManager. 
   * @param fm FragmentActivity FragmentManager
   * @param course_list the course list representing the state of the graph
   *                    represented by this UI component.
   */
  public SwipeAdapter(FragmentManager fm, UdacityCourseList course_list) 
  {
    super(fm);
    this.course_list = course_list;
    this.current_course = null;
    this.current_unit = null;
  }
  /**
   * Gets the number of pages currently available.  Overriding this method 
   * allows this class to vary the number of pages available for swiping based
   * on the current state, which is changed on user picking items from the list.
   */
  @Override
  public int getCount() {
    return (current_course == null ? 1 : current_unit == null ? 2 : 3);
  }
  /**
   * Get the fragment for the current page to be shown.
   */
  @Override public Fragment getItem(int position) {
    return UdacityListFragment.newInstance(position);
  }
  /**
   * Implemented to force the rebuilding of all pages on a 
   * notifyDataSetChanged event.  All that really has to be 
   * updated are nodes that are lower than the current in the 
   * course graph(pages to the right), but this was quick and 
   * easy, and doesn't seem to slow the app down all that much.
   */
  @Override public int getItemPosition(Object obj){
    return POSITION_NONE;
  }
  /** 
   * Interface to register for click events on the pager's lists.
   */
  public interface OnListClickListener {
    /**
     * Called each time an item on a ListAdapter is clicked.
     * @param item index of item clicked.
     * @param page index of page on which item was clicked.
     */
    public void onListClick(int item, int page);
  }
  /**
   * Add a new listener to this object.
   * @param l listener to be added.
   * @return true if this listener was not already in the set @see java.util.HashSet
   */
  public boolean addOnListClickListener(OnListClickListener l) {
    return this.listClickListeners.add(l);
  }
  /**
   * Notify all listeners of this object that a item in the current 
   * listAdapter has been cliked on.
   * @param i index of item clicked within list
   * @param p index of page of item clicked.
   */
  protected static void notifyOnListClickListeners(int i, int p) {
    Iterator<OnListClickListener> listIter = listClickListeners.iterator();
    while(listIter.hasNext()){
      listIter.next().onListClick(i,p);
    }
  }
  /**
   * Returns the current Unit selected by the user as saved in the state
   * of this adapter.
   * @return current unit selected
   */
  public CourseRev.Unit getCurrentUnit() {
    return current_unit;
  }
  /**
   * ListFragment object to populate for each page of the swipeAdapter.
   * This object forms the lists that are swiped through in the swipeAdapter,
   * or "pages" of the view.
   */
  public static class UdacityListFragment extends ListFragment {
    private int type;
    /**
     * Funky little quick-load trick to keep the object state fresh.
     * These pages are pretty quickly disposed of, and then reinstantiated
     * constantly when a 'page' is shown or not.  Thus we want to build them
     * as quickly as possible.  The bundles save the state for quicker access.
     * @param type the type of page to instantiate - course(0), unit(1) or nugget(2)
     */
    static UdacityListFragment newInstance(int type) {
      UdacityListFragment f = new UdacityListFragment();

      Bundle args = new Bundle();
      args.putInt("type", type);
      f.setArguments(args);

      return f;
    }
    //Overriden methods to create desired behavior for this fragment
    @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      type = getArguments() != null ? getArguments().getInt("type") : 1;
    }
    @Override 
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
			      Bundle savedInstanceState) {
      return inflater.inflate(R.layout.fragment_pager, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
      super.onActivityCreated(savedInstanceState);
      switch(type) {
	case 0:	viewCourseList();
		Log.i("Udacity.SwipeAdapter.onActivityCreated()","one");
		break;
	case 1: viewUnitList();
		Log.i("Udacity.SwipeAdapter.onActivityCreated()","two");
		break;
	case 2: viewNuggetList();
		Log.i("Udacity.SwipeAdapter.onActivityCreated()","default");
		break;
	default: viewCourseList();
		break;
      }
    }
    /**
     * Instantiate a course list, consisting of a list of courseArrayAdapters
     * with the data from course_list.
     */
    private void viewCourseList() {
	setListAdapter( new CourseArrayAdapter(getActivity(),course_list));
    }
    /**
     * Instantiate a unit list, consisting of a list of unitArrayAdapters
     * with data from the current selected unit.
     */
    private void viewUnitList() {
	setListAdapter( new UnitArrayAdapter(getActivity(), 
		current_course.getUnitList()));
    }
    /**
     * Instantiate a nugget list, consisting of a list of NuggetArrayAdapters
     * with data from current selected unit.
     */
    private void viewNuggetList() {
	if(DEBUG)Log.i("Udacity.SwipeAdapter.UdacityListFragment","first unit nuggets: "+
		current_course.getUnitNugList(current_unit.getKey()).size());
	setListAdapter( new NuggetArrayAdapter(getActivity(), 
		current_course.getUnitNugList(current_unit.getKey())));
    }
    /** 
     * Return the current type of this listFragment
     * @return an integer representing the type of this object, and it's position
     *         in the view 0 - course list, 1 - unit list, 2 - nugget list.
     */
    public int getType() {
      return type;
    }
    /**
     * Sets the current course, usually after user clicks appropriate list item.
     * @param c course object represented by click.
     */
    public void setCurrentCourse(UdacityCourseList.Course c) {
      current_course = c;
    }
    /**
     * Gets the current selected in the state of the swipeAdapter
     * @return most recent course selected in state of swipeAdapter
     */
    public UdacityCourseList.Course getCourse(int index) {
      return course_list.get(index);
    }
    /**
     * Sets the current unit selected in the swipeAdapter state
     * @param u Unit object to be set as selected.
     */
    public void setCurrentUnit(CourseRev.Unit u) {
      current_unit = u;
    }
    /**
     * Gets the unit selected in the current swipeAdapter
     * @return most recent unit selected by user
     */
    public CourseRev.Unit getUnit(int index) {
      return current_course.getUnitList().get(index);
    }
    /**
     * Based on the current state of the swipeAdapter, and the index in the
     * list clicked - set the current selected state appropriately.
     */
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
      Log.i("FragmentList", "Item clicked: postition ==" + position);
      switch(getType()) {
	case 0: setCurrentCourse(getCourse(position));
		setCurrentUnit(null);
		notifyOnListClickListeners(position, getType());
		//urrent_course = course_list.get(position);
		break;
	case 1: setCurrentUnit(getUnit(position));
		notifyOnListClickListeners(position, getType());
		//current_unit = current_course.getUnitList().get(position);
		break;
	default: //shouldn't happen, but if it does - forgetaboutit
		break;
      }
    }
  }
}
