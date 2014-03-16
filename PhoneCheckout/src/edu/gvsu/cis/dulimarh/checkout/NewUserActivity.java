package edu.gvsu.cis.dulimarh.checkout;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.SaveCallback;

public class NewUserActivity extends Activity implements View.OnClickListener {
    //private String TAG = getClass().getName();

    private EditText uname, email;
    private ImageView uphoto;
    private boolean newUserAdded;
    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        Window win = getWindow();
        win.setFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND , WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        LayoutParams params = win.getAttributes();
        params.width = 800;
        params.dimAmount = 0.3f;
        win.setAttributes(params);
        setContentView(R.layout.activity_newuser);
        uname = (EditText) findViewById(R.id.newuser_name);
        email = (EditText) findViewById(R.id.newuser_email);
        uphoto = (ImageView) findViewById(R.id.newuser_photo);
        uphoto.setOnClickListener(this);
        newUserAdded = false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.new_user_save, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onPause() {
        super.onPause();
        setResult(newUserAdded ? Activity.RESULT_OK : Activity.RESULT_CANCELED);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        String uname, email;
        switch (item.getItemId())
        {
            case R.id.save_user:
                uname = this.uname.getText().toString();
                email = this.email.getText().toString();
                if ("".equals(uname) || "".equals(email)) {
                    Toast.makeText(this, "Please enter both user name and email", Toast.LENGTH_LONG)
                            .show();
                    return true;
                }
                ParseObject newUser = new ParseObject("Users");
                newUser.put("user_id", email);
                newUser.put("user_name", uname);
                newUser.saveInBackground(new SaveCallback() {
                    @Override
                    public void done(ParseException e) {
                        newUserAdded = true;
                        finish();

                    }
                });
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        /* TODO: capture image */
    }
}
