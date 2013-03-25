package com.thewalletlist.securitymonitor;

import android.os.AsyncTask;
import android.util.Log;
import android.app.Activity;
import android.widget.Toast;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.Intent;
import android.widget.Button;
import android.view.View;
import android.widget.TextView;
import android.widget.EditText;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;

import java.util.Date;

public class MainActivity extends Activity {

  int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setResult(RESULT_CANCELED);

    // Find the widget id from the intent.
    Intent intent = getIntent();
    Bundle extras = intent.getExtras();
    if (extras != null) {
      mAppWidgetId = extras.getInt(
        AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
      );
    }

    // If they gave us an intent without the widget id, just bail.
    if (mAppWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
      finish();
    }

    Log.d(C.LOG, "opening mainactivity for widget id " + mAppWidgetId);
    setContentView(R.layout.main);
  }

  @Override public void onResume() {
    super.onResume();
    // prefs could have changed, so refresh the view
    display();
  }

  private boolean isReady() {
    SharedPreferences prefs =
      PreferenceManager.getDefaultSharedPreferences(this);

    String email = prefs.getString(C.getEmailKey(mAppWidgetId),null);
    if (email == null) {
      return false;
    }
    String lastResult = prefs.getString(C.getLastResultKey(mAppWidgetId),null);
    if (lastResult == null) {
      return false;
    }
    long lastDateInt = prefs.getLong(C.getLastDateKey(mAppWidgetId),0);
    if (lastDateInt == 0) {
      return false;
    }

    return true;
  }

  private void display() {
    SharedPreferences prefs =
      PreferenceManager.getDefaultSharedPreferences(this);

    View lookupResLayout = findViewById(R.id.linearlayout_lookupres);

    Log.d(C.LOG, "getting email from key " + C.getEmailKey(mAppWidgetId));
    String email = prefs.getString(C.getEmailKey(mAppWidgetId),null);
    if (email == null) {
      Log.d(C.LOG, "got null email");
      lookupResLayout.setVisibility(View.GONE);
      return;
    }
    Log.d(C.LOG, "got email: " + email);
    Log.d(C.LOG, "looking up prefs with key " + C.getLastResultKey(mAppWidgetId));
    String lastResult = prefs.getString(C.getLastResultKey(mAppWidgetId),null);
    if (lastResult == null) {
      lookupResLayout.setVisibility(View.GONE);
      return;
    }
    long lastDateLong = prefs.getLong(C.getLastDateKey(mAppWidgetId),0);
    assert (lastDateLong != 0);

    EditText emailEditText = (EditText) findViewById(R.id.edittext_email);
    emailEditText.setText(email);

    Date lastDate = new Date(lastDateLong);

    lookupResLayout.setVisibility(View.VISIBLE);

    // display result text
    TextView lookupresTextView = (TextView) findViewById(R.id.textview_lookupres);
    lookupresTextView.setText("As of " + lastDate.toString()+ ":");
    TextView lookupResEmail = (TextView) findViewById(R.id.textview_lookupres_email);
    lookupResEmail.setText(email);
    TextView lookupResResult = (TextView) findViewById(R.id.textview_lookupres_result);
    lookupResResult.setText(Util.displayResult(lastResult));
  }

  public void doLookupButton(View view) {
    EditText emailEditText = (EditText) findViewById(R.id.edittext_email);
    String email = emailEditText.getText().toString().trim().toLowerCase();
    if (!Util.isValidEmail(email)) {
      Toast.makeText(this, "invalid email", Toast.LENGTH_LONG).show();
      return;
    }
    SharedPreferences prefs =
      PreferenceManager.getDefaultSharedPreferences(this);
    String emailPref = prefs.getString(C.getEmailKey(mAppWidgetId),null);
    SharedPreferences.Editor e = prefs.edit();

    if (emailPref == null || !emailPref.equals(email)) {
      Log.d(C.LOG, "saving email " + email + " to wid " + mAppWidgetId);
      // new email, save it
      e.putString(C.getEmailKey(mAppWidgetId),email);
      e.apply();
    }

    String savedResult = prefs.getString(C.getSavedResultKey(mAppWidgetId), null);
    if (savedResult == null) {
      Log.d(C.LOG, "looks like first time");
      // first time
      String res = Util.downloadReq(email);
      if (res == null) {
        Toast.makeText(this,
          "email not recognized. are you signed up on thewalletlist.com?",
          Toast.LENGTH_LONG).show();
        return;
      }
      e.putString(C.getLastResultKey(mAppWidgetId), res);
      e.putLong(C.getLastDateKey(mAppWidgetId), new Date().getTime());
      e.apply();
      display();
    } else {
      // do the lookup
      new LookupTask().execute(prefs);
    }
  }

  private class LookupTask extends AsyncTask<SharedPreferences, Void, Integer> {
    @Override protected Integer doInBackground(SharedPreferences... prefs) {
      int res = Util.update(prefs[0], mAppWidgetId);
      return new Integer(res);
    }
    @Override protected void onPostExecute(Integer updateResultInteger) {
      int updateResult = updateResultInteger.intValue();
      if (updateResult == Util.NO_CHANGE) {
        Log.d(C.LOG, "no change");
        display();
      } else if (updateResult == Util.CHANGE) {
        Log.d(C.LOG, "change");
        Intent intent = new Intent(MainActivity.this, ConfirmChangeActivity.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        startActivity(intent);
      } else if (updateResult == Util.EMAIL_BLANK) {
        Log.d(C.LOG, "email blank");
        Toast.makeText(MainActivity.this, "invalid email", Toast.LENGTH_LONG).show();
      } else if (updateResult == Util.NOT_FOUND) {
        Log.d(C.LOG, "email not found");
        Toast.makeText(MainActivity.this,
          "email not recognized. are you signed up on thewalletlist.com?", Toast.LENGTH_LONG).show();
      }
    }
  }

  public void doDoneButton(View view) {
    if (isReady()) {
      // save the last result as "saved" result:
      SharedPreferences prefs =
        PreferenceManager.getDefaultSharedPreferences(this);
      String lastRes = prefs.getString(C.getLastResultKey(mAppWidgetId), null);
      long lastDate = prefs.getLong(C.getLastDateKey(mAppWidgetId), 0);
      assert (lastRes != null);
      assert (lastDate != 0);
      SharedPreferences.Editor e = prefs.edit();
      e.putString(C.getSavedResultKey(mAppWidgetId), lastRes);
      e.putLong(C.getSavedDateKey(mAppWidgetId), lastDate);
      e.apply();

      // Build the intent to call the service
      Intent intent = new Intent(this, UpdateWidgetService.class);
      int[] widgetIds = { mAppWidgetId };
      intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds);

      // Update the widget via the service
      startService(intent);


      Intent resultIntent = new Intent();
      resultIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
      setResult(Activity.RESULT_OK, resultIntent);
      finish();
    } else {
      Toast.makeText(this, "set your email and run a lookup first", Toast.LENGTH_LONG).show();
    }
  }

}
