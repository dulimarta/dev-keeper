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
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnMultiChoiceClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
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
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;

public class GoogleDriveListFragment extends ListFragment {
   private final static int REQUEST_SELECT_ACCT = 1;
   private final static int REQUEST_AUTH = 2;
   private final static int COLUMN_ZERO_USERID = 0;
   private final static int COLUMN_ZERO_USERNAME = 1;
   private static String SPREADSHEET_SCOPE = 
         "https://spreadsheets.google.com/feeds";
   private static String SPREADSHEET_FEED_URL = 
         SPREADSHEET_SCOPE + "/spreadsheets/";
   private String TAG = getClass().getName();
   private GoogleAccountCredential credential;
   private static Context context;
   private Drive service;
   private SpreadsheetService ssService;
   private String acctName;
   private ArrayList<Map<String, String>> allFiles;
//   private ArrayList<WorksheetEntry> allWorksheets;
   private SimpleAdapter adapter;
   private ListView theList;
   private ProgressBar loadProgress;
   private ImageButton cancelLoading;
   private TextView status;
   private boolean listFileTaskRunning;
   private String[] users;
   private ListFileTask listFileTask;
   
   /* (non-Javadoc)
    * @see android.app.Fragment#onCreate(android.os.Bundle)
    */
   @Override
   public void onCreate(Bundle savedInstanceState) {
      // TODO Auto-generated method stub
      super.onCreate(savedInstanceState);
      context = getActivity();
      allFiles = new ArrayList<Map<String,String>>();
//      allWorksheets = new ArrayList<WorksheetEntry>();
      listFileTaskRunning = false;
      listFileTask = null;
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
      cancelLoading = (ImageButton) view.findViewById(R.id.cancel_loading);
      status = (TextView) view.findViewById(R.id.text_info);
      cancelLoading.setOnClickListener(new OnClickListener() {
         
         @Override
         public void onClick(View v) {
            if (listFileTask != null && listFileTaskRunning)
               listFileTask.cancel(true);
         }
      });
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
         listFileTask = new ListFileTask();
         listFileTask.execute();
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
                  listFileTask = new ListFileTask();
                  listFileTask.execute();
               } catch (Exception e) {
                  Log.e(TAG, "Generated exception: " + e.getMessage());
                  e.printStackTrace();
               }
            }
         }
         break;
      case REQUEST_AUTH:
         if (requestCode == Activity.RESULT_OK) {
            listFileTask = new ListFileTask();
            listFileTask.execute();
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

   private class ListFileTask extends AsyncTask<Void, Integer, Integer> {

      /* (non-Javadoc)
       * @see android.os.AsyncTask#onPreExecute()
       */
      @Override
      protected void onPreExecute() {
         listFileTaskRunning = true;
         theList = getListView();
         theList.setVisibility(View.GONE);
         status.setText(R.string.label_gdrive_loading);
         loadProgress.setIndeterminate(true);
         loadProgress.setInterpolator(new BounceInterpolator());
         loadProgress.setVisibility(View.VISIBLE);
         cancelLoading.setVisibility(View.VISIBLE);
      }

      @Override
      protected Integer doInBackground(Void... params) {
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
            loadProgress.setIndeterminate(false);
            for (SpreadsheetEntry e : entries) {
               if (isCancelled()) break;
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
            return allFiles.size();
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
        return 0;
      }

      /* (non-Javadoc)
       * @see android.os.AsyncTask#onCancelled(java.lang.Object)
       */
      @Override
      protected void onCancelled(Integer result) {
         loadProgress.setVisibility(View.GONE);
         cancelLoading.setVisibility(View.GONE);
         status.setText(R.string.label_gdrive_select);
         Toast.makeText(getActivity(), "Operation cancelled", 
               Toast.LENGTH_LONG).show();
         listFileTaskRunning = false;
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
      protected void onPostExecute(Integer result) {
         listFileTaskRunning = false;
         theList.setVisibility(View.VISIBLE);
         loadProgress.setVisibility(View.GONE);
         cancelLoading.setVisibility(View.GONE);
         if (result > 0)
         {
            status.setText(R.string.label_gdrive_select);
            adapter.notifyDataSetChanged();
         }
         else
            Toast.makeText(getActivity(), "Error loading spreadsheets", 
                  Toast.LENGTH_LONG).show();
      }
   }

   private class FileDownloadTask extends AsyncTask<String, Integer, Integer>
   {
      private String fileName;
      /* (non-Javadoc)
       * @see android.os.AsyncTask#onPreExecute()
       */
      @Override
      protected void onPreExecute() {
         loadProgress.setVisibility(View.VISIBLE);
      }

      @Override
      protected Integer doInBackground(String... params) {
         fileName = params[0];
         int rows = Integer.parseInt(params[2]);
         String[][] tempUsers = new String [rows][2];
         try {
            URL cFeedURL = new URI(params[1] +
                  "?max-col=2&min-row=2&max-row=" + params[2]).toURL();
            CellFeed cFeed = ssService.getFeed(cFeedURL, CellFeed.class);
            int count = 0;
            for (CellEntry e: cFeed.getEntries())
            {
               Cell c = e.getCell();
               /* since we begin at row 2, we want to subtract row index by 2 */
               if (c.getValue() == null)
                  break;
               tempUsers[c.getRow() - 2][c.getCol() - 1] = c.getValue();
               publishProgress(count, rows);
               
               if (count < c.getRow())
                  count = c.getRow();
            }
            count--;
            users = null;
            users = new String[count];
            for (int k = 0; k < count; k++)
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
         showDialog(fileName, result);
      }
   }
   
   private static class UploadToParseTask extends AsyncTask<Object, String, Integer>
   {

      private ProgressDialog pdiag;
      
      /* (non-Javadoc)
       * @see android.os.AsyncTask#onPreExecute()
       */
      @Override
      protected void onPreExecute() {
         pdiag = new ProgressDialog(context);
         pdiag.show();
      }

      @Override
      protected Integer doInBackground(Object... params) {
         String[] users = (String[]) params[0];
         String[] toks;
         pdiag.setMax(users.length);
         int layout = (Integer) params[1];
         int k = 0;
         try {
            ParseQuery query = new ParseQuery("TestUsers");
            pdiag.setTitle("Deleting current users");
            for (ParseObject obj : query.find())
            {
               publishProgress("Deleting " + obj.getString("user_id"));
               obj.delete();
            }
            pdiag.setTitle("Uploading new users");
            for (String data : users) {
               toks = data.split("#::#");
               ParseObject userObj = new ParseObject("TestUsers");
               if (toks[0] != null && toks[1] != null ||
                     ! ("null".equals(toks[0]) || "null".equals(toks[1]))) {
                  if (layout == COLUMN_ZERO_USERID) {
                     publishProgress(k + "/" + users.length + " " + toks[0]);
                     userObj.put("user_id", toks[0]);
                     userObj.put("user_name", toks[1]);
                  } else {
                     publishProgress(k + "/" + users.length + " " + toks[1]);
                     userObj.put("user_id", toks[1]);
                     userObj.put("user_name", toks[0]);
                  }
                  userObj.save();
               }
               k++;
            }
            return users.length;
         } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
         }
         return 0;
      }
      
      /* (non-Javadoc)
       * @see android.os.AsyncTask#onProgressUpdate(Progress[])
       */
      @Override
      protected void onProgressUpdate(String... values) {
         pdiag.setMessage(values[0]);
      }

      /* (non-Javadoc)
       * @see android.os.AsyncTask#onPostExecute(java.lang.Object)
       */
      @Override
      protected void onPostExecute(Integer result) {
         pdiag.dismiss();
         if (result == 0)
            Toast.makeText(context, "Error uploading data to Parse server", 
                  Toast.LENGTH_LONG).show();
      }

   }

   private void showDialog(String file, int numRows)
   
   {
      DialogFragment diafrag;
      if (numRows > 0)
         diafrag = UploadConfirmDialog.newInstance(file, users);
      else
         diafrag = EmptyListWarningDialog.newInstance(file);
      diafrag.show(getFragmentManager(), "dialog");
   }
   
   public static class UploadConfirmDialog extends DialogFragment {
      private int columnLayout;
      private String[] usrList;
      
      public static UploadConfirmDialog newInstance(String file, String[] _userList)
      {
         UploadConfirmDialog frag = new UploadConfirmDialog();
         Bundle args = new Bundle();
         args.putString("filename", file);
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
         usrList = getArguments().getStringArray("users");
         String[] toks = usrList[0].split("#::#");
         builder.setTitle("Select column layout of \"" + 
               getArguments().getString("filename") + "\"\n" + 
               "WARNING: existing users will be deleted");
         builder.setSingleChoiceItems(new String[] {
               "userid:" + toks[0] + " username:" + toks[1],
               "userid:" + toks[1] + " username:" + toks[0],
         }, 0, new DialogInterface.OnClickListener() {
            
            @Override
            public void onClick(DialogInterface dialog, int which) {
               columnLayout = which;
            }
         });
         builder.setNegativeButton("Cancel", null);
         builder.setPositiveButton("Upload", new DialogInterface.OnClickListener() {
            
            @Override
            public void onClick(DialogInterface dialog, int which) {
               new UploadToParseTask().execute(usrList, columnLayout);
            }
         });
         return builder.create();
      }
   }
      
   public static class EmptyListWarningDialog extends DialogFragment {
      public static EmptyListWarningDialog newInstance(String file)
      {
         EmptyListWarningDialog frag = new EmptyListWarningDialog();
         Bundle args = new Bundle();
         args.putString("filename",file);
         return frag;
      }

      @Override
      public Dialog onCreateDialog(Bundle savedInstanceState) {
         AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
         builder.setMessage("Cannot upload from an empty spreadsheet " +
               getArguments().getString("filename"));
         builder.setPositiveButton("OK", null);
         return builder.create();
      }
      
   }
}
