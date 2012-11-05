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
  private static final int PIECE_ITEM= R.layout.nugget_piece_template;
  private static final int NAME_VIEW = R.id.nugget_name;
//  private static final int NUM_VIEW = R.id.nugget_number;
  private static final String YOUTUBE_WATCH = "http://www.youtube.com/watch?v=";
  private static LayoutInflater mInflater;
  private static Activity context;

  public NuggetArrayAdapter(Activity context, List<List<CourseRev.Unit.Nugget>> nug_array) {
    super(context, NUGGET_ITEM_LAYOUT, nug_array);
    this.mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    this.context = context;
  }
  private static String youtubeImgURL( String id ) {  
	  return "http://img.youtube.com/vi/"+id+"/default.jpg";
  }
  private String toWord(NuggetType t) {
    String retS = "Lecture";
    if(t == NuggetType.quiz ){
      retS = "Question";
    }
    return retS;
  }
  private String makeReadableName(CourseRev.Unit.Nugget n, String baseName) {
    String nName = n.getName().replace(baseName, "").trim();
    nName = (nName.length() == 0 ? toWord(n.getNuggetType()) : nName);
    return nName;
  }
  private void clickTube(View v, String url) {
    final String fUrl = url;
    v.setClickable(true);
    v.setOnClickListener( 
		    new OnClickListener() {
			@Override
			public void onClick(View v) {
			  context.startActivity(new Intent(Intent.ACTION_VIEW, 
			  Uri.parse(fUrl)));
			}
		    });
  }

  @Override
  public View getView(int position, View convertView, ViewGroup parent){
    if(DEBUG)Log.i("Udacity.CourseArrayAdapter.getView","position == " + position);
    View view;
    ViewGroup verLayout,horLayout;
    ImageView piece_img;
    //TODO not a big fan of this - but… it'll work for now
    if (convertView == null) {
      view = mInflater.inflate(NUGGET_ITEM_LAYOUT, parent, false);
    } else {
      view = convertView;
      //reuse recycle
      ((ViewGroup)view.findViewById(R.id.lin_ver)).removeAllViews();
      piece_img = (ImageView)view.findViewById(R.id.piece_img);
      ((ViewGroup)view.findViewById(R.id.lin_hor)).removeView(piece_img);
    }
    //TextView num = (TextView) view.findViewById(NUM_VIEW);	  
    TextView name = (TextView) view.findViewById(NAME_VIEW);
    verLayout = (ViewGroup)view.findViewById(R.id.lin_ver);
    horLayout= (ViewGroup)view.findViewById(R.id.lin_hor);
    List<CourseRev.Unit.Nugget> nugList = getItem(position);
    if(DEBUG)Log.i("Udacity.NuggetArrayAdapter.getView","nugList.size " +nugList.size());
    try {
      if(nugList.size() > 0 && nugList.get(0) != null) {
	CourseRev.Unit.Nugget nug;
	CourseRev.Unit.Nugget first = nugList.get(0);
	CourseRev.Unit.Nugget.Media med = first.getMedia();
	Iterator<CourseRev.Unit.Nugget> nugIter = nugList.iterator();
	TextView piece_txt;
	String nName, imgURL;
	InputStream is = null;
	if((med = first.getMedia()) != null ){
	  piece_img= (ImageView)mInflater.inflate(R.layout.nugget_img_template,
						   horLayout, false);
	  try{
	    is = (InputStream)(new URL(youtubeImgURL(med.getYoutubeId())).getContent());
	    piece_img.setImageDrawable(Drawable.createFromStream(is, "src"));
	  } catch(Exception e) {
	    //if the img doesn't appear - don't cry about it
	  } finally {
	    if(is!=null)is.close();
	  }
	  clickTube((View)piece_img, YOUTUBE_WATCH+med.getYoutubeId());
	  horLayout.addView(piece_img, 0);
	}
	
	while(nugIter.hasNext()) {
	  nug = nugIter.next();
	  piece_txt= (TextView)mInflater.inflate(R.layout.nugget_piece_template,
						   verLayout, false);
	  piece_txt.setText(makeReadableName(nug, first.getName()));
	  if((med=nug.getMedia()) != null) {
	    clickTube((View)piece_txt, YOUTUBE_WATCH+med.getYoutubeId());
	  }
	  verLayout.addView(piece_txt);
	}
	name.setText(first.getName());
	//num.setText(Integer.toString(position+1));
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
