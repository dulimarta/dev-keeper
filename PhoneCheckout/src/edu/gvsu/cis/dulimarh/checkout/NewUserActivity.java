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

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.SaveCallback;

import java.util.List;

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
                saveUser (email, uname);
                return true;

        }
        return super.onOptionsItemSelected(item);
    }

    private void saveUser (final String email, final String uname)
    {
        ParseQuery<ParseObject> userQuery = new ParseQuery<ParseObject>(Consts.USER_TABLE);
        userQuery.whereEqualTo("user_id", email);
        userQuery.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> parseObjects, ParseException e) {
                if (e == null) {
                    if (parseObjects.isEmpty()) {
                        ParseObject newUser = new ParseObject(Consts.USER_TABLE);
                        newUser.put("user_id", email);
                        newUser.put("user_name", uname);
                        newUser.saveInBackground(new SaveCallback() {
                            @Override
                            public void done(ParseException e) {
                                newUserAdded = true;
                                setResult(Activity.RESULT_OK);
                                finish();

                            }
                        });
                    } else {
                        Toast.makeText(NewUserActivity.this,
                                "User with the same email already exists",
                                Toast.LENGTH_LONG).show();
                    }

                } else
                    Toast.makeText(NewUserActivity.this,
                            "Unable to load user table", Toast.LENGTH_LONG).show();
            }
        });

    }
    @Override
    public void onClick(View v) {
        /* TODO: capture image */
    }
}
