package com.appittome.udacity.client;

import android.app.Activity;
import android.widget.ArrayAdapter;

import android.util.Log;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;

import org.json.JSONObject;
import org.json.JSONException;

public class CourseArrayAdapter extends ArrayAdapter<JSONObject> {

  private static final int COURSE_ITEM_LAYOUT = R.layout.fragment_course_item;
  private static final int ICON_VIEW = R.id.course_icon;
  private static final int ID_VIEW = R.id.course_id;
  private static final int NAME_VIEW = R.id.course_name;

  private static final String COURSE_ICON = "icon_url";
  private static final String COURSE_ID = "id";
  private static final String COURSE_NAME = "name";

  public CourseArrayAdapter(Activity context, JSONObject[] array) {
    super(context, COURSE_ITEM_LAYOUT, array);
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent){
    View view = convertView;
    TextView name = (TextView) view.findViewById(NAME_VIEW);
    TextView id = (TextView) view.findViewById(ID_VIEW);
    ImageView icon = (ImageView) view.findViewById(ICON_VIEW);
    JSONObject course = getItem(position);
    try { 
      course.getString(COURSE_ICON);
      id.setText(course.getString(COURSE_ID));
      name.setText(course.getString(COURSE_NAME));
    } catch (JSONException e) {
      Log.w("Udacity.CourseArrayAdapter.getView", 
	      "Course seems to be missing fields::" + e);
    }
    return view;
  }

}

