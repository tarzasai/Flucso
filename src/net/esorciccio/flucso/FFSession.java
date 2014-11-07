package net.esorciccio.flucso;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import net.esorciccio.flucso.Commons.PK;
import net.esorciccio.flucso.FFAPI.Entry;
import net.esorciccio.flucso.FFAPI.Feed;
import net.esorciccio.flucso.FFAPI.FeedInfo;
import net.esorciccio.flucso.FFAPI.FeedList;
import net.esorciccio.flucso.FFAPI.IdentItem;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.PreferenceManager;
import android.text.TextUtils;

public final class FFSession implements OnSharedPreferenceChangeListener {
	
	private static FFSession singleton;
	
	public static FFSession getInstance(Context context) {
		if (singleton == null) {
			singleton = new FFSession(context);
		}
		return singleton;
	}
	
	private final SharedPreferences prefs;
	
	public FFSession(Context context) {
		prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
		prefs.registerOnSharedPreferenceChangeListener(this);
		
		IdentItem.accountName = context.getResources().getString(R.string.yourname);
		IdentItem.accountFeed = context.getResources().getString(R.string.yourfeed);
		
		if (TextUtils.isEmpty(prefs.getString(PK.LOCALE, ""))) {
			SharedPreferences.Editor editor = prefs.edit();
			editor.putString(PK.LOCALE, Locale.getDefault().getLanguage());
			editor.commit();
		}
		
		loadLocalFilters();
	}
	
	private void loadLocalFilters() {
		String chk = prefs.getString(PK.FEED_HBK, "").toLowerCase(Locale.getDefault()).trim();
		if (TextUtils.isEmpty(chk))
			Entry.bWords.clear();
		else
			Entry.bWords = new ArrayList<String>(Arrays.asList(chk.replaceAll("^[,\\s]+", "").split("(?:,\\s*)+")));
		chk = prefs.getString(PK.FEED_HBF, "").toLowerCase(Locale.getDefault()).trim();
		if (TextUtils.isEmpty(chk))
			Entry.bFeeds.clear();
		else
			Entry.bFeeds = new ArrayList<String>(Arrays.asList(chk.replaceAll("^[,\\s]+", "").split("(?:,\\s*)+")));
	}
	
	public FeedInfo profile;
	public FeedList navigation;
	public Feed cachedFeed;
	public Entry cachedEntry;
	
	public SharedPreferences getPrefs() {
		return prefs;
	}
	
	public String getUsername() {
		return prefs.getString(PK.USERNAME, "");
	}
	
	public String getRemoteKey() {
		return prefs.getString(PK.REMOTEKEY, "");
	}
	
	public boolean hasAccount() {
		return !(TextUtils.isEmpty(getUsername()) || TextUtils.isEmpty(getRemoteKey()));
	}
	
	public boolean hasProfile() {
		return profile != null;
	}
	
	public void updateProfile() {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putLong(PK.SERV_PROR, new Date().getTime());
		editor.commit();
	}
	
	public void saveAccount(String username, String password) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(PK.USERNAME, username.toLowerCase(Locale.getDefault()));
		editor.putString(PK.REMOTEKEY, password);
		editor.commit();
	}
	
	public void initProfile() {
		if (hasProfile())
			IdentItem.accountID = profile.id;
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals(PK.USERNAME) || key.equals(PK.REMOTEKEY)) {
			profile = null;
			navigation = null;
			FFAPI.dropClients();
		} else if (key.equals(PK.LOCALE)) {
			FFAPI.dropClients();
		} else if (key.equals(PK.FEED_HBK) || key.equals(PK.FEED_HBF)) {
			loadLocalFilters();
		}
	}
}