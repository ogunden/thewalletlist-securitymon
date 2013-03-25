package com.thewalletlist.securitymonitor;

import java.util.Random;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.RemoteViews;

/* could store the list of appWidgetIds myself because AppWidgetManager is returning
 * a bunch of previously-deleted ones, and that would reduce extraneous network
 * traffics.. */

public class MyWidgetProvider extends AppWidgetProvider {

  @Override
  public void onUpdate(Context context, AppWidgetManager appWidgetManager,
      int[] appWidgetIds) {

    Log.w(C.LOG, "onUpdate method called");
    // Get all ids
    ComponentName thisWidget = new ComponentName(context,
        MyWidgetProvider.class);
    int[] allWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget);

    // Build the intent to call the service
    Intent intent = new Intent(context.getApplicationContext(),
        UpdateWidgetService.class);
    intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, allWidgetIds);

    // Update the widgets via the service
    context.startService(intent);

  }
}
