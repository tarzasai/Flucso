package net.ggelardi.flucso;

import net.ggelardi.flucso.Commons.PK;
import net.ggelardi.flucso.FFAPI.Entry;
import net.ggelardi.flucso.FFAPI.Entry.Comment;
import net.ggelardi.flucso.FFAPI.Feed;
import net.ggelardi.flucso.FFAPI.Feed.Realtime;
import retrofit.RetrofitError;
import android.annotation.TargetApi;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Build;
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class FFService extends IntentService implements OnSharedPreferenceChangeListener {
	public static final String SERVICE_ERROR = "net.ggelardi.flucso.FFService.SERVICE_ERROR";
	public static final String PROFILE_READY = "net.ggelardi.flucso.FFService.PROFILE_READY";
	public static final String DM_BASE_NOTIF = "net.ggelardi.flucso.FFService.DM_BASE_NOTIF";

	public static final int NOTIFICATION_ID = 1;
	
	private FFSession session;
	private LocalBroadcastManager notifier;
	
	private Boolean terminated = false;
	private long printv; // profile update interval (ms)
	private long dmintv; // direct messages update interval (ms)
	private int dmnotf; // direct messages notification type
	private Realtime dmlast;
	
	public FFService() {
		super("FFService");
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		super.onStartCommand(intent, flags, startId);
		
		return START_STICKY;
	}
	
	@Override
	protected void onHandleIntent(Intent intent) {
		Log.v("FFService", "onHandleIntent()");
		
		session = FFSession.getInstance(this);
		notifier = LocalBroadcastManager.getInstance(this);
		
		printv = session.getPrefs().getInt(PK.SERV_PROF, 0);
		dmintv = session.getPrefs().getInt(PK.SERV_MSGS, 0);
		dmnotf = session.getPrefs().getInt(PK.SERV_NOTF, 0);
		
		session.getPrefs().registerOnSharedPreferenceChangeListener(this);
		
		int waitTime = 500;
		try {
			while (!terminated) {
				if (isLollipopPSorSO())
					waitTime = 60000;
				else if (!session.hasAccount())
					waitTime = 2000;
				else if (!checkProfile())
					terminated = true;
				else {
					checkMessages();
					checkFeedCache();
					waitTime = 50000;
				}
				Thread.sleep(waitTime);
			}
		} catch (Exception e) {
			terminated = true;
			notifyError(e);
		} finally {
			stopSelf();
		}
	}
	
	@Override
	public void onDestroy() {
		Log.v("FFService", "onDestroy()");
		
		session.getPrefs().unregisterOnSharedPreferenceChangeListener(this);
		
		super.onDestroy();
	}
	
	private void notifyError(Throwable error) {
		String text;
		try {
			text = error instanceof RetrofitError ? Commons.retrofitErrorText((RetrofitError) error) : error.getMessage();
		} catch (Exception err) {
			text = "Unknown error";
		}
		notifier.sendBroadcast(new Intent().setAction(SERVICE_ERROR).putExtra("message", text));
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	private boolean isLollipopPSorSO() {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
			return false;
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		return pm.isPowerSaveMode() || !pm.isInteractive();
	}
	
	private boolean checkProfile() {
		if (session.hasProfile() && session.getProfile().getAge() <= printv * 60000)
			return true;
		Log.v("FFService", "checkProfile()");
		session.setProfile(FFAPI.client_profile(session).get_profile_sync("me"));
		session.setNavigation(FFAPI.client_profile(session).get_navigation_sync());
		notifier.sendBroadcast(new Intent(PROFILE_READY));
		return true;
	}
	
	private void checkMessages() {
		if (dmnotf == 0)
			return;
		long chk = dmlast != null ? dmlast.timestamp : session.getPrefs().getLong(PK.SERV_MSGS_TIME, 0);
		if (chk > 0 && System.currentTimeMillis() - chk <= dmintv * 60000)
			return;
		Log.v("FFService", "checkMessages()");
		String cursor = dmlast != null ? dmlast.cursor : session.getPrefs().getString(PK.SERV_MSGS_CURS, "");
		Feed data = FFAPI.client_msgs(session).get_feed_updates("filter/direct", 50, cursor, 0, 1);
		dmlast = data.realtime;
		// save the token
		SharedPreferences.Editor editor = session.getPrefs().edit();
		editor.putString(PK.SERV_MSGS_CURS, dmlast.cursor);
		editor.putLong(PK.SERV_MSGS_TIME, System.currentTimeMillis());
		editor.commit();
		// check for updates
		boolean news = false;
		boolean upds = false;
		for (Entry e : data.entries) {
			if (news)
				break;
			if (e.from.isMe()) {
				if (!upds && replied(e))
					upds = true;
			} else if (dmnotf == 2 || (e.to.length == 1 && e.to[0].isMe())) {
				if (e.created)
					news = true;
				else if (!upds && (e.updated || replied(e)))
					upds = true;
			}
		}
		if (news || upds) {
			PendingIntent rpi = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class).setAction(DM_BASE_NOTIF),
				PendingIntent.FLAG_UPDATE_CURRENT);
			NotificationCompat.Builder ncb = new NotificationCompat.Builder(this).setSmallIcon(
				R.drawable.ic_launcher).setContentTitle(getResources().getString(R.string.app_name)).setContentText(
				getResources().getString(news ? R.string.notif_dm_new : R.string.notif_dm_upd)).setContentIntent(rpi);
			NotificationManager nmg = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			nmg.notify(NOTIFICATION_ID, ncb.build());
		}
	}
	
	private void checkFeedCache() {
		if (!Commons.isOnWIFI(this) || (session.cachedFeed != null && session.cachedFeed.getAge() < 10*60*1000))
			return;
		Log.v("FFService", "checkFeedCache()");
		String fid = session.cachedFeed != null ? session.cachedFeed.id :
			session.getPrefs().getString(PK.STARTUP, "home");
		String cur = session.cachedFeed != null && session.cachedFeed.realtime != null ?
			session.cachedFeed.realtime.cursor : "";
		if (cur == "") {
			session.cachedFeed = FFAPI.client_feed(session).get_feed_normal(fid, 0, 20);
			session.cachedFeed.realtime = FFAPI.client_feed(session).get_feed_updates(fid, 20, "", 0, 1).realtime;
		} else {
			session.cachedFeed.update(FFAPI.client_feed(session).get_feed_updates(fid,
				session.cachedFeed.entries.size(), session.cachedFeed.realtime.cursor, 0, 1));
		}
	}
	
	private static boolean replied(Entry e) {
		for (Comment c: e.comments)
			if ((c.created || c.updated) && !c.from.isMe())
				return true;
		return false;
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals(PK.SERV_PROF)) {
			printv = sharedPreferences.getInt(PK.SERV_PROF, 0);
			Log.v("FFService", "printv: " + Long.toString(printv));
		} else if (key.equals(PK.SERV_NOTF)) {
			dmnotf = session.getPrefs().getInt(PK.SERV_NOTF, 0);
			Log.v("FFService", "dmnotf: " + Integer.toString(dmnotf));
		} else if (key.equals(PK.SERV_MSGS)) {
			dmintv = sharedPreferences.getInt(PK.SERV_MSGS, 0);
			Log.v("FFService", "dmintv: " + Long.toString(dmintv));
		}
	}
}