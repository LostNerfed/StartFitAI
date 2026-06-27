package com.example

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews

class MealWidget : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.widget_meal_layout)

        // Set pending intents for quick actions
        views.setOnClickPendingIntent(R.id.btn_nutrition, getPendingIntentForUrl(context, "synergyfit://add-meal-general", 101))
        views.setOnClickPendingIntent(R.id.btn_train, getPendingIntentForUrl(context, "synergyfit://tab-plan", 102))
        views.setOnClickPendingIntent(R.id.btn_progress, getPendingIntentForUrl(context, "synergyfit://tab-progress", 103))
        views.setOnClickPendingIntent(R.id.btn_coach, getPendingIntentForUrl(context, "synergyfit://ask-ai", 104))

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun getPendingIntentForUrl(context: Context, url: String, requestCode: Int): PendingIntent {
        val intent = Intent(
            Intent.ACTION_VIEW,
            Uri.parse(url)
        ).apply {
            setClass(context, MainActivity::class.java)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
