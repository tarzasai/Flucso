package net.esorciccio.flucso;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
	public static final String DSC_BASE_NOTIF = "net.esorciccio.flucso.FFService.DSC_BASE_NOTIF";
	public static final String ADSC_BASE_NOTIF = "net.esorciccio.flucso.FFService.ADSC_BASE_NOTIF";

	public static final int NOTIFICATION_ID = 1;
	
	private FFSession session;
	private LocalBroadcastManager notifier;
	
	private Boolean terminated = false;
	private String cursor_messages;
	private String cursor_discussions;
	private long printv;
	private long dmintv;
	private long cmintv;
	private long prlast;
	private long dmlast;
	private long cmlast;
	private int dmnotf;
	private int cmnotf;
	private List<DiscussionNotification> discussionNotifications = new ArrayList<DiscussionNotification>();
	
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
		
		cursor_messages = session.getPrefs().getString(PK.SERV_MSGS_CURS, "");
		cursor_discussions = session.getPrefs().getString(PK.SERV_DSCS_CURS, "");
		printv = session.getPrefs().getInt(PK.SERV_PROF, 0);
		dmnotf = session.getPrefs().getInt(PK.SERV_NOTF, 0);
		dmintv = session.getPrefs().getInt(PK.SERV_MSGS, 0);
		cmnotf = session.getPrefs().getInt(PK.SERV_NOTC, 0);
		cmintv = session.getPrefs().getInt(PK.SERV_COMM, 0);
		
		session.getPrefs().registerOnSharedPreferenceChangeListener(this);
		
		int waitTime = 5000;
		try {
			while (!terminated) {
				if (!session.hasAccount())
					waitTime = 2000;
				else {
					waitTime = 5000;
					checkProfile();
					checkMessages();
					checkComments();
				}
				try {
					Thread.sleep(waitTime);
				} catch (InterruptedException e) {
					terminated = true;
					notifyError(e);
				}
			}
		} finally {
			try {
				SharedPreferences.Editor editor = session.getPrefs().edit();
				editor.putString(PK.SERV_MSGS_CURS, cursor_messages);
				editor.putString(PK.SERV_DSCS_CURS, cursor_discussions);
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
	
	private void checkProfile() {
		if (session.hasProfile() && (printv == 0 || printv > (new Date().getTime() - prlast) / (60 * 1000) % 60))
			return;
		Log.v("FFService", "checkProfile()");
		try {
			session.profile = FFAPI.client_profile(session).get_profile_sync("me");
			session.initProfile();
			prlast = new Date().getTime();
			notifier.sendBroadcast(new Intent(PROFILE_READY));
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
	
	private void checkMessages() {
		if (dmnotf == 0 || dmintv > (new Date().getTime() - dmlast) / (60 * 1000) % 60)
			return;
		Log.v("FFService", "checkMessages()");
		try {
			Feed data = FFAPI.client_feed(session).get_feed_updates("filter/direct", 50, cursor_messages, 0, 1);
			cursor_messages = data.realtime.cursor;
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

	private void checkComments() {
		if (cmnotf == 0 || cmintv > (new Date().getTime() - cmlast) / (60 * 1000) % 60)
			return;
		Log.v("FFService", "checkComments()");
		try {
			PendingIntent rpi = null;
			Feed data = FFAPI.client_feed(session).get_feed_updates("filter/discussions", 50, cursor_discussions, 0, 1);
			cursor_discussions = data.realtime.cursor;
			for (Entry e : data.entries) {
				int likes = 0;
				int comments = 0;
				if(e.comments.size() > 0) {
					for(Comment c : e.comments) {
						if(c.from.isMe() == false) {
							comments++;
						}
					}
				}
				if(e.likes.size() > 0) {
					for(Entry.Like l : e.likes) {
						if(l.isMine() == false) {
							likes++;
						}
					}
				}

				if(likes > 0 || comments > 0) {
					String r = "";
					boolean found = false;
					DiscussionNotification n = new DiscussionNotification(e.hashCode(),e.id);

					if (cmnotf == 1) {
						if(comments > 1 || (comments == 0 && likes > 0)) {
							r = getResources().getString(R.string.notif_cm_new) + " " + e.body;
						} else {
							r = e.comments.get(0).body;
						}

						if(discussionNotifications.size() > 0) {
							for (DiscussionNotification nn : discussionNotifications) {
								if (nn.idthread.equals(n.idthread)) {
									n.idnotification = nn.idnotification;
									found = true;
									break;
								}
							}
						}
						if(!found) {
							discussionNotifications.add(n);
						}
						rpi = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class).setAction(DSC_BASE_NOTIF).putExtra("id",e.id),
								PendingIntent.FLAG_UPDATE_CURRENT);
					} else if(cmnotf == 2) {
						r = getResources().getString(R.string.notif_cms_new);

						if(discussionNotifications.size() > 0) {
							n.idnotification = discussionNotifications.get(0).idnotification;
						} else {
							discussionNotifications.add(n);
						}
						rpi = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class).setAction(ADSC_BASE_NOTIF),
								PendingIntent.FLAG_UPDATE_CURRENT);
					}

					NotificationCompat.Builder ncb = new NotificationCompat.Builder(this).setSmallIcon(
							R.drawable.ic_launcher).setContentTitle(getResources().getString(R.string.app_name)).setContentText(r)
							.setAutoCancel(true).setContentIntent(rpi);
					NotificationManager nmg = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);                    

					nmg.notify(n.idnotification, ncb.build());

					if(cmnotf == 2) {
						break;
					}
				}
			}
			cmlast = new Date().getTime();
		} catch (Exception error) {
			notifyError(error);
		}
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
		} else if (key.equals(PK.SERV_NOTC)) {
			cmnotf = session.getPrefs().getInt(PK.SERV_NOTC, 0);
			Log.v("FFService", "cmnotf: " + Integer.toString(cmnotf));
		} else if (key.equals(PK.SERV_COMM)) {
			cmintv = sharedPreferences.getInt(PK.SERV_COMM, 0);
			Log.v("FFService", "dmintv: " + Long.toString(cmintv));
		}
	}

	private class DiscussionNotification {
		public int idnotification;
		public String idthread;

		DiscussionNotification(int idAndroid, String idFF) {
			idnotification = idAndroid;
			idthread = idFF;
		}
	}
}