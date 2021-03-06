package com.example.daybook;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.TextView;

import org.joda.time.DateTime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

// activity pokazujące się podczas dzwonienia alarmu
public class AlarmReceiverActivity extends Activity {
  private MediaPlayer mMediaPlayer;
  private ExpandableListView listView; // ListView dla wydarzeń
  private ExpandableListAdapter listAdapter; // adapter do ListView
  private ArrayList<Event> todayEvents = new ArrayList<Event>();
  private List<String> mListEvents; // lista wydarzeń na aktualny dzień
  private ArrayList<Event> mEvents = new ArrayList<Event>(); // wydarzenia przekazane z MainActivity
  private HashMap<String, List<String>> listHash; // lista dodatkowych opisów do wydarzeń
  private Alarm mAlarm;
  private Vibrator vibrator;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.requestWindowFeature(Window.FEATURE_NO_TITLE);
    this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
      WindowManager.LayoutParams.FLAG_FULLSCREEN);
    setContentView(R.layout.alarm);

    vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

    // otrzymujemy wydarzenia
    Intent received_intent = getIntent();
    mEvents = (ArrayList<Event>) received_intent.getSerializableExtra((MainActivity.eventExtra));
    mAlarm = received_intent.getParcelableExtra(MainActivity.alarmExtra);

    // iterator dla listy z wydarzeniami
    Iterator mEventsIterator = mEvents.iterator();

    // aktualna data
    String todayDate = new DateTime(DateTime.now()).toString("dd-MM-yyyy");

    // sprawdzamy, które wydrzenie jest na aktualny dzień - dzień alarmu
    while (mEventsIterator.hasNext()) {
      Event event = (Event) mEventsIterator.next();
      if (new DateTime(event.date).toString("dd-MM-yyyy").equals(todayDate)) {
        todayEvents.add(event);
      }
    }

    // jeżeli nie ma wydarzeń wyświetlamy stosowny komunikat, w innym wypadku inicjujemy wszystkie potrzebne rzeczy
    if (todayEvents.isEmpty()) {
      TextView info = (TextView) findViewById(R.id.dayInfo);
      info.setText("You have no events scheduled for today! You can enjoy your day!");
    } else {
      TextView info = (TextView) findViewById(R.id.dayInfo);
      info.setText("Today will be fun! You have these events planned:");

      listView = (ExpandableListView) findViewById(R.id.eventExpandable);
      initData();
      listAdapter = new ExpandableListAdapter(this, mListEvents, listHash);
      listView.setAdapter(listAdapter);
    }

    // przycisk do wyłączenia alarmu i zamknięcia activity
    Button stopAlarm = (Button) findViewById(R.id.stopAlarm);
    stopAlarm.setOnTouchListener(new OnTouchListener() {
      public boolean onTouch(View arg0, MotionEvent arg1) {
        mMediaPlayer.stop();
        vibrator.cancel();
        mAlarm.set = false;

        finish();
        return false;
      }
    });

    playSound(this, getAlarmUri());

    long[] pattern = {0, 100, 1000, 300, 200, 100, 500, 200, 100};
    vibrator.vibrate(pattern, 0);
  }

  // funkcja inicjująca dane - ustawia wydarzenia do listy, dodaje ich opisy
  private void initData() {
    mListEvents = new ArrayList<String>();
    listHash = new HashMap<>();

    for (int i = 0; i < todayEvents.size(); i++) {
      Event event = todayEvents.get(i);
      mListEvents.add(event.title);

      ArrayList<String> eventList = new ArrayList<>();
      if (!event.description.isEmpty()) {
        eventList.add("Additional information: " + event.description);
      }
      listHash.put(mListEvents.get(i), eventList);
    }
  }

  private void playSound(Context context, Uri alert) {
    mMediaPlayer = new MediaPlayer();
    try {
      mMediaPlayer.setDataSource(context, alert);
      final AudioManager audioManager = (AudioManager) context
        .getSystemService(Context.AUDIO_SERVICE);
      if (audioManager.getStreamVolume(AudioManager.STREAM_ALARM) != 0) {
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_ALARM);
        mMediaPlayer.prepare();
        mMediaPlayer.start();
      }
    } catch (IOException e) {
      System.out.println("OOPS");
    }
  }

  // funkcja pobierająca dźwięk alarmu - jak jest ustawiony to go bierze, jak nie, to dźwięk notyfikacji,
  // jak ten też nie ustawiony - to dzwonek
  private Uri getAlarmUri() {
    Uri alert = RingtoneManager
      .getDefaultUri(RingtoneManager.TYPE_ALARM);
    if (alert == null) {
      alert = RingtoneManager
        .getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
      if (alert == null) {
        alert = RingtoneManager
          .getDefaultUri(RingtoneManager.TYPE_RINGTONE);
      }
    }
    return alert;
  }
}
