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

public class ConfirmChangeActivity extends Activity {

  int mAppWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID;

  @Override public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setResult(Activity.RESULT_CANCELED);

    setContentView(R.layout.confirm);

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

  }

  @Override public void onResume() {
    super.onResume();
    display();
  }

  public void display() {
    SharedPreferences prefs =
      PreferenceManager.getDefaultSharedPreferences(this);

    String email = prefs.getString(C.getEmailKey(mAppWidgetId),null);
    String savedResult = prefs.getString(C.getSavedResultKey(mAppWidgetId),null);
    Date savedDate = new Date(prefs.getLong(C.getSavedDateKey(mAppWidgetId),0));
    String lastResult = prefs.getString(C.getLastResultKey(mAppWidgetId),null);
    if (lastResult == null) {
      lastResult = "(email not found)";
    }
    Date lastDate = new Date(prefs.getLong(C.getLastDateKey(mAppWidgetId),0));

    assert (email != null);
    assert (savedResult != null);

    if (savedResult.equals(lastResult)) {
      Intent resultIntent = new Intent();
      resultIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
      setResult(Activity.RESULT_OK, resultIntent);
      finish();
    }

    TextView origDate = (TextView) findViewById(R.id.textview_orig_date);
    origDate.setText(savedDate.toString());
    TextView origEmail = (TextView) findViewById(R.id.textview_orig_email);
    origEmail.setText(email);
    TextView origRes = (TextView) findViewById(R.id.textview_orig_result);
    origRes.setText(Util.displayResult(savedResult));

    TextView newDate = (TextView) findViewById(R.id.textview_new_date);
    newDate.setText(lastDate.toString());
    TextView newEmail = (TextView) findViewById(R.id.textview_new_email);
    newEmail.setText(email);
    TextView newRes = (TextView) findViewById(R.id.textview_new_result);
    newRes.setText(Util.displayResult(lastResult));
  }

  public void doAuthorizeButton(View view) {
    SharedPreferences prefs =
      PreferenceManager.getDefaultSharedPreferences(this);
    SharedPreferences.Editor e = prefs.edit();
    String lastResult = prefs.getString(C.getLastResultKey(mAppWidgetId),null);
    if (lastResult == null) {
      Toast.makeText(this, "email not found, nothing to check", Toast.LENGTH_LONG).show();
      return;
    }
    Date lastDate = new Date(prefs.getLong(C.getLastDateKey(mAppWidgetId),0));
    e.putString(C.getSavedResultKey(mAppWidgetId),lastResult);
    e.putLong(C.getSavedDateKey(mAppWidgetId),lastDate.getTime());
    e.apply();

    // Build the intent to call the service
    Intent intent = new Intent(this, UpdateWidgetService.class);
    int[] widgetIds = { mAppWidgetId };
    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds);

    // Update the widget via the service
    startService(intent);

    finish();
  }

  public void doLookupButton(View view) {
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
    new LookupTask().execute(prefs);
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
        Intent intent = new Intent(ConfirmChangeActivity.this, MainActivity.class);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, mAppWidgetId);
        startActivity(intent);
      } else if (updateResult == Util.CHANGE) {
        Log.d(C.LOG, "change");
        display();
      } else if (updateResult == Util.EMAIL_BLANK) {
        Log.d(C.LOG, "email blank");
        Toast.makeText(ConfirmChangeActivity.this, "bug? invalid email", Toast.LENGTH_LONG).show();
      }
    }
  }

}
