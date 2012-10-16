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

import com.udacity.api.CourseRev;
/**
 * Custom array adapter to represent unit in listFragment.
 * Shows unit name, and it's [number/position] in the unit list.
 */
public class UnitArrayAdapter extends ArrayAdapter<CourseRev.Unit> {
  /** debug log switch*/
  private static final boolean DEBUG = true;
  private static final int COURSE_ITEM_LAYOUT = R.layout.fragment_unit_item;
  private static final int ID_VIEW = R.id.unit_id;
  private static final int NAME_VIEW = R.id.unit_name;
  private LayoutInflater mInflater;

  public UnitArrayAdapter(Activity context, List<CourseRev.Unit> unit_list) {
    super(context, COURSE_ITEM_LAYOUT, unit_list);
    mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent){
    if(DEBUG)Log.i("Udacity.UnitArrayAdapter.getView","position == " + position);
    View view;
    //TODO not a big fan of this - but… it'll work for now
    if (convertView == null) {
      if(DEBUG)Log.i("Udacity.UnitArrayAdapter.getView","init");
      view = mInflater.inflate(COURSE_ITEM_LAYOUT, parent, false);
    } else {
      view = convertView;
    }
    TextView name = (TextView) view.findViewById(NAME_VIEW);
    TextView id = (TextView) view.findViewById(ID_VIEW);
    CourseRev.Unit unit = getItem(position);
    try {
      id.setText(Integer.toString(position+1));//unit.getId());
      name.setText(unit.getName());
    } catch (Exception e) {
      if(DEBUG)Log.w("Udacity.UnitArrayAdapter.getView", 
	      "Course seems to be missing fields::" + e);
    }
    return view;
  }
}
