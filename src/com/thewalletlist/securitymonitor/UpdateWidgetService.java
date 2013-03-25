package com.thewalletlist.securitymonitor;

import android.app.PendingIntent;
import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.RemoteViews;
import android.graphics.Color;
import java.util.Date;
import android.text.format.DateUtils;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;

public class UpdateWidgetService extends Service {

  @Override
  public void onStart(Intent intent, int startId) {
    Log.i(C.LOG, "Called");

    AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this
        .getApplicationContext());

    int[] allWidgetIds = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS);

    ComponentName thisWidget = new ComponentName(getApplicationContext(),
        MyWidgetProvider.class);
    int[] allWidgetIds2 = appWidgetManager.getAppWidgetIds(thisWidget);
    Log.w(C.LOG, "From Intent: " + String.valueOf(allWidgetIds.length));
    Log.w(C.LOG, "Direct: " + String.valueOf(allWidgetIds2.length));

    for (int widgetId : allWidgetIds) {

      SharedPreferences prefs =
        PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());

      int res = Util.update(prefs, widgetId);

      RemoteViews remoteViews = new RemoteViews(this
          .getApplicationContext().getPackageName(),
          R.layout.widget_layout);

      // choose where to redirect when we click.
      Intent clickIntent = null;

      if (res == Util.NO_CHANGE) {
        Log.w(C.LOG, "all is well " + widgetId);
        remoteViews.setTextColor(R.id.status, Color.GREEN);
        remoteViews.setTextViewText(R.id.status, "OK");
        clickIntent = new Intent(this.getApplicationContext(), MainActivity.class);
      } else if (res == Util.CHANGE) {
        Log.w(C.LOG, "something changed!");
        remoteViews.setTextColor(R.id.status, Color.RED);
        remoteViews.setTextViewText(R.id.status, "ALERT");
        clickIntent = new Intent(this.getApplicationContext(), ConfirmChangeActivity.class);
      } else if (res == Util.EMAIL_BLANK) {
        // shouldn't happen
        Log.w(C.LOG, "email not configured");
        clickIntent = new Intent(this.getApplicationContext(), MainActivity.class);
      }

      long lastUpdate = prefs.getLong(C.getLastDateKey(widgetId),0);
      String rangeText = DateUtils.formatSameDayTime(lastUpdate, new Date().getTime(),
        java.text.DateFormat.SHORT, java.text.DateFormat.SHORT).toString();

      remoteViews.setTextViewText(R.id.timestamp, rangeText);
      Log.w(C.LOG, "putting range text: " + rangeText);

      //clickIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
      //clickIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      //clickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, allWidgetIds);
      clickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);

      PendingIntent pendingIntent = PendingIntent.getActivity(this.getApplicationContext(),
         widgetId, clickIntent, PendingIntent.FLAG_UPDATE_CURRENT);
      remoteViews.setOnClickPendingIntent(R.id.widgetpic, pendingIntent);
      appWidgetManager.updateAppWidget(widgetId, remoteViews);
      Log.w(C.LOG, "done registering " + widgetId);
    }
    stopSelf();

    super.onStart(intent, startId);
  }

  @Override
  public IBinder onBind(Intent intent) {
    return null;
  }
}