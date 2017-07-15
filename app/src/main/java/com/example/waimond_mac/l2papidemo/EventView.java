package com.example.waimond_mac.l2papidemo;

import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import org.json.JSONObject;

import java.util.Calendar;


public class EventView extends ActionBarActivity {

    private String TAG = CalendarView.class.getName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_event_view);

        TextView view = (TextView) findViewById(R.id.EventTitle);
        Intent intent = getIntent();
        String data = intent.getStringExtra("object");
        Calendar cal = Calendar.getInstance();
        try {
            JSONObject dataObj = new JSONObject(data);
            java.util.Date startTime = new java.util.Date(dataObj.getLong("eventDate") * 1000);
            java.util.Date endTime = new java.util.Date(dataObj.getLong("endDate") * 1000);
            view.setText("Title: " +  dataObj.getString("title") + "\n"
                        + " Event date: " + startTime + "\n"
                        + " End date: " + endTime + "\n"
            );
        } catch (Exception e)
        {
            Log.d(TAG, "json exception: " + e);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_event_view, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
