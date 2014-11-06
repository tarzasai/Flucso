package net.esorciccio.flucso;

import java.util.Date;

import net.esorciccio.flucso.Commons.PK;
import net.esorciccio.flucso.FFAPI.Entry;
import net.esorciccio.flucso.FFAPI.Entry.Comment;
import net.esorciccio.flucso.FFAPI.Feed;
import retrofit.RetrofitError;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class FFService extends IntentService implements OnSharedPreferenceChangeListener {
	public static final String SERVICE_ERROR = "net.esorciccio.flucso.FFService.SERVICE_ERROR";
	public static final String PROFILE_READY = "net.esorciccio.flucso.FFService.PROFILE_READY";
	public static final String DM_BASE_NOTIF = "net.esorciccio.flucso.FFService.DM_BASE_NOTIF";

	public static final int NOTIFICATION_ID = 1;
	
	private FFSession session;
	private LocalBroadcastManager notifier;
	
	private Boolean terminated = false;
	private String cursor;
	private long printv;
	private long dmintv;
	private long prlast;
	private long dmlast;
	private int dmnotf;
	
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
		
		cursor = session.getPrefs().getString(PK.SERV_MSGS_CURS, "");
		printv = session.getPrefs().getInt(PK.SERV_PROF, 0);
		dmnotf = session.getPrefs().getInt(PK.SERV_NOTF, 0);
		dmintv = session.getPrefs().getInt(PK.SERV_MSGS, 0);
		
		session.getPrefs().registerOnSharedPreferenceChangeListener(this);
		
		int waitTime = 500;
		try {
			while (!terminated) {
				try {
					Thread.sleep(waitTime);
				} catch (InterruptedException e) {
					terminated = true;
					notifyError(e);
				}
				if (!session.hasAccount())
					waitTime = 2000;
				else if (!checkProfile())
					terminated = true;
				else {
					checkMessages();
					checkFeedCache();
					waitTime = 5000;
				}
			}
		} finally {
			try {
				SharedPreferences.Editor editor = session.getPrefs().edit();
				editor.putString(PK.SERV_MSGS_CURS, cursor);
				editor.commit();
			} catch (Exception err) {
			}
			stopSelf();
		}
	}
	
	@Override
	public void onDestroy() {
		Log.v("FFService", "onDestroy()");
		
		//terminated = true;
		
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
	
	private boolean checkProfile() {
		if (session.hasProfile() && (printv == 0 || printv > (new Date().getTime() - prlast) / (60 * 1000) % 60))
			return true;
		Log.v("FFService", "checkProfile()");
		try {
			session.profile = FFAPI.client_profile(session).get_profile_sync("me");
			session.initProfile();
			prlast = new Date().getTime();
			if (session.navigation == null)
				session.navigation = FFAPI.client_profile(session).get_navigation_sync();
			notifier.sendBroadcast(new Intent(PROFILE_READY));
			return true;
		} catch (Exception error) {
			notifyError(error);
			return false;
		}
	}
	
	private void checkMessages() {
		if (dmnotf == 0 || dmintv > (new Date().getTime() - dmlast) / (60 * 1000) % 60)
			return;
		Log.v("FFService", "checkMessages()");
		try {
			Feed data = FFAPI.client_feed(session).get_feed_updates("filter/direct", 50, cursor, 0, 1);
			cursor = data.realtime.cursor;
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
			dmlast = new Date().getTime();
		} catch (Exception error) {
			notifyError(error);
		}
	}
	
	private void checkFeedCache() {
		if (!Commons.isOnWIFI(this) || (session.cachedFeed != null && session.cachedFeed.getAge() < 10*60*1000))
			return;
		Log.v("FFService", "checkFeedCache()");
		try {
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
		} catch (Exception error) {
			notifyError(error);
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
		if (key.equals(PK.SERV_PROR)) {
			prlast = 0;
			Log.v("FFService", "profile update requested");
		} else if (key.equals(PK.SERV_PROF)) {
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