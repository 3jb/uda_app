package com.appittome.udacity.client;

import android.util.Log;
import android.os.AsyncTask;

import java.util.List;
import java.util.Map;
import java.util.LinkedList;
import java.util.HashMap;
import java.util.Iterator;

import java.util.HashSet;
import java.util.Iterator;

import com.google.gson.Gson;
import com.udacity.api.CourseRev;
import com.udacity.api.Request;
import com.udacity.api.Response;
import com.udacity.api.CourseGetResponse;
/**
 * Course List managing class. Manages the model for this application.
 * This class updates its own state using a connection to create a local
 * representation of the courrent account's list of courses and their 
 * units and nuggets.
 */
public class UdacityCourseList extends LinkedList<UdacityCourseList.Course>
				implements Connection.OnConnectionReadyListener
{

  /**Course list has access to a connection object*/
  private static Connection udacityConn; 
  /**print debug log info*/
  private static final boolean DEBUG = true;
  private static HashSet<OnCourseListChangeListener> changeListeners = 
				    new HashSet<OnCourseListChangeListener>();

  /**
   * CourseList instantiation initializes single onCourseListChangeListener.
   * Listener will be notified anytime underlying course hierarchy is modified.
   * @param ui Listener object that responds to graph changes in the course list.
   */
  public UdacityCourseList(OnCourseListChangeListener ui) {
    addOnChangeListener(ui);
  }

  /**
   * Takes JSON response from account.courses_of_interest and builds a course list.
   * Course constructor invokes course.get
   * @param infos array of course infos
   */
  public void addAll(List<Response.CourseInfo> infos) {
    Iterator<Response.CourseInfo> i = infos.iterator();
    while (i.hasNext()) {
      this.add(new Course(i.next()));
    }
  }
  /**
   * On connection ready interface:  when the connection is ready, this object
   * queries the server for a new list of courses.
   * @param c the connection that is ready
   */
  public void onConnectionReady(Connection c) {
    this.udacityConn = c;
    if(DEBUG) Log.i("Udacity.UdacityCourseList.onConnectionReady", "Connection ready");
    fetchCourseList();
  }
  /**
   * Listener interface that is notified each time this object updates the 
   * underlying data or graph.
   */
  public interface OnCourseListChangeListener {
    /**
     * Called each time CourseList updates the underlying course graph.
     */
    public void onCourseListChange();
  }
  /**
   * Add a OnCourseListChangeListener to this object.  Any number of listeners
   * is acceptable, they are managed as a HashSet.  Even if a listener is added
   * more than once, it will only be notified once.
   * @param listener OnCourseChangeListener to add to this object
   * @return true if this is the first time this object has been added to this set
   *         @see java.util.HashSet
   */
  public boolean addOnChangeListener(OnCourseListChangeListener listener) {
    return changeListeners.add(listener);
  }
  /**
   * Remove a specific listener from the OnCourseListChangeListener set. After
   * removal from the set, the listener will no longer be notified of course list
   * updates.
   * @param listener Listener to be removed
   * @return true if the set contained the specified object, @see java.util.HashSet
   */
  public boolean removeOnChangeListener(OnCourseListChangeListener listener) {
    return changeListeners.remove(listener);
  }
  /**
   * Notifiy all listners that a change in the underlying graph of this object has
   * occured.
   */
  protected void notifyOnChangeListeners() {
    Iterator<OnCourseListChangeListener> i = changeListeners.iterator();
    while(i.hasNext()) {
      i.next().onCourseListChange();
    }
  }

  /**
   * Perform a GET account.courses_of_interest, returning a 
   * Response.CourseListobject containing an array of courses
   * to populate this list.
   */
  private void fetchCourseList() {
    udacityConn.new AsyncJSONGetTask<Response.CourseList>() {
      @Override
      protected void onPostExecute(Response.CourseList resp) {
	  addAll(resp.getList());
	  notifyOnChangeListeners();
      }
    }.execute(Request.coursesOfInterestBuilder().build());
  }	
  /**
   * Class to represent members of the CourseList object: courses.
   * Upon instantiation, each course list will try to perform an async
   * GET course.get, and then parse its unit, and nugget lists.
   *
   *<p>NOTE: The response to the GET course.get action is a rather 
   * bulky object, and parsing it takes some time.  Right now this is 
   * all implemented to happen in one huge lump: parse the entire object, 
   * organize the list of units, for each set of units organize a list
   * of lists of nuggets.</p>
   * <p> With a custom parser the response could be broken down, and
   * portions parsed individualy so that the UI could be updated more 
   * fluidly.  Then the most pianful porition of the wait would only 
   * be the 20-30k object download.</p>
   *
   */
  public class Course {
    /** relative path of this course */
    String course_URI = null;
    /** information extracted from account.courses_of_interest response*/
    Response.CourseInfo course_info;
    /** units associated with this course*/
    LinkedList<CourseRev.Unit> unitList;
    /** units mapped to their list of odered lists of lists of nuggets. */
    Map<String, List<List<CourseRev.Unit.Nugget>>> unitToNug;
    /**
     * Instantiate a Course object by building it around the course info 
     * {@code Response}. New lists are instantiated, then an async process is 
     * started to populate this course with units and nuggets
     * @param course_info the info returned from GET accounts.courses_of_interest
     */
    protected Course(Response.CourseInfo course_info){
      this.course_info = course_info;
      this.unitList = new LinkedList<CourseRev.Unit>();
      this.unitToNug = new HashMap<String, List<List<CourseRev.Unit.Nugget>>>();
      fetchUnitList();
    }
    /**
     * Returns a List of Lists of nuggets.  Nuggets come in sets that are often 
     * clumped around a single topic.  Often there will be a set of 3 a Letcure, 
     * a Question, and an answer.
     * @param unitKey Hash signature of unit to retrieve nugget lists for
     * @return a list of sets of nuggets clumped around topics
     */
    public List<List<CourseRev.Unit.Nugget>> getUnitNugList(String unitKey) {
      return unitToNug.get(unitKey);
    }
    /**
     * Gets the list of units for this course.
     * @return list of units for this course.
     */
    public List<CourseRev.Unit> getUnitList() {
      return unitList;
    }
    /**
     * Gets the most recent respones to a GET
     * account.courses_of_interest {@code Request}. {@see com.udacity.api.Request}
     * @return response from GET account.courses_of_intrest representing this course.
     */
    public Response.CourseInfo getCourseInfo() {
      return course_info;
    }
    /**
     * Gets the URL for the icon representing this course as found in the JSON
     * course_info object for this course.
     * @return URL of the icon for this course.
     */
    public String getIconURL() {
      return this.course_info.getIconUrl();
    }
    /** 
     * Gets the user friendly name for this course from the GET 
     * account.courses_of_interest response.
     * @return User friendly name of this course
     */
    public String getName() {
      return this.course_info.getName();
    } 
    /**
     * Gets the course Id from the GET account.courses_of_interest 
     * response.
     * @return course catalog type id of this course
     */
    public String getId() {
      return this.course_info.getId(); 
    }
    /**
     * Gets the field called "id_or_name" from the GET account.courses_of_interest
     * response.  This field is often used to distiguish the course in communication
     * with the server.
     * @return id_or_name field in GET account.courses_of_interest response.
     */
    public String getIdOrName() {
      return this.course_info.getIdOrName();
    }
    /**
     * After the course has been populated with the GET account.courses_of_interest
     * info, this object is ready to GET course.get. Fetch the remainder 
     * of the course info: units, and nugget lists.  This request receives a 200-300k 
     * JSON Object {@see com.udacity.api.CourseGetResponse} as a response.
     */
    private void fetchUnitList() {
      //first we need to figure out where the most recent course revision actually is.
      //A generic course path is tried, and a 302 redirect is received from the server
      //with an updated URI for the current course revision.
      if(udacityConn != null){
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
	  if(DEBUG) Log.i("Udacity.UdacityCourseList.fetchNugList","fetching nugget list");
	  udacityConn.new AsyncJSONGetTask<CourseGetResponse>() {
	    @Override
	    protected void onPostExecute(CourseGetResponse resp){
	      setUnitList(resp.getCourseRev());
	    }
	  }.execute( Request.courseGetBuilder().setPath(getCourseURI()).build());
	}
      }
    }
    /**
     * Gets the relative path of the most current revision of this
     * course.
     * @return relative path of the current revision of this course as String.
     */
    private String getCourseURI() {
      return course_URI;
    }
    /**
     * Sets the path the the current revision of this course.
     * @param URI relative path of the current revision of this course
     */
    private void setCourseURI(String URI){
      course_URI = URI;
    }
    /**
     * Called when fields of GET account.courses_of_interest are not
     * as expected.  This is just a stub for now.
     */
    public void revalidateCourse(){
      //TODO query again?
    }
    /**
     * This method disects the response from GET course.get.
     * Extracts the ordered graph of units and nuggets for this
     * course, and update the state of this course accordingly.
     * @param rev from {@code GET course.get - payload.course_rev}
     */
    private void setUnitList(CourseRev rev){
      try{
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
    *  Currently this method takes the unitList of this course as an array
    *  and passes it to a single async process that moves through the list
    *  linearly until it has populated the nugget list for each unit with 
    *  all the available lists for each topic.
    */
    public void startNuggetSortTask() {
       /*Iterator<CourseRev.Unit> unitIter = unitList.iterator();
       while(unitIter.hasNext()) {*/
      //TODO don't convert to array?
      new AsyncNuggetSortTask().execute(unitList.toArray(new CourseRev.Unit[1]));
    }
    /**
     * Adds a new list of nuggets (a topic set) to the unit list of lists
     * as identified by the provided key.
     * @param key hash signature for unit to add list to
     * @param nList a topic list to add to the unit's list of topid sets.
     */ 
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
    /**
     * For each unit: organize nuggets according to the nuggetLayout.
     */
    private class AsyncNuggetSortTask 
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
		if(DEBUG) Log.w("Udacity.UdacityCourseList.Course.AsyncSort",
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
