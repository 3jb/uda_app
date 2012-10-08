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

import com.udacity.api.CourseRev;

public class SwipeAdapter extends FragmentStatePagerAdapter {
  private static final boolean DEBUG = true;
  private static UdacityCourseList course_list;
  private static UdacityCourseList.Course current_course;
  private static CourseRev.Unit current_unit;

  public SwipeAdapter(FragmentManager fm, UdacityCourseList course_list) {
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
    return ArrayListFragment.newInstance(position);
  }

  public static class ArrayListFragment extends ListFragment {
    private int type;
    static ArrayListFragment newInstance(int type) {
      ArrayListFragment f = new ArrayListFragment();

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
	case 0:	setCourseList();
		Log.i("Udacity.SwipeAdapter.onActivityCreated()","one");
		break;
	case 1: setUnitList();
		Log.i("Udacity.SwipeAdapter.onActivityCreated()","two");
		break;
	case 2: setNuggetList();
		Log.i("Udacity.SwipeAdapter.onActivityCreated()","default");
		break;
	default: setCourseList();
		break;
      }
    }
    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
      Log.i("FragmentList", "Item clicked: postition ==" + position);
      switch(type) {
	case 0: current_course = course_list.get(position);
		break;
	case 1: current_unit = current_course.getUnitList().get(position);
		break;
	case 2: //for now do nothing
		break;
      }
    }
    private void setCourseList() {
	setListAdapter( new CourseArrayAdapter(getActivity(),course_list));
    } 
    private void setUnitList() {
	setListAdapter( new UnitArrayAdapter(getActivity(), 
		current_course.getUnitList()));
    }
    private void setNuggetList() {
	setListAdapter( new NuggetArrayAdapter(getActivity(), 
		current_course.getUnitNugList(current_unit.getKey())));
		  
    }
  }
}
