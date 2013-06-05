package edu.gvsu.cis.dulimarh.checkout;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
//import java.util.List;
import java.util.Map;

import org.apache.http.impl.client.DefaultHttpClient;

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
import android.widget.Toast;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpStatusCodes;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Files;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.gdata.client.Service.GDataRequestFactory;
import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.BaseEntry;
import com.google.gdata.data.ParseSource;
import com.google.gdata.data.spreadsheet.CellEntry;
import com.google.gdata.data.spreadsheet.CellFeed;
import com.google.gdata.data.spreadsheet.ListEntry;
import com.google.gdata.data.spreadsheet.ListFeed;
import com.google.gdata.data.spreadsheet.SpreadsheetEntry;
import com.google.gdata.data.spreadsheet.SpreadsheetFeed;
import com.google.gdata.data.spreadsheet.WorksheetEntry;
//import com.google.gdata.client.spreadsheet.SpreadsheetService;
//import com.google.gdata.data.ParseSource;
//import com.google.gdata.data.spreadsheet.SpreadsheetEntry;
//import com.google.gdata.data.spreadsheet.SpreadsheetFeed;
//import com.google.gdata.data.spreadsheet.WorksheetEntry;
//import com.google.gdata.util.ServiceException;
import com.google.gdata.util.ServiceException;

public class GoogleDriveListFragment extends ListFragment {
   final static int REQUEST_SELECT_ACCT = 1;
   final static int REQUEST_AUTH = 2;
   private static String SPREADSHEET_SCOPE = 
         "https://spreadsheets.google.com/feeds";
   private static String SPREADSHEET_FEED_URL = 
         SPREADSHEET_SCOPE + "/spreadsheets/";
   private String TAG = getClass().getName();
   private GoogleAccountCredential credential;
   private Drive service;
   private SpreadsheetService ssService;
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
            new String[] {"name", "mod"}, 
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
      
      /* Allow multiple scopes: Drive and Spreadsheet APIs */
      credential = GoogleAccountCredential.usingOAuth2(getActivity(), 
            DriveScopes.DRIVE, SPREADSHEET_SCOPE);
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
         ssService = new SpreadsheetService("HansDevCheckout");
         ssService.setProtocolVersion(SpreadsheetService.Versions.V3);
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
               try {
                  credential.setSelectedAccountName(acctName);
                  /* create a Google Drive service */
                  service = getDriveService (credential);
                  ssService = new SpreadsheetService("HansDevCheckout");
                  ssService.setProtocolVersion(SpreadsheetService.Versions.V3);
                  SharedPreferences.Editor prefEditor;
                  prefEditor = getActivity().getApplicationContext().getSharedPreferences("dev_checkout", Context.MODE_PRIVATE).edit();
                  prefEditor.putString("acctName", acctName);
                  prefEditor.commit();
                  new ListFileTask().execute();
               } catch (Exception e) {
                  Log.e(TAG, "Generated exception: " + e.getMessage());
                  e.printStackTrace();
               }
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

   /* (non-Javadoc)
    * @see android.app.ListFragment#onListItemClick(android.widget.ListView, android.view.View, int, long)
    */
   @Override
   public void onListItemClick(ListView l, View v, int position, long id) {
      Map<String,String> selection = allFiles.get(position);
      Toast.makeText(getActivity(), "Selecting " + selection.get("name"), 
            Toast.LENGTH_LONG).show();
//      new FileDownloadTask().execute(selection.get("name"), selection.get("id"),
//            selection.get("url")); 
   }

   private Drive getDriveService (GoogleAccountCredential cred)
   {
      return new Drive.Builder(AndroidHttp.newCompatibleTransport(), 
            new GsonFactory(), cred).build();
   }
   
