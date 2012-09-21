package com.appittome.udacity.client;

import org.json.JSONObject;
import org.json.JSONException;
import android.util.Log;

public class SignInCredentials {
  
  protected String email;
  protected String password;
  protected String csrf_token;
  private static final String METHOD = "account.sign-in";
  private static final String VERSION = "dacity-45";

  public SignInCredentials() {
    this(null,null);  
  }
  public SignInCredentials(String email, String password) {
    this(null, null, null);
  }
  public SignInCredentials(String email, String password, String csrf_token) {
    this.email = email;
    this.password = password;
    this.csrf_token = csrf_token;
  }
  //setters
  public void setEmail(String email) {
    this.email = email;
  }
  public void setPassword(String password) {
    this.password = password;
  }
  public void setCsrf_token(String csrf_token) {
    this.csrf_token = csrf_token;
  }
  //public attributes
  public String getEmail(){
    return this.email;
  }
  public int getPasswordLength() {
    return this.password.length();
  }
  //private attributes
  protected String getPassword(){
    return this.password;
  }
  protected String getCsrf_token() {
    return this.csrf_token;
  }

  public JSONObject toJSON() {
    JSONObject retObj = new JSONObject();
    JSONObject data = new JSONObject();

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
      Log.w("collectForSubmission()", e);
    }
    return retObj;
  }
}

