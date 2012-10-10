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
import android.graphics.drawable.Drawable;

import org.json.JSONObject;
import org.json.JSONException;

import java.util.List;
import java.util.LinkedList;
import java.util.Iterator;

import com.udacity.api.CourseRev;

public class NuggetArrayAdapter extends ArrayAdapter<List<CourseRev.Unit.Nugget>> {

  private static final boolean DEBUG = true;
  private static final int NUGGET_ITEM_LAYOUT= R.layout.fragment_nugget_item;
  private static final int PIECE_ITEM_TEXTVIEW= R.layout.nugget_piece_template;
  private static final int HOR_LAYOUT= R.id.lin_hor;
  private static final int NAME_VIEW = R.id.nugget_name;
  private static final int NUM_VIEW = R.id.nugget_number;
  private static LayoutInflater mInflater;

  public NuggetArrayAdapter(Activity context, List<List<CourseRev.Unit.Nugget>> nug_array) {
    super(context, NUGGET_ITEM_LAYOUT, nug_array);
    mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent){
    Log.i("Udacity.CourseArrayAdapter.getView","position == " + position);
    View view;
    //TODO not a big fan of this - but… it'll work for now
    if (convertView == null) {
      view = mInflater.inflate(NUGGET_ITEM_LAYOUT, parent, false);
    } else {
      view = convertView;
      //stip off the previous nugget's piece list
      ((ViewGroup)view.findViewById(HOR_LAYOUT)).removeAllViews();
    }
    TextView num = (TextView) view.findViewById(NUM_VIEW);	  
    TextView name = (TextView) view.findViewById(NAME_VIEW);
    List<CourseRev.Unit.Nugget> nugList = getItem(position);
    if(DEBUG)Log.i("Udacity.NuggetArrayAdapter.getView","nugList.size " +nugList.size());
    try {
      if(nugList.size() > 0 && nugList.get(0) != null) {
	CourseRev.Unit.Nugget nug;
	Iterator<CourseRev.Unit.Nugget> nugIter = nugList.iterator();
	String firstName = nugList.get(0).getName();
	LinkedList<String> names = new LinkedList<String>();
	String wStr;
	while(nugIter.hasNext()) {
	  nug = nugIter.next();
	  wStr = nug.getName().replace(firstName, "").trim();
	  names.add((wStr.length() == 0 ? nug.getNuggetType() : wStr));
	}
	Iterator<String> nameIter = names.iterator();
	Log.i("Udacity.NuggetArrayAdapter.onView", "names.size()="+names.size());
	ViewGroup pieces = (ViewGroup)view.findViewById(HOR_LAYOUT);
	while(nameIter.hasNext()){
	  TextView piece = (TextView)mInflater
			      .inflate(PIECE_ITEM_TEXTVIEW, pieces, false);
	  pieces.addView(piece);
	  piece.setText(nameIter.next());
	}
	name.setText(nugList.get(0).getName());
	num.setText(Integer.toString(position+1));
      } else { 
	name.setText(":oddity");	
      }
    } catch (Exception e) {
      Log.w("Udacity.CourseArrayAdapter.getView", 
	      "Course seems to be missing fields::" + e);
    }
    //view.setDrawingCacheBackgroundColor(Integer.parseInt("FFFF0000",16));
    return view;
  }

}

