package edu.gvsu.cis.dulimarh.checkout;

import java.io.IOException;

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

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

public class UploadUsersActivity extends Activity {
   private String TAG = getClass().getName();
   final static int REQUEST_SELECT_ACCT = 1;
   final static int REQUEST_AUTH = 2;
   private GoogleAccountCredential credential;
   private Drive service;

   /*
    * (non-Javadoc)
    * 
    * @see android.app.Activity#onCreate(android.os.Bundle)
    */
   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      credential = GoogleAccountCredential.usingOAuth2(this, DriveScopes.DRIVE);
      SharedPreferences pref = getSharedPreferences("dev_checkout", MODE_PRIVATE);
      
      /* get the user ID from the shared preference */
      String acctName = pref.getString("acctName", null);
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
    * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
    */
   @Override
   protected void onActivityResult(int requestCode, int resultCode, Intent data) {
      switch (requestCode)
      {
      case REQUEST_SELECT_ACCT:
         if (resultCode == RESULT_OK && data != null && data.getExtras() != null)
         {
            String acctName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
            if (acctName != null)
            {
               credential.setSelectedAccountName(acctName);
               SharedPreferences.Editor prefEditor;
               prefEditor = getSharedPreferences("dev_checkout", MODE_PRIVATE).edit();
               prefEditor.putString("acctName", acctName);
               prefEditor.commit();
               service = getDriveService (credential);
               new ListFileTask().execute();
            }
         }
         break;
      case REQUEST_AUTH:
         if (requestCode == RESULT_OK) {
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

      @Override
      protected Void doInBackground(Void... params) {
         try {
            Files xfiles = service.files();
            List request = xfiles.list();
            FileList files = request.execute();
            for (File f : files.getItems())
            {
               Log.i(TAG, "Found file " + f.getTitle() + " kind: " + f.getKind());
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
      
   }
}
