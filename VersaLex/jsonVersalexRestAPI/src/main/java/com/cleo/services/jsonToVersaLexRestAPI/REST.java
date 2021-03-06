package com.cleo.services.jsonToVersaLexRestAPI;


import com.cleo.services.jsonToVersaLexRestAPI.POJO.AccessToken;
import com.cleo.services.jsonToVersaLexRestAPI.POJO.VersalexCollectionResponse;
import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import sun.awt.image.ImageWatched;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;


public class REST {

  public static String CONNECTIONS_URL = "/connections";
  public static String AUTHENTICATION_URL = "/authentication";
  public static String AUTHENTICATOR_URL = "/authenticators";
  public static String ACTION_URL = "/actions";

  private String url;
  private int port;
  private String baseUrl;
  private String versalexUrl;
  private String authToken;
  private CloseableHttpClient httpClient;

  public REST(String url, int port, String username, String password) throws Exception {
    this.url = url;
    this.port = port;
    this.baseUrl = url + ":" + port;
    this.versalexUrl = this.baseUrl + "/api";

    this.authToken = getToken(username, password);
  }

  private String getToken(String username, String password) throws Exception {
    HttpPost httpAuthenticationPost = new HttpPost(this.versalexUrl + AUTHENTICATION_URL);
    httpAuthenticationPost.addHeader("content-type", "application/x-www-form-urlencoded");
    List<NameValuePair> params = new ArrayList<NameValuePair>();
    params.add(new BasicNameValuePair("grant_type", "password"));
    params.add(new BasicNameValuePair("username", username));
    params.add(new BasicNameValuePair("password", password));
    httpAuthenticationPost.setEntity(new UrlEncodedFormEntity(params));

    String result = executeHttpRequest(httpAuthenticationPost, 200);
    Gson gson = new Gson();
    AccessToken accessToken = gson.fromJson(result, AccessToken.class);
    return accessToken.access_token;
  }

  public VersalexCollectionResponse getConnections() throws Exception {
    HttpGet httpGetConnections = new HttpGet(this.versalexUrl + CONNECTIONS_URL);

    String result = executeHttpRequest(httpGetConnections, 200);
    return REST.getVersalexConnectionResponse(result);
  }

  public VersalexCollectionResponse getAuthenticators(String filter) throws Exception {
    HttpGet httpGetAuthenticators = new HttpGet(this.versalexUrl + AUTHENTICATOR_URL);
    URI uri = new URIBuilder(httpGetAuthenticators.getURI()).addParameter("filter", filter).build();
    httpGetAuthenticators.setURI(uri);

    String result = executeHttpRequest(httpGetAuthenticators, 200);
    return REST.getVersalexConnectionResponse(result);
  }

  public LinkedTreeMap createConnection(String connectionJson) throws Exception {
    return postJSON(connectionJson, this.versalexUrl + CONNECTIONS_URL);
  }

  public LinkedTreeMap createAuthenticator(String authenticatorJson) throws Exception {
    return postJSON(authenticatorJson, this.versalexUrl + AUTHENTICATOR_URL);
  }

  public LinkedTreeMap createUser(String userJson, String authId) throws Exception {
    return postJSON(userJson, this.versalexUrl + makeUserUrl(authId));
  }

  public LinkedTreeMap createAction(String actionJson) throws Exception {
    return postJSON(actionJson, this.versalexUrl + ACTION_URL);
  }

  public boolean delete(String href) {
    HttpDelete httpDelete = new HttpDelete(this.baseUrl + href);
    try {
      executeHttpRequest(httpDelete, 204);
    } catch (Exception ex) {
      return false;
    }
    return true;
  }

  public LinkedTreeMap postJSON(String json, String url) throws Exception {
    HttpPost httpConnectionPost = new HttpPost(url);
    httpConnectionPost.setEntity(new StringEntity(json));
    httpConnectionPost.addHeader("content-type", "application/json");

    String result = executeHttpRequest(httpConnectionPost, 201);

    return REST.getJsonResponse(result);
  }

  public String getAuthToken() {
    return this.authToken;
  }

  public String executeHttpRequest(HttpRequestBase httpRequest, int successCode) throws Exception {
    HttpClient httpClient = REST.getDefaultHTTPClient();
    if (this.authToken != null)
      httpRequest.addHeader("Authorization", "Bearer " + this.authToken);
    try {
      HttpResponse response = httpClient.execute(httpRequest);
      int responseCode = response.getStatusLine().getStatusCode();
      if (responseCode == successCode) {
        return EntityUtils.toString(response.getEntity());
      } else {
        String msg = "Failed HTTP Request";
        if (response.getEntity() != null)
          msg += ": " + EntityUtils.toString(response.getEntity());
        throw new Exception(msg);
      }
    } finally {
      httpRequest.releaseConnection();
    }
  }

  public static LinkedTreeMap getJsonResponse(String responseText) {
    Gson gson = new Gson();
    return gson.fromJson(responseText, LinkedTreeMap.class);
  }

  public static VersalexCollectionResponse getVersalexConnectionResponse(String responseText) {
    Gson gson = new Gson();
    VersalexCollectionResponse versalexRestResponse = gson.fromJson(responseText, VersalexCollectionResponse.class);
    return versalexRestResponse;
  }

  public static HttpClient getDefaultHTTPClient() {
    return HttpClients.custom()
            .setDefaultRequestConfig(RequestConfig.custom().setCookieSpec(CookieSpecs.STANDARD).build()).build();
  }

  public static String makeUserUrl(String authenticator) {
    return AUTHENTICATOR_URL + "/" + authenticator + "/users";
  }
}
