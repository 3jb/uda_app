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

public class SwipeAdapter extends FragmentStatePagerAdapter {
  private static final boolean DEBUG = true;
  private static UdacityCourseList course_list;
  private static UdacityCourseList.Course current_course;
  private static CourseRev.Unit current_unit;
  private static HashSet<OnListClickListener> listClickListeners = 
					  new HashSet<OnListClickListener>();

  public SwipeAdapter(FragmentManager fm, UdacityCourseList course_list) 
  {
    super(fm);
    this.course_list = course_list;
    this.current_course = null;
    this.current_unit = null;
  }
  @Override
  public int getCount() {
    return (current_course == null ? 1 : current_unit == null ? 2 : 3);
  }

  @Override public Fragment getItem(int position) {
    return UdacityListFragment.newInstance(position);
  }
  @Override public int getItemPosition(Object obj){
    return POSITION_NONE;
  }
  public interface OnListClickListener {
    public void onListClick(int item, int page);
  }
  public void addOnListClickListener(OnListClickListener l) {
    this.listClickListeners.add(l);
  }
  protected static void notifyOnListClickListeners(int i, int p) {
    Iterator<OnListClickListener> listIter = listClickListeners.iterator();
    while(listIter.hasNext()){
      listIter.next().onListClick(i,p);
    }
  }
  public CourseRev.Unit getCurrentUnit() {
    return current_unit;
  }
  public static class UdacityListFragment extends ListFragment {
    private int type;
    static UdacityListFragment newInstance(int type) {
      UdacityListFragment f = new UdacityListFragment();

      Bundle args = new Bundle();
      args.putInt("type", type);
      f.setArguments(args);

      return f;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      type = getArguments() != null ? getArguments().getInt("type") : 1;
    }

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
    private void viewCourseList() {
	setListAdapter( new CourseArrayAdapter(getActivity(),course_list));
    } 
    private void viewUnitList() {
	setListAdapter( new UnitArrayAdapter(getActivity(), 
		current_course.getUnitList()));
    }
    private void viewNuggetList() {
	Log.i("Udacity.SwipeAdapter.UdacityListFragment","first unit nuggets: "+
		current_course.getUnitNugList(current_unit.getKey()).size());
	setListAdapter( new NuggetArrayAdapter(getActivity(), 
		current_course.getUnitNugList(current_unit.getKey())));
    }
    public int getType() {
      return type;
    }
    public void setCurrentCourse(UdacityCourseList.Course c) {
      current_course = c;
    }
    public UdacityCourseList.Course getCourse(int index) {
      return course_list.get(index);
    }
    public void setCurrentUnit(CourseRev.Unit u) {
      current_unit = u;
    }
    public CourseRev.Unit getUnit(int index) {
      return current_course.getUnitList().get(index);
    }

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
	default: //for now do nothing
		break;
      }
    }
  }
}
