package swifiic.soa;

import java.net.HttpURLConnection;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.*;
import java.net.*;
import java.util.Iterator;
import java.util.List;
import java.util.Map;


import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;
import android.util.Pair;

public class AuthenticationActivity extends Activity implements OnClickListener,OnItemClickListener{
	
	
	public static ArrayList<Pair<String,String>> basicNameValuePairs= new ArrayList<>();
	public  static ArrayList<Pair<String,String>> curNameValuePairs= null;
	
	public static SharedPreferences settings = null; // shared preferences , where the logged in user data is stored 
	public static final String COOKIE_ID = null; 
	public static HashMap<String,String> users = null;
	public static DBHelper helper = null; // DbHelper to help in storing the data of the logged in users
	
	
	Button bLogin;
	EditText etPwd;
	CheckBox cbRememberMe ;
	AutoCompleteTextView etUser;
	private boolean authenticateThreadRunning = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		initbasicNameValuePairs();
		settings = getSharedPreferences(Constants.LOGIN_DETAILS, 0);
		setContentView(R.layout.authenticate_user);
		bLogin = (Button) findViewById(R.id.bLogin);
		etUser = (AutoCompleteTextView) findViewById(R.id.atvUserName);
		etPwd = (EditText) findViewById(R.id.etPassword);
		cbRememberMe = (CheckBox) findViewById(R.id.cbRememberMe);
		bLogin.setOnClickListener(this);
		
		curNameValuePairs = new ArrayList<Pair<String,String>>();
		curNameValuePairs.addAll(basicNameValuePairs);
		
		// check if the settings contain the logged in user data , login directly without authentication if already logged in
		 boolean isLoggedIn = false;
		if (getPrefs(Constants.JSESSIONID,null)!=null){
			
			String cookieID = getPrefs(Constants.JSESSIONID,null);
			String user = getPrefs(Constants.USERNAME,null);
			curNameValuePairs.add(new Pair<>(Constants.JSESSIONID,cookieID));
			curNameValuePairs.add(new Pair<>(Constants.userId_tag,user));
			startActivity(new Intent(AuthenticationActivity.this,MainActivity.class));
			finish();
		}
	
		helper = new DBHelper(this);
		users = helper.getAllUsers();
		String userNames[] = users.keySet().toArray(new String[users.size()]); 
	
	// Adapter for the auto complete textview to get the drop down options for the logged in users
		ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1, userNames);
		etUser.setAdapter(adapter);
		etUser.setOnItemClickListener(this);
	}
	
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.login_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		//new LogoutTask().execute(ipAddr);
		switch(item.getItemId()){
			case R.id.login_settings :
				final Dialog dialog = new Dialog(AuthenticationActivity.this,android.R.style.Theme_Black);
				dialog.setContentView(R.layout.change_settings);
				dialog.setTitle(getResources().getString(R.string.action_settings));
				final EditText etIpAddr = (EditText) dialog.findViewById(R.id.etIpAddress);
				final Button bSave = (Button) dialog.findViewById(R.id.bSaveChanges);
				if (settings.contains(Constants.URL)) etIpAddr.setText(settings.getString(Constants.URL, null));
		/* onclicklistener in the settings dialog where the changes to the shared preferences are done ,
			Everytime an user logs in successfully , the shared preferences will be changed */
				bSave.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View v) {
						String ipAddr =etIpAddr.getText().toString().trim();
						if (ipAddr==null || ipAddr.equals("")) toast("Please fill..!!");
						else {
							SharedPreferences.Editor editor = settings.edit();
							editor.putString(Constants.URL,ipAddr);
							editor.commit();
							toast("Changes saved!!");
							dialog.dismiss();
						}
					}
				});
				dialog.show();
				break;
		}
		return true;
	}
	
	
	
	public static String getCookieId(){
		return COOKIE_ID;
	}
	
	public static SharedPreferences getSettings(){
		return settings;
	}
	// Initialize the common values for the session
	void initbasicNameValuePairs(){
		
		basicNameValuePairs = new ArrayList<>();
    	basicNameValuePairs.add(new Pair<>(Constants.clntSessId_tag,"somejunkSession"));
    	basicNameValuePairs.add(new Pair<>(Constants.secret_tag,"secretVal"));
    	basicNameValuePairs.add(new Pair<>(Constants.version_tag,"0.1"));
    	basicNameValuePairs.add(new Pair<>(Constants.appId_tag,"Oprtr"));
    	basicNameValuePairs.add(new Pair<>(Constants.appVer_tag,"0.1"));
    	basicNameValuePairs.add(new Pair<>(Constants.pduLogVisibility_tag,"OperatorOnly"));
    	basicNameValuePairs.add(new Pair<>(Constants.billing_tag,"None"));
    	basicNameValuePairs.add(new Pair<>(Constants.deviceId_tag,getMacAddress()));
    }
	
	String getMacAddress(){
		WifiManager manager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		WifiInfo info = manager.getConnectionInfo();
		return info.getMacAddress();
	}

	
	@Override
	public void onClick(View v) {
		
		switch(v.getId()){ // only one button
			case R.id.bLogin:{
				if (authenticateThreadRunning) break;
				String userId = etUser.getText().toString().trim();
				String pass = etPwd.getText().toString().trim();
				if (userId==null || userId.equals("") || 
						pass==null || pass.equals("")){
					toast("Please check the fields..!!"); }
				else  {
					authenticateThreadRunning = true;
					new AuthenticateTask().execute();
				}
			}
		}
	}
