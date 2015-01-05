package net.ggelardi.flucso;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import net.ggelardi.flucso.Commons.PK;
import net.ggelardi.flucso.FFAPI.Entry;
import net.ggelardi.flucso.FFAPI.Feed;
import net.ggelardi.flucso.FFAPI.FeedInfo;
import net.ggelardi.flucso.FFAPI.FeedList;
import net.ggelardi.flucso.FFAPI.IdentItem;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;

public final class FFSession implements OnSharedPreferenceChangeListener {
	
	private static FFSession singleton;
	
	public static FFSession getInstance(Context context) {
		if (singleton == null) {
			singleton = new FFSession(context);
		}
		return singleton;
	}
	
	private final SharedPreferences prefs;
	private FeedInfo profile;
	private FeedList navigation;
	
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
		
		if (hasProfile()) // just a trick to reload profile and navigation data.
			Log.v("FFSession", "Cached profile loaded");
		
		loadFilters();
	}
	
	private void loadFilters() {
		String chk = prefs.getString(PK.FEED_HBK, "").toLowerCase(Locale.getDefault()).trim();
		if (TextUtils.isEmpty(chk))
			Commons.bWords.clear();
		else
			Commons.bWords = new ArrayList<String>(Arrays.asList(chk.replaceAll("^[,\\s]+", "").split("(?:,\\s*)+")));
		chk = prefs.getString(PK.FEED_HBF, "").toLowerCase(Locale.getDefault()).trim();
		if (TextUtils.isEmpty(chk))
			Commons.bFeeds.clear();
		else
			Commons.bFeeds = new ArrayList<String>(Arrays.asList(chk.replaceAll("^[,\\s]+", "").split("(?:,\\s*)+")));
		Commons.bSpoilers = prefs.getBoolean(PK.FEED_SPO, false);
	}
	
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
	
	public void saveAccount(String username, String password) {
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(PK.USERNAME, username.toLowerCase(Locale.getDefault()));
		editor.putString(PK.REMOTEKEY, password);
		editor.commit();
	}
	
	public boolean hasAccount() {
		return !(TextUtils.isEmpty(getUsername()) || TextUtils.isEmpty(getRemoteKey()));
	}
	
	public boolean hasProfile() {
		return getProfile() != null && getNavigation() != null;
	}
	
	public void setProfile(FeedInfo value) {
		profile = value;
		IdentItem.accountID = profile != null ? profile.id : null;
		SharedPreferences.Editor editor = prefs.edit();
		if (profile == null)
			editor.remove(PK.PROF_INFO);
		else try {
			editor.putString(PK.PROF_INFO, new Gson().toJson(value));
		} catch (Exception err) {
			Log.e("FFSession", "setProfile", err);
			editor.remove(PK.PROF_INFO);
		}
		editor.commit();
	}
	
	public FeedInfo getProfile() {
		if (profile == null && prefs.contains(PK.PROF_INFO))
			try {
				profile = new Gson().fromJson(prefs.getString(PK.PROF_INFO, null), FeedInfo.class);
			} catch (Exception err) {
				Log.e("FFSession", "getProfile", err);
			}
		return profile;
	}
	
	public void setNavigation(FeedList value) {
		navigation = value;
		SharedPreferences.Editor editor = prefs.edit();
		if (navigation == null)
			editor.remove(PK.PROF_LIST);
		else try {
			editor.putString(PK.PROF_LIST, new Gson().toJson(value));
		} catch (Exception err) {
			Log.e("FFSession", "setNavigation", err);
			editor.remove(PK.PROF_LIST);
		}
		editor.commit();
	}
	
	public FeedList getNavigation() {
		if (navigation == null && prefs.contains(PK.PROF_LIST))
			try {
				navigation = new Gson().fromJson(prefs.getString(PK.PROF_LIST, null), FeedList.class);
			} catch (Exception err) {
				Log.e("FFSession", "getNavigation", err);
			}
		return navigation;
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		if (key.equals(PK.USERNAME) || key.equals(PK.REMOTEKEY)) {
			profile = null;
			navigation = null;
			SharedPreferences.Editor editor = prefs.edit();
			editor.remove(PK.PROF_INFO);
			editor.remove(PK.PROF_LIST);
			editor.commit();
			FFAPI.dropClients();
		} else if (key.equals(PK.LOCALE)) {
			FFAPI.dropClients();
		} else if (key.equals(PK.FEED_HBK) || key.equals(PK.FEED_HBF) || key.equals(PK.FEED_SPO)) {
			loadFilters();
		}
	}
}