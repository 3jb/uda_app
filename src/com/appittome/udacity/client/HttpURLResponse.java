package com.appittome.udacity.client;

import android.util.Log;

import java.net.HttpURLConnection;
import java.io.IOException;

import java.util.Map;
import java.util.List;

import org.apache.commons.io.IOUtils;

/** 
 * Used to return complete HTTP reponse through asynchronous classes in 
 * Connection. see <code>Connection.getJSON</code>, 
 * <code>Connection.postJSON</code>, and <code>Connection.GrabPageTask</code>
 */
public class HttpURLResponse {
  /** debug log switch*/ 
  static final boolean DEBUG = true;

  int responseCode;
  Map<String, List<String>> headers;
  String response;
  /**
   * Builds new object from open connection.
   * @param conn open HttpURLConnection
   * @throws IOException if response stream fails in some way
   */
  public HttpURLResponse(HttpURLConnection conn) throws IOException {
    this.responseCode = conn.getResponseCode();
    this.headers = conn.getHeaderFields();
    this.response = readResponse(conn);
  }

  public int getResponseCode(){
    return this.responseCode;
  }

  public Map<String, List<String>> getHeaderFields() {
    return this.headers;
  }

  public String getResponse() {
    return this.response;
  }

  private String readResponse(HttpURLConnection c) throws IOException {
    return IOUtils.toString(c.getInputStream());
  }
}