// ToastThread to display a toast message (used from an AsyncTask's doInBackground method, where the access to the 
// UI elements is not allowed
 private class ToastThread implements Runnable{
		String msg = null;
		ToastThread(String msg){
			this.msg = msg;
		}
		@Override
		public void run() {
			Toast.makeText(AuthenticationActivity.this,msg,Toast.LENGTH_LONG).show();
		}
	}
	
 
 // this task does the authentication of the user  
public class AuthenticateTask extends AsyncTask<Void,Void,Void>{	
	
	@Override
	protected Void doInBackground(Void... params) {
		String url = null;
		try {
		url = getUrl();	
		String userId = etUser.getText().toString().trim();
		String pass = etPwd.getText().toString().trim();
		
		ArrayList<Pair<String,String>> nameValuePairs = new ArrayList<>(curNameValuePairs);
    	nameValuePairs.add(new Pair<>(Constants.name_tag,"Login"));
    	nameValuePairs.add(new Pair<>(Constants.userId_tag,userId));
    	nameValuePairs.add(new Pair<>(Constants.Password_tag,pass));
/*    	
    	SchemeRegistry schemeRegistry = new SchemeRegistry();
    	schemeRegistry.register(new Scheme("https", 
    	            SSLSocketFactory.getSocketFactory(), 443));*/
//------------------
    	/*
    	HttpParams par = new BasicHttpParams();
    	SingleClientConnManager mgr = new SingleClientConnManager(par, schemeRegistry);
    	HttpClient httpclient = new DefaultHttpClient(mgr, par);*/
   //-------------------
    	//HttpClient httpclient = new MyHttpClient(getApplicationContext());
		/*
    	HttpClient httpclient = new DefaultHttpClient();//();
    	//HttpClient httpclient = createHttpClient();
    	HttpPost httppost = new HttpPost("http://"+url+"/HubSrvr/Oprtr");*/

		URL urlnew = new URL("http://"+url+"/HubSrvr/Oprtr");

		HttpURLConnection urlConnection = (HttpURLConnection) urlnew.openConnection();
		urlConnection.setReadTimeout(60000); //one minute
		urlConnection.setConnectTimeout(60000); //one minute
			urlConnection.setRequestMethod("POST");
		urlConnection.setDoInput(true);
		urlConnection.setDoOutput(true);
			OutputStream os = urlConnection.getOutputStream();
			BufferedWriter writer = new BufferedWriter(
					new OutputStreamWriter(os, "UTF-8"));
			writer.write(Helpers.getUrlFromNameValue(nameValuePairs));

			writer.flush();
			writer.close();
			os.close();
			int responseCode=urlConnection.getResponseCode();



      //  int responseCode = response.getStatusLine().getStatusCode();
            Log.v("ERROR", "Response Code => " + responseCode);
          if (responseCode==HttpURLConnection.HTTP_OK){
			  String cookie = null,cookieID=null;
			  Map<String, List<String>> map = urlConnection.getHeaderFields();
			  for (Map.Entry<String, List<String>> entry : map.entrySet())
			  {
				  if (entry.getKey() == null)
					  continue;
				  if(entry.getKey().toString().startsWith("Set-Cookie")){
					  cookie = entry.getValue().toString();
					  if (cookie.startsWith("["+Constants.JSESSIONID)) {
						  boolean ffl = false;
						  for(int i = 0 ; i<cookie.length();++i)
						  {
							  if(ffl==false)
							  {if(cookie.charAt(i)=='=')
							  {
								  ffl = true;
							  }}
							  else
							  {
								  if(cookie.charAt(i)==';')
								  {
									  break;

								  }
								  else
								  {
									  if(cookieID == null)cookieID="";
									  cookieID+=cookie.charAt(i);
								  }
							  }
						  }

						  break;
					  }

			  }


			  }

        	  /*
			  Header[] headers = response.getAllHeaders();
              String cookie = null,cookieID=null;
  			for (int i = 0; i < headers.length; i++) {
                  System.out.println(headers[i] + "");
                  if (headers[i].toString().startsWith("Set-Cookie: ")){// JSESSIONID")){
                      cookie = headers[i].toString().substring(12);
                      if (cookie.startsWith(Constants.JSESSIONID)) {
                          cookieID = cookie.substring(11,11+32);
                          break;
                      }
                  } }
  			*/
        	 SharedPreferences.Editor editor = settings.edit();
		     editor.putString(Constants.USERNAME,userId);
		     editor.putString(Constants.JSESSIONID,cookieID);
		     editor.commit();
		     // Remember the user if the 'RememberMe' checkbox is checked!!
		     curNameValuePairs.add(new Pair<>(Constants.JSESSIONID,cookieID));
		     curNameValuePairs.add(new Pair<>(Constants.userId_tag,userId));
		     
		     if (cbRememberMe.isChecked()) {
		    	 users.put(userId,pass);
		    	 helper.addUser(userId, pass);
		     }
		    runOnUiThread(new ToastThread(getResources().getString(R.string.Logged_in)));
			startActivity(new Intent(AuthenticationActivity.this,MainActivity.class)); 
			AuthenticationActivity.this.finish();
			}
            else {
		    	 runOnUiThread(new ToastThread(getResources().getString(R.string.cannot_login))); 
		     }
				} 
		catch(IOException e){
			runOnUiThread(new ToastThread(getResources().getString(R.string.HostConnRefused))); 
		}
		catch(Exception e){
			e.printStackTrace();
			runOnUiThread(new ToastThread("Exception : "+e)); 
		}
		authenticateThreadRunning = false;	
		return null;
			}
}


public static void editPreferences(String key,String value){
	SharedPreferences.Editor editor = settings.edit();
	editor.putString(key, value);
	editor.commit();
}

//displays the toast message
void toast(String s){
	Toast.makeText(AuthenticationActivity.this,s,Toast.LENGTH_SHORT).show();
}

public static ArrayList<Pair<String,String>> getCurNamevaluePairs(){

	return curNameValuePairs;
}

public static void removePrefs(String key){
	SharedPreferences.Editor editor = settings.edit();
	editor.remove(key);
	editor.commit();
}

public static void addPrefs(String key,String value){
	SharedPreferences.Editor editor = settings.edit();
	editor.putString(key, value);
	editor.commit();
}

public static String getPrefs(String key,String def){
	return settings.getString(key,def);
}

public static String getUrl() throws Exception {
	if (settings.contains(Constants.URL)) return settings.getString(Constants.URL,null);
	else throw new Exception("URL not set in settings!!");
}

// ItemClick handles the event when the user name from drop down is selected and fills the password for the respective 
// user , which it remembers with an sqlite database
@Override
public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
	// TODO Auto-generated method stub
	String user = etUser.getText().toString();
	String curPass = users.get(user);
	etPwd.setText((CharSequence)curPass);
}
}

