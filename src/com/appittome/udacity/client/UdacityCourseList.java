package com.appittome.udacity.client;

import android.util.Log;
import android.os.AsyncTask;

import java.util.List;
import java.util.Map;
import java.util.LinkedList;
import java.util.HashMap;

import java.util.HashSet;
import java.util.Iterator;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

import com.google.gson.Gson;
import com.udacity.api.CourseRev;

public class UdacityCourseList extends LinkedList<UdacityCourseList.Course>
				implements Connection.OnConnectionReadyListener
{
  public static final String JSON_NAME = "name";
  public static final String JSON_ID   = "id";
  public static final String JSON_ICON = "icon_url";
  public static final String JSON_ID_OR_NAME = "id_or_name";

  private static Connection udacityConn = new Connection();
  private static final boolean DEBUG = true;
  private static HashSet<OnCourseListChangeListener> changeListeners = 
				    new HashSet<OnCourseListChangeListener>();

  public UdacityCourseList(OnCourseListChangeListener ui) {
    addOnChangeListener(ui);
  }

  public UdacityCourseList(JSONObject payload, OnCourseListChangeListener ui) 
							      throws JSONException{
    addOnChangeListener(ui);
    addAll(payload.getJSONArray("courses"));
    
  }

  public void addAll(JSONArray jarray) throws JSONException{
    //this.add(new Course(jarray.getJSONObject(0)));
    for(int i=0; i < jarray.length(); i++){
      this.add(new Course(jarray.getJSONObject(i)));
    }
  }

  public interface OnCourseListChangeListener {
    public void onCourseListChange();
  }

  public boolean addOnChangeListener(OnCourseListChangeListener listener) {
    return changeListeners.add(listener);
  }

  public boolean removeOnChangeListener(OnCourseListChangeListener listener) {
    return changeListeners.remove(listener);
  }

  protected void notifyOnChangeListeners() {
    Iterator<OnCourseListChangeListener> i = changeListeners.iterator();
    while(i.hasNext()) {
      i.next().onCourseListChange();
    }
  }

  public void onConnectionReady(Connection c) {
    this.udacityConn = c;
    if(DEBUG) Log.i("Udacity.UdacityCourseList.onConnectionReady", "Connection ready");
    fetchCourseList();
  }

  public void fetchCourseList() {
    //TODO CONCOLIDATE SERVER REQUESTs
    JSONObject data = new JSONObject();
    JSONObject payload = new JSONObject();
    try {
      payload.put("data", data);
      payload.put("method","account.courses_of_interest");
      payload.put("version","dacity-1");
    } catch (JSONException e){
      Log.w("Udacity.UdacityConnection.fetchCourseList()", e);
    }

    udacityConn.new AsyncJSONGetTask() {
      @Override
      protected void onPostExecute(JSONObject json) {
	try{
	  if(DEBUG) Log.i("UdacityConnection.fetchCourseList", json.toString());
	  addAll(json.getJSONObject("payload").getJSONArray("courses"));
	  notifyOnChangeListeners();
	} catch (JSONException e) {
	    Log.w("Udacity.UdacityConnection.fetchCourseList()",
		    "Server did not return valid JSON::" + e);
	}
      }
    }.execute(payload);
  }	

  public class Course {
    String course_URI = null;
    JSONObject course_info;

    LinkedList<CourseRev.Unit> unitList;
    Map<String, List<List<CourseRev.Unit.Nugget>>> unitToNug;

    protected Course(JSONObject course_info){
      this.course_info = course_info;
      this.unitList = new LinkedList<CourseRev.Unit>();
      this.unitToNug = new HashMap<String, List<List<CourseRev.Unit.Nugget>>>();
      fetchUnitList();
    }
    public List<List<CourseRev.Unit.Nugget>> getUnitNugList(String unitKey) {
      return unitToNug.get(unitKey);
    }
    public List<CourseRev.Unit> getUnitList() {
      return unitList;
    }
    public JSONObject getCourseInfo() {
      return course_info;
    }

    public String getIconURL() {
      String ret = "";
      try {
	ret = course_info.getString(JSON_ICON);
      } catch (JSONException e){
	if(DEBUG) Log.w("Udacity.UdacityCourseList.getIconURL()","invalid JSON"+e);
	revalidateCourse();
      }
      return ret;
    }
    public String getName() {
      String ret = "";
      try {
	ret = course_info.getString(JSON_NAME);
      } catch (JSONException e){
	if(DEBUG) Log.w("Udacity.UdacityCourseList.getIconURL()","invalid JSON"+e);
	revalidateCourse();
      }
      return ret;
    } 
    public String getId() {
      String ret = "";
      try {
	ret = course_info.getString(JSON_ID);
      } catch (JSONException e){
	if(DEBUG) Log.w("Udacity.UdacityCourseList.getIconURL()","invalid JSON"+e);
	revalidateCourse();
      }
      return ret;
    }
    public String getIdOrName() {
      String ret = "";
      try {
	ret = course_info.getString(JSON_ID_OR_NAME);
      } catch (JSONException e){
	if(DEBUG) Log.w("Udacity.UdacityCourseList.getIconURL()","invalid JSON"+e);
	revalidateCourse();
      }
      return ret;
    }

    private void fetchUnitList() {
      if( udacityConn != null && udacityConn.isReady() ){
	if(getCourseURI() == null) {
	  // 302 to get query string,
	  // course/cs387
	  String course_spec = "/course/" + getIdOrName();
	  udacityConn.new GrabPageTask() {
	    @Override
	    protected void onPostExecute(HttpURLResponse resp) {
	      List<String> locList = resp.getHeaderFields().get("Location");
	      //the return is a fully qualified URL:
	      //http://www.udacity.com/view#Course/cs313/CourseRev/1
	      //all that's needed is what is after #
	      String course_path;
	      course_path=locList.get(0);
	      course_path=course_path.substring(course_path.indexOf("#")+1);
	      if(DEBUG)Log.i("Udacity.UdacityCourseList.FetchUnitList", 
			"response code: "+resp.getResponseCode()+"\n"+ locList.get(0)+"\n"+
			course_path);
	      
	      setCourseURI(course_path);
	      fetchUnitList();
	    }
	  }.execute(course_spec);
	} else {
	  JSONObject data = new JSONObject();
	  JSONObject payload = new JSONObject();
	  try{
	    //TODO CONSOLIDATE SERVER REQUESTs
	    data.put("path", getCourseURI());
	    payload.put("data",data);
	    payload.put("method", "course.get");
	    payload.put("version", "dacity-1");
	  }catch (JSONException e) {
	    if(DEBUG) 
	      Log.w("Udacity.UdacityCourseList.fetchUnitList","Should never happen");
	  }
	  //then JSON::
	  //{"data":{"path":"#Course/cs387/CourseRev/apr2012"},
	  // "method":"course.get","version":"dacity-1"}
	  Log.i("Udacity.UdacityCourseList.fetchNugList","fetching nugget list");
	  udacityConn.new AsyncJSONGetTask() {
	    @Override
	    protected void onPostExecute(JSONObject JSONResp){
	      setUnitList(JSONResp);
	    }
	  }.execute(payload);
	}
      }
    }
    private String getCourseURI() {
      return course_URI;
    }

    private void setCourseURI(String URI){
      course_URI = URI;
    }

    public void revalidateCourse(){
      //TODO query again?
    }
  
    private void setUnitList(JSONObject payload){
      try{
	//TODO this is absurd - String to JSON to String to… java object.
	// need to build custom parser
	String json = payload.getJSONObject("payload")
			      .getJSONObject("course_rev").toString();
  
	Log.i("udacity.CourseList.Course.setUnitList", 
		json.length()+": "+json.substring(1,70));
	
	CourseRev rev = new Gson().fromJson(json, CourseRev.class);

	HashMap<String,CourseRev.Unit> unitMap = new HashMap<String,CourseRev.Unit>();

	CourseRev.Unit unit;
	//build list of all units
	List<CourseRev.Unit> units = rev.getUnits();
	Iterator<CourseRev.Unit> unitIter = units.iterator();
	//STEP 1: build map of keys to units
	while(unitIter.hasNext()) {
	  unit = unitIter.next();
	  unitMap.put(unit.getKey(), unit);
	}
	//build layout iterator
	List<CourseRev.UnitLink> unitLayout = rev.getUnitLayout();
	Iterator<CourseRev.UnitLink> uLayoutIter = unitLayout.iterator();
	//STEP 2: sort the units.
	while(uLayoutIter.hasNext()) {
	  //call UI update - list can grow.
	  unitList.addLast(unitMap.get(uLayoutIter.next().getUnitKey()));
	  notifyOnChangeListeners();
	}
	startNuggetSortTask();
      }catch (Exception e) {
	if(DEBUG) 
	  Log.w("Udacity.UdacityCourseList.fetchNugList","GSON fail: "+e);
      }
    }
    /**
    *  Override to implement custom scheduling of nugget population.
    *  This version just moves linearly down the unit list, starting
    *  a infinite number of processes until it exhausts the list.
    *
    *  It's horrible.
    */
    public void startNuggetSortTask() {
       /*Iterator<CourseRev.Unit> unitIter = unitList.iterator();
       while(unitIter.hasNext()) {*/
	  new AsyncNuggetSortTask().execute(unitList.toArray(new CourseRev.Unit[1]));
    }

    private void addNuggetListToUnit (String key, 
				      List<CourseRev.Unit.Nugget> nList){
      List<List<CourseRev.Unit.Nugget>> listOfLists = unitToNug.get(key);
      if (listOfLists == null){
	listOfLists = new LinkedList<List<CourseRev.Unit.Nugget>>();
	unitToNug.put(key, listOfLists);
      }
      listOfLists.add(nList);
      //TODO might be fun…
      //notifyOnChangeListeners();
    }

    protected class AsyncNuggetSortTask 
	  extends AsyncTask<CourseRev.Unit, Integer, CourseRev.Unit[]>
    {
      @Override
      protected CourseRev.Unit[] doInBackground(CourseRev.Unit... units) {
	for(CourseRev.Unit unit: units){
	  Log.i("Udacity.UdacityCourseList.AsyncNuggetSortTask", "name: "+unit.getName());
	  HashMap<String, CourseRev.Unit.Nugget> nugMap = 
				      new HashMap<String, CourseRev.Unit.Nugget>();
	  CourseRev.Unit.Nugget nug;
	  Iterator<CourseRev.Unit.Nugget> nugIter;
	  //STEP 1: build key map
	  nugIter = unit.getNuggets().iterator();
	  while(nugIter.hasNext()) {
	    nug = nugIter.next();
	    nugMap.put(nug.getKey(), nug);
	  }

	  Iterator<List<CourseRev.Unit.NuggetLink>> nLayoutIter;
	  Iterator<CourseRev.Unit.NuggetLink> nSubLayoutIter;
	  List<CourseRev.Unit.Nugget> nugList;
	  CourseRev.Unit.NuggetLink nLink;
	  //STEP 2: dereference nugget layout
	  nLayoutIter = unit.getNuggetLayout().iterator();
	  while(nLayoutIter.hasNext()){
	    nSubLayoutIter = nLayoutIter.next().iterator();
	    nugList = new LinkedList<CourseRev.Unit.Nugget>();
	    while(nSubLayoutIter.hasNext()) {
	      try{
		nLink = nSubLayoutIter.next();
		if(nLink != null) 
		  nugList.add(nugMap.get(nLink.getNuggetKey()));
	      }catch (Exception e) {
		Log.w("Udacity.UdacityCourseList.Course.AsyncSort",
		"key:"+e);
	      }
	    }
	    addNuggetListToUnit(unit.getKey(),nugList);
	  } 
	}
      return units;
      }
    }
  }
}
