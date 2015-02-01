package net.ggelardi.flucso.serv;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import retrofit.RetrofitError;
import retrofit.client.Response;
import retrofit.mime.TypedByteArray;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;

import com.squareup.picasso.Picasso;

public class Commons {
	// we better don't tell FF and other sites we are a phone, if we want to receive all images as supposed:
	public static final String USER_AGENT = "Mozilla/5.0 (Windows NT 5.1; rv:16.0) Gecko/20100101 Firefox/16.0";
	
	public static class PK {
		public static final String USERNAME = "Username";
		public static final String REMOTEKEY = "RemoteKey";
		public static final String STARTUP = "pk_startup";
		public static final String LOCALE = "pk_locale";
		public static final String PROXY_USED = "pk_proxy_active";
		public static final String PROXY_HOST = "pk_proxy_host";
		public static final String PROXY_PORT = "pk_proxy_port";
		public static final String PROF_INFO = "pk_prof_info";
		public static final String PROF_LIST = "pk_prof_list";
		public static final String FEED_UPD = "pk_feed_upd";
		public static final String FEED_FOF = "pk_feed_fof";
		public static final String FEED_HID = "pk_feed_hid";
		public static final String FEED_ELC = "pk_feed_elc";
		public static final String FEED_HBK = "pk_feed_hbk";
		public static final String FEED_HBF = "pk_feed_hbf";
		public static final String FEED_SPO = "pk_feed_spo";
		public static final String ENTR_IMCO = "pk_entry_imco";
		public static final String SERV_PROF = "pk_serv_prof";
		public static final String SERV_NOTF = "pk_serv_notf";
		public static final String SERV_MSGS = "pk_serv_msgs";
		public static final String SERV_MSGS_TIME = "pk_serv_msgs_time";
		public static final String SERV_MSGS_CURS = "pk_serv_msgs_cursor";
	}
	
	public static ArrayList<String> bFeeds = new ArrayList<String>();
	public static ArrayList<String> bWords = new ArrayList<String>();
	public static boolean bSpoilers = false;
	
	public static boolean isConnected(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo ni = cm.getActiveNetworkInfo();
		return ni != null && ni.isConnectedOrConnecting();
	}
	
	public static boolean isOnWIFI(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo ni = cm.getActiveNetworkInfo();
		return ni != null && ni.isConnectedOrConnecting() && ni.getType() == ConnectivityManager.TYPE_WIFI;
	}
	
	public static Picasso picasso(Context ctx) {
		/*
		 * FOR DEBUG ONLY!
		 * 
	    Picasso.Builder builder = new Picasso.Builder(ctx);
	    builder.downloader(new UrlConnectionDownloader(ctx) {
	        @Override
	        protected HttpURLConnection openConnection(Uri uri) throws IOException {
	            HttpURLConnection connection = super.openConnection(uri);
	            connection.setRequestProperty("User-Agent", USER_AGENT);
	            return connection;
	        }
	    });
	    builder.listener(new Picasso.Listener() {
			@Override
			public void onImageLoadFailed(Picasso picasso, Uri uri, Exception error) {
				Log.v("picasso", error.getLocalizedMessage() + " -- " + uri.toString());
			}});
	    return builder.build();
	    */
	    return Picasso.with(ctx);
	}
	
	public static long convertTime(long timestamp, String fromTimeZone, String toTimeZone) {
		Calendar fromCal = new GregorianCalendar(TimeZone.getTimeZone(fromTimeZone));
		fromCal.setTimeInMillis(timestamp);
		Calendar toCal = new GregorianCalendar(TimeZone.getTimeZone(toTimeZone));
		toCal.setTimeInMillis(fromCal.getTimeInMillis());
		return toCal.getTimeInMillis();
	}

	public static int retrofitErrorCode(RetrofitError error) {
		Response r = error.getResponse();
		if (r != null)
			return r.getStatus();
		return -1;
	}

	public static String retrofitErrorText(RetrofitError error) {
		String msg;
		Response r = error.getResponse();
		if (r != null) {
			
			if (r.getBody() instanceof TypedByteArray) {
				TypedByteArray b = (TypedByteArray) r.getBody();
				msg = new String(b.getBytes());
			} else
				msg = r.getReason();
		} else
			msg = error.getLocalizedMessage();
		if (msg == null && error.getCause() != null) {
			msg = error.getCause().getLocalizedMessage();
			if (msg == null)
				msg = error.getCause().getClass().getName();
		}
		Log.v("RPC", msg);
		Log.v("RPC", error.getUrl());
		return msg;
	}
	
	public static String firstUrl(String text) {
		if (!TextUtils.isEmpty(text))
			for (String s : text.split("\\s+"))
				if (Patterns.WEB_URL.matcher(s).matches())
					return s;
		return null;
	}
	
	public static String firstImage(String text) {
		if (!TextUtils.isEmpty(text)) {
			String chk;
			String[] words = text.split("\\s+");
			for (String s : words)
				if (Patterns.WEB_URL.matcher(s).matches()) {
					if (YouTube.isVideoUrl(s))
						return YouTube.getPreview(s);
					chk = s.toLowerCase(Locale.getDefault());
					if (chk.indexOf("/m.friendfeed-media.com/") > 0 || (chk.endsWith(".jpg") || chk.endsWith(".jpeg") ||
						chk.endsWith(".png") || chk.endsWith(".gif")))
						return s;
				}
		}
		return null;
	}
	
	public static class YouTube {
		
		public static boolean isVideoUrl(String url) {
			return url.contains("www.youtube.com") || url.contains("://youtu.be/");
		}
		
		public static String getId(String url) {
			if (url.startsWith("http://youtu.be/"))
				return url.substring(16);
			if (url.startsWith("https://youtu.be/"))
				return url.substring(17);
			if (url.contains("www.youtube.com/watch"))
				return Uri.parse(url).getQueryParameter("v");
			if (url.contains("www.youtube.com/v/")) {
				String id = Uri.parse(url).getLastPathSegment();
				return id.contains("&") ? id.substring(0, id.indexOf("&")) : id;
			}
			return null;
		}
		
		public static String getPreview(String url) {
			String id = getId(url);
			if (id != null)
				return "http://img.youtube.com/vi/" + id + "/0.jpg";
			return null;
		}
		
		public static String getFriendlyUrl(String url) {
			return !isVideoUrl(url) ? url : "http://www.youtube.com/watch?v=" + getId(url);
		}
	}
}