package com.appittome.udacity.client;

import android.content.Context;
import android.app.Activity;
import android.content.Intent;
import android.widget.ArrayAdapter;

import android.util.Log;

import android.view.View;
import android.view.View.OnClickListener;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;

import android.net.Uri;
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
import com.udacity.api.NuggetType;
/**
 * Array adapter that displays a nugget topic group in a listFragment.
 * Shows topic name, and sub-nuggets with appropriate links to youtube 
 * videos.
 */
public class NuggetArrayAdapter extends ArrayAdapter<List<CourseRev.Unit.Nugget>> {
  /** log debug switch */
  private static final boolean DEBUG = true;
  private static final int NUGGET_ITEM_LAYOUT= R.layout.fragment_nugget_item;
  private static final int PIECE_ITEM_TEXTVIEW= R.layout.nugget_piece_template;
  private static final int HOR_LAYOUT= R.id.lin_hor;
  private static final int NAME_VIEW = R.id.nugget_name;
  private static final int NUM_VIEW = R.id.nugget_number;
  private static LayoutInflater mInflater;
  private static Activity context;

  public NuggetArrayAdapter(Activity context, List<List<CourseRev.Unit.Nugget>> nug_array) {
    super(context, NUGGET_ITEM_LAYOUT, nug_array);
    this.mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    this.context = context;
  }
  private String toWord(NuggetType t) {
    String retS = "program";
    switch(t){
      case lecture:
	   retS = "Lecture";
	   break;
      case quiz:
	   retS = "Question";
	   break;
    }
    return retS;
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent){
    if(DEBUG)Log.i("Udacity.CourseArrayAdapter.getView","position == " + position);
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
    ViewGroup pieces = (ViewGroup)view.findViewById(HOR_LAYOUT);
    List<CourseRev.Unit.Nugget> nugList = getItem(position);
    if(DEBUG)Log.i("Udacity.NuggetArrayAdapter.getView","nugList.size " +nugList.size());
    try {
      if(nugList.size() > 0 && nugList.get(0) != null) {
	CourseRev.Unit.Nugget nug;
	Iterator<CourseRev.Unit.Nugget> nugIter = nugList.iterator();
	String firstName = nugList.get(0).getName();
	TextView piece;
	String nName;
	while(nugIter.hasNext()) {
	  nug = nugIter.next();
	  nName = nug.getName().replace(firstName, "").trim();
	  nName = (nName.length() == 0 ? toWord(nug.getNuggetType()) : nName);
	  piece = (TextView)mInflater
			      .inflate(PIECE_ITEM_TEXTVIEW, pieces, false);
	  piece.setText(nName);
	  if(nug.getMedia() != null) {
	    final String youtubeId = nug.getMedia().getYoutubeId();
	    piece.setClickable(true);
	    piece.setOnClickListener( 
			    new OnClickListener() {
				@Override
				public void onClick(View v) {
				  context.startActivity(new Intent(Intent.ACTION_VIEW, 
				  Uri.parse("http://www.youtube.com/watch?v="+youtubeId)));
				}
			    });
	  }
	  pieces.addView(piece);
	}
	name.setText(nugList.get(0).getName());
	num.setText(Integer.toString(position+1));
      } else { 
	name.setText(":oddity");	
      }
    } catch (Exception e) {
      if(DEBUG)Log.w("Udacity.CourseArrayAdapter.getView", 
	      "Course seems to be missing fields::" + e);
    }
    return view;
  }

}