   private Comparator<Map<String,String>> fileNameComparator =
         new Comparator<Map<String,String>>() {

            @Override
            public int compare(Map<String, String> obj1,
                  Map<String, String> obj2) {
               return obj1.get("name").toLowerCase().
                     compareTo(obj2.get("name").toLowerCase());
            }
         };

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
            GoogleCredential gc = new GoogleCredential();
            gc.setAccessToken(credential.getToken());
            ssService.setOAuth2Credentials(gc);
            SpreadsheetFeed ssFeed = ssService.getFeed(new URL(SPREADSHEET_FEED_URL), 
                  SpreadsheetFeed.class);
            List<SpreadsheetEntry> entries = ssFeed.getEntries();
            StringBuilder sb = new StringBuilder();
            int totalEntries = entries.size();
            int k = 0;
            for (SpreadsheetEntry e : entries) {
               publishProgress(k, totalEntries);
               WorksheetEntry ws = e.getDefaultWorksheet();
               if (ws == null) continue;
               URL cFeedURL = new URI(ws.getCellFeedUrl().toString() +
                        "?max-row=1&max-col=2").toURL();
               CellFeed cFeed = ssService.getFeed(cFeedURL, CellFeed.class);
               sb.setLength(0);
               List<CellEntry> colList = cFeed.getEntries();
               if (colList.size() >= 2)
               {
                  Map<String, String> aFile = new HashMap<String, String>();
                  aFile.put("name", e.getTitle().getPlainText());
                  sb.append(" " + colList.get(0).getCell().getInputValue());
                  sb.append(" " + colList.get(1).getCell().getInputValue());
                  aFile.put("mod", sb.toString());
                  allFiles.add(aFile);
               }
               k++;
            }
            Collections.sort(allFiles, fileNameComparator);
         } 
         catch (UserRecoverableAuthIOException ioauth)
         {
            startActivityForResult(ioauth.getIntent(), REQUEST_AUTH);
         }
         catch (IOException e) {
            Log.e(TAG, "Drive API generated an exception " + e.getMessage());
            e.printStackTrace();
         } catch (ServiceException e) {
            Log.e(TAG, "Spreadsheet API generated an exception " + e.getMessage());
            e.printStackTrace();
         } catch (GoogleAuthException e1) {
            Log.e(TAG, "Credential related exception " + e1.getMessage());
            e1.printStackTrace();
         } catch (URISyntaxException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
         }
        return null;
      }

      
      /* (non-Javadoc)
       * @see android.os.AsyncTask#onProgressUpdate(Progress[])
       */
      @Override
      protected void onProgressUpdate(Integer... values) {
         loadProgress.setProgress(values[0]);
         loadProgress.setMax(values[1]);
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

   private class FileDownloadTask extends AsyncTask<String, Void, Void>
   {
      /* (non-Javadoc)
       * @see android.os.AsyncTask#onPreExecute()
       */
      @Override
      protected void onPreExecute() {
         loadProgress.setVisibility(View.VISIBLE);
      }

      @Override
      protected Void doInBackground(String... params) {
         try {
            HttpRequestFactory reqFactory = service.getRequestFactory();
//            URL spreadsheetURL = new URL(SPREADSHEET_FEED_URL + params[1]);
//            GenericUrl ssURL = new GenericUrl(SPREADSHEET_FEED_URL + params[1]);
            GenericUrl ssURL = new GenericUrl(params[2]);
            HttpRequest req = reqFactory.buildGetRequest(ssURL);
            HttpResponse resp = req.execute();
            if (resp.getStatusCode() == HttpStatusCodes.STATUS_CODE_OK)
            {
               Scanner scan = new Scanner (resp.getContent());
               while (scan.hasNextLine())
               {
                  Log.d(TAG, scan.nextLine());
               }
               ParseSource ps = new ParseSource(resp.getContent());
               SpreadsheetEntry ssEntry = new SpreadsheetEntry(SpreadsheetEntry.readEntry(ps));
               Log.d(TAG, "PlainText " + ssEntry.getPlainTextContent());
               Log.d(TAG, "Text " + ssEntry.getTextContent().toString());
            }
         } catch (Exception e) {
            Log.e(TAG, "Exception generated: " + e.getMessage());
            e.printStackTrace();
         }
         return null;
      }

      @Override
      protected void onPostExecute(Void result) {
//         theList.setVisibility(View.VISIBLE);
         loadProgress.setVisibility(View.GONE);
//         adapter.notifyDataSetChanged();
      }
   }
}
