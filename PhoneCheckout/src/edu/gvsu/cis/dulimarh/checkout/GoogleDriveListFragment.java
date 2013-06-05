package edu.gvsu.cis.dulimarh.checkout;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
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
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.spreadsheet.Cell;
import com.google.gdata.data.spreadsheet.CellEntry;
import com.google.gdata.data.spreadsheet.CellFeed;
import com.google.gdata.data.spreadsheet.SpreadsheetEntry;
import com.google.gdata.data.spreadsheet.SpreadsheetFeed;
import com.google.gdata.data.spreadsheet.WorksheetEntry;
import com.google.gdata.util.ServiceException;
//import java.util.List;
//import com.google.gdata.client.spreadsheet.SpreadsheetService;
//import com.google.gdata.data.ParseSource;
//import com.google.gdata.data.spreadsheet.SpreadsheetEntry;
//import com.google.gdata.data.spreadsheet.SpreadsheetFeed;
//import com.google.gdata.data.spreadsheet.WorksheetEntry;
//import com.google.gdata.util.ServiceException;

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
//   private ArrayList<WorksheetEntry> allWorksheets;
   private SimpleAdapter adapter;
   private ListView theList;
   private ProgressBar loadProgress;
   private boolean listFileTaskRunning;
   private String[] users;
   
   /* (non-Javadoc)
    * @see android.app.Fragment#onCreate(android.os.Bundle)
    */
   @Override
   public void onCreate(Bundle savedInstanceState) {
      // TODO Auto-generated method stub
      super.onCreate(savedInstanceState);
      allFiles = new ArrayList<Map<String,String>>();
//      allWorksheets = new ArrayList<WorksheetEntry>();
      listFileTaskRunning = false;
      adapter = new SimpleAdapter(getActivity(), allFiles, 
            android.R.layout.simple_list_item_2, 
            new String[] {"name", "columns"}, 
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
      // TODO How to store the running status of the current AsyncTask...
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
                  /* create a Google Drive and SpreadSheet services */
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
      new FileDownloadTask().execute(selection.get("name"),
            selection.get("url"), selection.get("rowCount")); 
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
         listFileTaskRunning = true;
         theList = getListView();
         theList.setVisibility(View.GONE);
         loadProgress.setVisibility(View.VISIBLE);
      }

      @Override
      protected Void doInBackground(Void... params) {
         try {
            
            /* build the GoogleCredential from GoogleAccountCredential */
            GoogleCredential gc = new GoogleCredential();
            gc.setAccessToken(credential.getToken());
            ssService.setOAuth2Credentials(gc);
            
            /* Get the URL to the list of spreadsheets */
            SpreadsheetFeed ssFeed = ssService.getFeed(new URL(SPREADSHEET_FEED_URL), 
                  SpreadsheetFeed.class);
            List<SpreadsheetEntry> entries = ssFeed.getEntries();
            StringBuilder sb = new StringBuilder();
            int totalEntries = entries.size();
            int k = 0;
            allFiles.clear();
//            allWorksheets.clear();
            for (SpreadsheetEntry e : entries) {
               publishProgress(k, totalEntries);
               WorksheetEntry ws = e.getDefaultWorksheet();
//               allWorksheets.add(ws);
               if (ws == null) continue;
               /* get the URL to the list of cells (only the first 
                * two columns on the first row */
               URL cFeedURL = new URI(ws.getCellFeedUrl().toString() +
                        "?max-row=1&max-col=2").toURL();
               CellFeed cFeed = ssService.getFeed(cFeedURL, CellFeed.class);
               sb.setLength(0);
               List<CellEntry> colList = cFeed.getEntries();
               if (colList.size() >= 2)
               {
                  Map<String, String> aFile = new HashMap<String, String>();
                  /* The name of the spreadsheet */
                  aFile.put("name", e.getTitle().getPlainText());
                  sb.append("First 2 columns: ");
                  /* the name of the first two column */
                  sb.append(" " + colList.get(0).getCell().getInputValue());
                  sb.append(" " + colList.get(1).getCell().getInputValue());
                  aFile.put("columns", sb.toString());
                  aFile.put("url", ws.getCellFeedUrl().toString());
                  aFile.put("rowCount", String.valueOf(ws.getRowCount()));
                  aFile.put("colCount", String.valueOf(ws.getColCount()));
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
         listFileTaskRunning = false;
         theList.setVisibility(View.VISIBLE);
         loadProgress.setVisibility(View.GONE);
         adapter.notifyDataSetChanged();
      }
   }

   private class FileDownloadTask extends AsyncTask<String, Integer, Integer>
   {
      /* (non-Javadoc)
       * @see android.os.AsyncTask#onPreExecute()
       */
      @Override
      protected void onPreExecute() {
         loadProgress.setVisibility(View.VISIBLE);
      }

      @Override
      protected Integer doInBackground(String... params) {
         int rows = Integer.parseInt(params[2]);
         String[][] tempUsers = new String [rows][2];
         users = new String[rows];
         try {
            URL cFeedURL = new URI(params[1] +
                  "?max-col=2&min-row=2&max-row=" + params[2]).toURL();
            CellFeed cFeed = ssService.getFeed(cFeedURL, CellFeed.class);
            int k = 0;
            for (CellEntry e: cFeed.getEntries())
            {
               Cell c = e.getCell();
               Log.d(TAG, "R" + c.getRow() + "C" + c.getCol() + " => " +
                     c.getValue());
               /* since we begin at row 2, we want to subtract row index by 2 */
               tempUsers[c.getRow()-2][c.getCol()-1] = c.getValue();
               publishProgress(k, rows);
               k++;
            }
            for (k = 0; k < tempUsers.length; k++)
               users[k] = tempUsers[k][0] + "#::#" + tempUsers[k][1];
               
         } catch (Exception e) {
            Log.e(TAG, "Exception generated: " + e.getMessage());
            e.printStackTrace();
         }
         return users.length;
      }

      /* (non-Javadoc)
       * @see android.os.AsyncTask#onProgressUpdate(Progress[])
       */
      @Override
      protected void onProgressUpdate(Integer... values) {
         loadProgress.setProgress(values[0]);
         loadProgress.setMax(values[1]);
      }

      @Override
      protected void onPostExecute(Integer result) {
//         theList.setVisibility(View.VISIBLE);
         loadProgress.setVisibility(View.GONE);
         showDialog(result);
      }
   }
   
   private void showDialog(int numRows)
   {
      DialogFragment diafrag;
      if (numRows > 0)
         diafrag = UploadConfirmDialog.newInstance(users);
      else
         diafrag = EmptyListWarningDialog.newInstance();
      diafrag.show(getFragmentManager(), "dialog");
   }
   
   public static class UploadConfirmDialog extends DialogFragment {

      public static UploadConfirmDialog newInstance(String[] _userList)
      {
         UploadConfirmDialog frag = new UploadConfirmDialog();
         Bundle args = new Bundle();
         args.putStringArray("users", _userList);
         frag.setArguments(args);
         return frag;
      }
      
      /* (non-Javadoc)
       * @see android.app.DialogFragment#onCreateDialog(android.os.Bundle)
       */
      @Override
      public Dialog onCreateDialog(Bundle savedInstanceState) {
         AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
         String[] usrList;
         usrList = getArguments().getStringArray("users");
         String[] toks = usrList[0].split("#::#");
         builder.setTitle("Select column layout");
         builder.setItems(new String[] {
               "userid:" + toks[0] + " username:" + toks[1],
               "userid:" + toks[1] + " username:" + toks[0],
         }, null);
         builder.setNegativeButton("Cancel", null);
         builder.setPositiveButton("Upload", null);
         return builder.create();
      }
   }
   
   public static class EmptyListWarningDialog extends DialogFragment {
      public static EmptyListWarningDialog newInstance()
      {
         EmptyListWarningDialog frag = new EmptyListWarningDialog();
         return frag;
      }

      @Override
      public Dialog onCreateDialog(Bundle savedInstanceState) {
         AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
         builder.setMessage("Cannot upload from an empty spreadsheet");
         builder.setPositiveButton("OK", null);
         return builder.create();
      }
      
   }
}
