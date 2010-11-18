/*Copyright [2010] [David Van de Ven]

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

 */

package org.wahtod.wififixer;

import java.io.File;
import java.util.List;

import org.wahtod.wififixer.LegacySupport.LegacyLogFile;
import org.wahtod.wififixer.LegacySupport.VersionedLogFile;
import org.wahtod.wififixer.PrefConstants.Pref;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class WifiFixerActivity extends Activity {
    // Is this the paid version?
    public boolean ISFREE = true;
    public boolean ISAUTHED = false;
    public boolean ABOUT = false;
    public boolean LOGGING_MENU = false;
    public boolean LOGGING = false;

    // constants
    private static final int MENU_LOGGING = 1;
    private static final int MENU_SEND = 2;
    private static final int MENU_PREFS = 3;
    private static final int MENU_HELP = 4;
    private static final int MENU_ABOUT = 5;
    private static final int LOGGING_GROUP = 42;
    
    private static final int CONTEXT_ENABLE = 1;
    private static final int CONTEXT_DISABLE = 2;
    private static final int CONTEXT_CONNECT = 3;
    
    private String clicked;
    VersionedLogFile vlogfile;
    // New key for About nag
    // Set this when you change the About xml
    static final String sABOUT = "ABOUT2";
    

    void authCheck() {
	if (!ISAUTHED) {
	    // Handle Donate Auth
	    Intent sendIntent = new Intent(
		    "com.wahtod.wififixer.WFDonateService");
	    startService(sendIntent);
	    nagNotification();
	}
    }

    boolean getLogging() {
	return PrefUtil.readBoolean(this, Pref.LOG_KEY);
    }

    void launchHelp() {
	Intent myIntent = new Intent(this, HelpActivity.class);
	startActivity(myIntent);
    }

    void launchPrefs() {
	startActivity(new Intent(this, PrefActivity.class));
    }

    void sendLog() {

	File file = vlogfile.getLogFile(getBaseContext());
	Log.i("WifiFixerActivity", file.getAbsolutePath());

	if (Environment.getExternalStorageState() != null
		&& !(Environment.getExternalStorageState()
			.contains(Environment.MEDIA_MOUNTED))) {
	    Toast.makeText(WifiFixerActivity.this, "SD Card Unavailable",
		    Toast.LENGTH_LONG).show();

	    return;

	} else if (!file.exists()) {
	    Toast.makeText(WifiFixerActivity.this, "Log doesn't exist",
		    Toast.LENGTH_LONG).show();
	    return;
	}
	setLogging(false);
	Intent sendIntent = new Intent(Intent.ACTION_SEND);
	sendIntent.setType("text/plain");
	sendIntent.putExtra(Intent.EXTRA_EMAIL,
		new String[] { "zanshin.g1@gmail.com" });
	sendIntent.putExtra(Intent.EXTRA_SUBJECT, "WifiFixer Log");
	sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(file.toURI()
		.toString()));
	sendIntent.putExtra(Intent.EXTRA_TEXT,
		"Please include time at which issue occurred and description of the issue:\n\n"
			+ LogService.getBuildInfo());

	startActivity(Intent.createChooser(sendIntent, "Email:"));

    }

    void setIcon() {
	ImageButton serviceButton = (ImageButton) findViewById(R.id.ImageButton01);
	if (PrefUtil.readBoolean(this, Pref.DISABLE_KEY)) {
	    serviceButton.setImageResource(R.drawable.inactive);
	} else {
	    serviceButton.setImageResource(R.drawable.active);
	}
    }

    void setLogging(boolean state) {
	LOGGING = state;
	PrefUtil.writeBoolean(this, Pref.LOG_KEY, state);
	PrefUtil.notifyPrefChange(this, Pref.LOG_KEY);
	if (state) {
	    /*
	     * Delete old log if toggling logging on
	     */
	    if (vlogfile != null) {

	    } else
		vlogfile = new LegacyLogFile();
	    File file = vlogfile.getLogFile(this);
	    file.delete();

	}
    }

    void setText() {
	PackageManager pm = getPackageManager();
	String vers = "";
	try {
	    // ---get the package info---
	    PackageInfo pi = pm.getPackageInfo("org.wahtod.wififixer", 0);
	    // ---display the versioncode--
	    vers = pi.versionName;
	} catch (NameNotFoundException e) {
	    /*
	     * shouldn't ever be not found
	     */
	    e.printStackTrace();
	}
	TextView vButton = (TextView) findViewById(R.id.version);
	vButton.setText(vers.toCharArray(), 0, vers.length());
    }

    void setToggleIcon(Menu menu) {
	MenuItem logging = menu.getItem(MENU_LOGGING - 1);
	if (LOGGING) {
	    logging.setIcon(R.drawable.logging_enabled);
	    logging.setTitle(R.string.turn_logging_off);
	} else {
	    logging.setIcon(R.drawable.logging_disabled);
	    logging.setTitle(R.string.turn_logging_on);
	}

    }

    void showNotification() {

	NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

	// The details of our message
	CharSequence from = "WiFi Fixer";
	PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
		new Intent(this, About.class), 0);
	// construct the NotifUtil object.
	Notification notif = new Notification(R.drawable.icon, "Please Read",
		System.currentTimeMillis());

	// Set the info for the views that show in the notification panel.
	notif.setLatestEventInfo(this, from, "Tap Here to Read About Page",
		contentIntent);
	notif.flags = Notification.FLAG_AUTO_CANCEL;
	nm.notify(4145, notif);

    }

    private static void startwfService(final Context context) {
	context.startService(new Intent(context, WifiFixerService.class));
    }

    void nagNotification() {

	NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

	PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
		new Intent(Intent.ACTION_VIEW, Uri
			.parse("market://details?id=com.wahtod.wififixer")), 0);
	Notification notif = new Notification(R.drawable.icon, "Thank You",
		System.currentTimeMillis());

	RemoteViews contentView = new RemoteViews(getPackageName(),
		R.layout.nag_layout);
	contentView.setImageViewResource(R.id.image, R.drawable.icon);
	contentView
		.setTextViewText(
			R.id.text,
			"Thank you for using Wifi Fixer. If you find this software useful, please tap here to donate.");
	notif.contentView = contentView;
	notif.contentIntent = contentIntent;

	notif.flags = Notification.FLAG_AUTO_CANCEL;

	// hax
	nm.notify(31337, notif);

    }

    private static void removeNag(final Context context) {
	NotificationManager nm = (NotificationManager) context
		.getSystemService(NOTIFICATION_SERVICE);
	nm.cancel(31337);
    }

    void toggleLog() {

	LOGGING = getLogging();
	if (LOGGING) {
	    Toast.makeText(WifiFixerActivity.this, "Disabling Logging",
		    Toast.LENGTH_SHORT).show();
	    setLogging(false);
	} else {
	    if (Environment.getExternalStorageState() != null
		    && !(Environment.getExternalStorageState()
			    .contains("mounted"))) {
		Toast.makeText(WifiFixerActivity.this, "SD Card Unavailable",
			Toast.LENGTH_SHORT).show();
		return;
	    }

	    Toast.makeText(WifiFixerActivity.this, "Enabling Logging",
		    Toast.LENGTH_SHORT).show();
	    setLogging(true);
	}
    }

    // On Create
    @Override
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setTitle(R.string.app_name);
	setContentView(R.layout.main);

	/*
	 * Grab and set up ListView in sliding drawer for network list UI
	 */
	final ListView lv = (ListView) findViewById(R.id.ListView01);
	lv.setTextFilterEnabled(true);
	lv.setAdapter(new ArrayAdapter<String>(this, R.layout.list_item_layout,
		getNetworks(this)));
	lv.setOnItemClickListener(new OnItemClickListener() {
	    @Override
	    public void onItemClick(AdapterView<?> arg0, View v, int position,
		    long id) {
		clicked= lv.getItemAtPosition(position).toString();

	    }

	});
	registerForContextMenu(lv);

	// Set layout version code
	setText();
	oncreate_setup();

    };

    private static final String[] getNetworks(final Context context) {
	WifiManager wm = (WifiManager) context
		.getSystemService(Context.WIFI_SERVICE);
	List<WifiConfiguration> wifiConfigs = wm.getConfiguredNetworks();
	String[] networks = new String[wifiConfigs.size()];
	for (WifiConfiguration wfResult : wifiConfigs) {
	    networks[wfResult.networkId] = wfResult.SSID.replace("\"", "");
	}

	return networks;
    }

    private void oncreate_setup() {
	ISAUTHED = PrefUtil.readBoolean(this, "ISAUTHED");
	ABOUT = PrefUtil.readBoolean(this, sABOUT);
	LOGGING_MENU = PrefUtil.readBoolean(this, "Logging");
	LOGGING = getLogging();
	// Fire new About nag
	if (!ABOUT) {
	    showNotification();

	}

	// Here's where we fire the nag
	authCheck();
	vlogfile = VersionedLogFile.newInstance(this);

    }

    @Override
    public void onStart() {
	super.onStart();
	setIcon();
	LOGGING_MENU = PrefUtil.readBoolean(this, "Logging");
	LOGGING = getLogging();
	startwfService(this);
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v,
	    ContextMenuInfo menuInfo) {
	super.onCreateContextMenu(menu, v, menuInfo);
	/*
	 * Clicked is the stored string triggered in the OnClickListener
	 */
	 menu.setHeaderTitle(clicked);
	    menu.add(0, CONTEXT_ENABLE, 0, "Enable");
	    menu.add(0, CONTEXT_DISABLE, 1, "Disable");
	    menu.add(0,CONTEXT_CONNECT, 2, "Connect Now");
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
	
	return true;
    }

    // Create menus
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
	super.onCreateOptionsMenu(menu);
	menu.add(LOGGING_GROUP, MENU_LOGGING, 0, R.string.toggle_logging)
		.setIcon(R.drawable.logging_enabled);

	menu.add(LOGGING_GROUP, MENU_SEND, 1, R.string.send_log).setIcon(
		R.drawable.ic_menu_send);

	menu.add(Menu.NONE, MENU_PREFS, 2, R.string.preferences).setIcon(
		R.drawable.ic_prefs);

	menu.add(Menu.NONE, MENU_HELP, 3, R.string.documentation).setIcon(
		R.drawable.ic_menu_help);

	menu.add(Menu.NONE, MENU_ABOUT, 4, R.string.about).setIcon(
		R.drawable.ic_menu_info);

	return true;
    }

    /* Handles item selections */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
	super.onOptionsItemSelected(item);

	switch (item.getItemId()) {

	case MENU_LOGGING:
	    toggleLog();
	    return true;

	case MENU_SEND:
	    sendLog();
	    return true;

	case MENU_PREFS:
	    launchPrefs();
	    return true;

	case MENU_HELP:
	    launchHelp();
	    return true;
	case MENU_ABOUT:
	    Intent myIntent = new Intent(this, About.class);
	    startActivity(myIntent);
	    return true;

	}
	return false;
    }

    @Override
    public void onPause() {
	super.onPause();
	removeNag(this);
    }

    @Override
    public void onResume() {
	super.onResume();
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
	super.onPrepareOptionsMenu(menu);
	// Menu drawing stuffs

	if (LOGGING_MENU) {
	    menu.setGroupVisible(LOGGING_GROUP, true);
	    setToggleIcon(menu);

	} else {
	    menu.setGroupVisible(LOGGING_GROUP, false);
	}

	return true;
    }
}