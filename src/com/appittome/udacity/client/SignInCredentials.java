package com.appittome.udacity.client;

import org.json.JSONObject;
import org.json.JSONException;
import android.util.Log;
/**
 *  Hold the state of the current user credentials.
 *
 */
public class SignInCredentials implements Connection.OnNewCsrfTokenListener {
  /**Current email stored in this object. */
  protected String email;
  /**Current password stored in this object. */
  protected String password;
  /**Current token stored in this object */
  protected String csrf_token;
  //TODO this should be replaced with an enum or something from com.udacity.api
  private static final String METHOD = "account.sign_in";
  private static final String VERSION = "dacity-1";

  /**
   *  Generate on failure to find expected Credentials
   */
  public class NullCredentialsException extends NullPointerException {
    /**
     * Create new exception
     * @param msg message for exception
     */
    public NullCredentialsException(String msg) {
      super(msg);
    }
  }
  /**
   * New Credentials object with all null parameters.
   */
  public SignInCredentials() {
    this(null,null);  
  }
  /**
   * New Credentials object with null CSRF_TOKEN
   * @param email user email as String
   * @param password user password as String
   */
  public SignInCredentials(String email, String password) {
    this(email, password, null);
  }
  /**
   * New, completely specified, credentials Object.
   * @param email email as String
   * @param password password as String
   * @param csrf_token token as String
   */
  public SignInCredentials(String email, String password, String csrf_token) {
    this.email = email;
    this.password = password;
    this.csrf_token = csrf_token;
  }
//---setters---//
  /**
   * Sets the email value for this cretentials object.
   * @param email email string to set
   */
  public void setEmail(String email) {
    this.email = email;
  }
  /**
   * Sets the password value for this credentials object.
   * @param password password string to set
   */
  public void setPassword(String password) {
    this.password = password;
  }
  /**
   * Sets the CSRF_TOKEN value for this credentials object.
   * @param csrf_token CSRF_TOKEN string to set
   */
  public void setCsrfToken(String csrf_token) {
    this.csrf_token = csrf_token;
  }
//---getters---//
  /**
   * Gets current email value.
   * @return email <code>String</code>
   * @throws NullCredentialsException if email requested is null
   */
  public String getEmail() throws NullCredentialsException {
    if (this.email == null) 
      throw new NullCredentialsException("email");
    return this.email;
  } 
  /**
   * Gets current password length.
   * @return length of password string
   */
  public int getPasswordLength() {
    return this.password.length();
  }
  /**
   * Gets password value of this object
   * @return password as <code>String</code>
   * @throws NullCredentialsException if password requested is null
   */
  protected String getPassword() throws NullCredentialsException {
    if (this.email == null) 
      throw new NullCredentialsException("email");
    return this.password;
  }
  /**
   * Gets current CSRF_TOKEN value for this object
   * @return CSRF_TOKEN as string
   */
  protected String getCsrf_token() {
    return this.csrf_token;
  }
  /**
   * Interface method {@link Connection.OnNewCsrfTokenListener}
   * @param token new CSRF_TOKEN that was found in most recent response.
   */
  public void onNewCsrfToken(String token) {
    setCsrfToken(token);
  }
  /** 
   * Converts this Credentials object to a Udacity login JSONObject.
   * @return a JSONObect representing this credentials object as necessary for 
   *         the Udacity login.
   */
  public JSONObject toJSON() throws NullCredentialsException {
    //TODO replace with enum, GSON, and com.udacity.api
    JSONObject retObj = new JSONObject();
    JSONObject data = new JSONObject();
    Log.w("Udacity.SignInCredentials.toJSON", "begin assembling JSON…");
    try {
      //User relavant attibutes
      data.put("email", getEmail());
      data.put("password", getPassword());
      retObj.put("data", data);
      //Client relavant attibutes
      retObj.put("method", METHOD);
      retObj.put("version", VERSION);
      retObj.put("csrf_token", getCsrf_token());
    } catch (JSONException e) {
      Log.w("Udacity.SignInCredentials.toJSON()", e);
    } catch (NullCredentialsException e) {
      throw new NullCredentialsException(e.toString());
    }
    return retObj;
  }
}

