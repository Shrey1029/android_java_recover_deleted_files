package com.example.fileminer

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.widget.RemoteViews
import kotlin.random.Random

class PhotoWidgetProvider : AppWidgetProvider() {

    companion object {
        private const val ACTION_REFRESH = "com.example.fileminer.ACTION_REFRESH"
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH) {
            val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
                updateAppWidget(context, AppWidgetManager.getInstance(context), appWidgetId)
            }
        }
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
        val views = RemoteViews(context.packageName, R.layout.photo_widget)

        // Set the image
        val randomImageUri = getRandomImageUri(context)
        if (randomImageUri != null) {
            views.setImageViewUri(R.id.widget_image, randomImageUri)
        } else {
            views.setImageViewResource(R.id.widget_image, R.drawable.ic_image_placeholder) // A placeholder
        }

        // Set up the refresh button pending intent
        val refreshIntent = Intent(context, PhotoWidgetProvider::class.java).apply {
            action = ACTION_REFRESH
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        val refreshPendingIntent = PendingIntent.getBroadcast(
            context,
            appWidgetId, // Use widget ID as request code to ensure uniqueness
            refreshIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setOnClickPendingIntent(R.id.widget_refresh_button, refreshPendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun getRandomImageUri(context: Context): Uri? {
        val projection = arrayOf(MediaStore.Images.Media._ID)
        val cursor: Cursor? = context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            null
        )

        cursor?.use {
            val count = it.count
            if (count > 0) {
                val randomPosition = Random.nextInt(count)
                it.moveToPosition(randomPosition)
                val idColumnIndex = it.getColumnIndex(MediaStore.Images.Media._ID)
                val imageId = it.getLong(idColumnIndex)
                return Uri.withAppendedPath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, imageId.toString())
            }
        }
        return null
    }
}