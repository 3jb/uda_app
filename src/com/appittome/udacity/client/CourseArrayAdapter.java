package com.appittome.udacity.client;

import android.content.Context;
import android.app.Activity;
import android.widget.ArrayAdapter;

import android.util.Log;

import android.view.View;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;

import java.net.URL;
import java.io.InputStream;
import java.io.IOException;
import java.util.List;
import android.graphics.drawable.Drawable;

import org.json.JSONObject;
import org.json.JSONException;

/**
 * Array adapter extension that displays Courses in a listFragment with 
 * icon, name and id all visible.
 */
public class CourseArrayAdapter extends ArrayAdapter<UdacityCourseList.Course> {
  /** debug log switch*/
  private static final boolean DEBUG = true;
  private static final int COURSE_ITEM_LAYOUT = R.layout.fragment_course_item;
  private static final int ICON_VIEW = R.id.course_icon;
  private static final int ID_VIEW = R.id.course_id;
  private static final int NAME_VIEW = R.id.course_name;
  private LayoutInflater mInflater;

  public CourseArrayAdapter(Activity context, List<UdacityCourseList.Course> course_list) {
    super(context, COURSE_ITEM_LAYOUT, course_list);
    mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent){
    if(DEBUG) Log.i("Udacity.CourseArrayAdapter.getView","position == " + position);
    View view;
    //TODO not a big fan of this - but… it'll work for now
    if (convertView == null) {
      view = mInflater.inflate(COURSE_ITEM_LAYOUT, parent, false);
      TextView id = (TextView) view.findViewById(ID_VIEW);
      TextView name = (TextView) view.findViewById(NAME_VIEW);
      ImageView icon = (ImageView) view.findViewById(ICON_VIEW);
      UdacityCourseList.Course course = getItem(position);
      try { 
	id.setText(course.getId());
	name.setText(course.getName());
	InputStream is = (InputStream)(new URL(course.getIconURL()).getContent());
	icon.setImageDrawable(Drawable.createFromStream(is, "src"));
      }catch (IOException e) {
	//TODO sub in default image…
	if(DEBUG) Log.w("Udacity.CourseArrayAdapter.getView","Exception "+e); 
      } catch (Exception e) {
	if(DEBUG) Log.w("Udacity.CourseArrayAdapter.getView", 
		"Course seems to be missing fields::" + e);
      }
    } else {
      view = convertView;
    }
    return view;
  }

}

