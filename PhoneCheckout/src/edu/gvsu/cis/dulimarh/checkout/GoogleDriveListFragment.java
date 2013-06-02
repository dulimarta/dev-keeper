package edu.gvsu.cis.dulimarh.checkout;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.ListFragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Files;
import com.google.api.services.drive.Drive.Files.List;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

public class GoogleDriveListFragment extends ListFragment {
   final static int REQUEST_SELECT_ACCT = 1;
   final static int REQUEST_AUTH = 2;
   private String TAG = getClass().getName();
   private GoogleAccountCredential credential;
   private Drive service;
   private String acctName;
   private ArrayList<Map<String, String>> allFiles;
   private SimpleAdapter adapter;
   private ListView theList;
   private ProgressBar loadProgress;

   /* (non-Javadoc)
    * @see android.app.Fragment#onCreate(android.os.Bundle)
    */
   @Override
   public void onCreate(Bundle savedInstanceState) {
      // TODO Auto-generated method stub
      super.onCreate(savedInstanceState);
      allFiles = new ArrayList<Map<String,String>>();
      adapter = new SimpleAdapter(getActivity(), allFiles, 
            android.R.layout.simple_list_item_2, 
            new String[] {"name", "type"}, 
            new int[] {android.R.id.text1, android.R.id.text2});
      setListAdapter(adapter);
   }

   /* (non-Javadoc)
    * @see android.app.ListFragment#onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)
    */
   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container,
         Bundle savedInstanceState) {
      View view = inflater.inflate(R.layout.fragment_gdrivelist, container, false);
      loadProgress = (ProgressBar) view.findViewById(R.id.gdrive_progress);
      credential = GoogleAccountCredential.usingOAuth2(getActivity(), DriveScopes.DRIVE);
      SharedPreferences pref = getActivity().getApplicationContext().getSharedPreferences("dev_checkout", Context.MODE_PRIVATE);
      
      /* get the user ID from the shared preference */
      acctName = pref.getString("acctName", null);
      return view;
   }

   
   /* (non-Javadoc)
    * @see android.app.ListFragment#onViewCreated(android.view.View, android.os.Bundle)
    */
   @Override
   public void onViewCreated(View view, Bundle savedInstanceState) {
      super.onViewCreated(view, savedInstanceState);
      if (acctName != null) {
         credential.setSelectedAccountName(acctName);
         
         /* create a Google Drive service */
         service = getDriveService (credential);
         new ListFileTask().execute();
      }
      else
         startActivityForResult(credential.newChooseAccountIntent(),
            REQUEST_SELECT_ACCT);
   }

   /* (non-Javadoc)
    * @see android.app.Fragment#onPause()
    */
   @Override
   public void onPause() {
      // TODO Auto-generated method stub
      super.onPause();
   }

   /* (non-Javadoc)
    * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
    */
   @Override
   public void onActivityResult(int requestCode, int resultCode, Intent data) {
      switch (requestCode)
      {
      case REQUEST_SELECT_ACCT:
         if (resultCode == Activity.RESULT_OK && data != null && data.getExtras() != null)
         {
            String acctName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
            if (acctName != null)
            {
               credential.setSelectedAccountName(acctName);
               SharedPreferences.Editor prefEditor;
               prefEditor = getActivity().getApplicationContext().getSharedPreferences("dev_checkout", Context.MODE_PRIVATE).edit();
               prefEditor.putString("acctName", acctName);
               prefEditor.commit();
               service = getDriveService (credential);
               new ListFileTask().execute();
            }
         }
         break;
      case REQUEST_AUTH:
         if (requestCode == Activity.RESULT_OK) {
            new ListFileTask().execute();
         }
         break;
      }
   }

   private Drive getDriveService (GoogleAccountCredential cred)
   {
      return new Drive.Builder(AndroidHttp.newCompatibleTransport(), 
            new GsonFactory(), cred).build();
   }
   
   private class ListFileTask extends AsyncTask<Void, Integer, Void> {

      /* (non-Javadoc)
       * @see android.os.AsyncTask#onPreExecute()
       */
      @Override
      protected void onPreExecute() {
         theList = getListView();
         theList.setVisibility(View.GONE);
         loadProgress.setVisibility(View.VISIBLE);
      }

      @Override
      protected Void doInBackground(Void... params) {
         try {
            Files xfiles = service.files();
            List request = xfiles.list();
            FileList files = request.execute();
            for (File f : files.getItems())
            {
               if (f.getMimeType().endsWith("spreadsheet")) {
                  Map<String, String> aFile = new HashMap<String, String>();
                  aFile.put("name", f.getTitle());
                  aFile.put("type", f.getMimeType());
                  allFiles.add(aFile);
               }
            }
         } 
         catch (UserRecoverableAuthIOException ioauth)
         {
            startActivityForResult(ioauth.getIntent(), REQUEST_AUTH);
         }
         catch (IOException e) {
            Log.e(TAG, "Drive API generated an exception " + e.getMessage());
            e.printStackTrace();
         }
        return null;
      }

      /* (non-Javadoc)
       * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
       */
      @Override
      protected void onPostExecute(Void result) {
         theList.setVisibility(View.VISIBLE);
         loadProgress.setVisibility(View.GONE);
         adapter.notifyDataSetChanged();
      }
      
      
   }
   
}
