package edu.gvsu.cis.dulimarh.checkout;

import android.app.Activity;
import android.os.Bundle;

public class UploadUsersActivity extends Activity {
   private String TAG = getClass().getName();

   /*
    * (non-Javadoc)
    * 
    * @see android.app.Activity#onCreate(android.os.Bundle)
    */
   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);

      setContentView(R.layout.activity_gdrivelist);
   }

}
