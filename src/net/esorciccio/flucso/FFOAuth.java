package net.esorciccio.flucso;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

public class FFOAuth {
	private static final String BASE_URL = "https://friendfeed.com/account/oauth";
	private static final String CONS_KEY = "c914bd31ea024b9bade1365cefa8b989";
	private static final String CONS_SEC = "d5d5e78a0ced4a1da49230fe09696353078d9f37b0a841a888e24c064e88212d";
	
	public static final Token consumer_token = new Token(CONS_KEY, CONS_SEC);
	
	static class Token {
		String key;
		String secret;
		
		public Token(String k, String s) {
			key = k;
			secret = s;
		}
	}
	
	static String enc(String value) {
		try {
			return URLEncoder.encode(value, "utf-8");
		} catch (Exception e) {
			Log.e("FFOAuth", "encode", e);
			return e.toString();
		}
	}
	
	private static String hmacSha1(String value, String key) throws UnsupportedEncodingException,
		NoSuchAlgorithmException, InvalidKeyException {
		Mac mac = Mac.getInstance("HmacSHA1");
		SecretKeySpec secret = new SecretKeySpec(key.getBytes(), mac.getAlgorithm());
		mac.init(secret);
		byte[] digest = mac.doFinal(value.getBytes());
		return enc(Base64.encodeToString(digest, Base64.NO_WRAP));
	}
	
	public static String get_signature(String method, String url, List<String> params, Token token) {
		try {
			String value = "GET&" + enc(url) + "&" + enc(TextUtils.join("&", params));
			String key = consumer_token.secret + "&" + (token != null ? token.secret : "");
			return hmacSha1(value, key);
			/*
			String res = hmacSha1(value, key);
			Log.v("FFOAuth", "get_signature: value=" + value);
			Log.v("FFOAuth", "get_signature: sign=" + res);
			return res;
			*/
		} catch (Exception e) {
			Log.e("FFOAuth", "get_signature", e);
			return e.toString();
		}
	}
	
	public static String get_access_token_url(String user, String pass) {
		String url = BASE_URL + "/ia_access_token";
		List<String> args = new ArrayList<String>();
		args.add("ff_password=" + pass);
		args.add("ff_username=" + user);
		args.add("oauth_consumer_key=" + enc(consumer_token.key));
		args.add("oauth_signature_method=HMAC-SHA1");
		args.add("oauth_timestamp=" + Long.toString((new Date().getTime()) / 1000));
		args.add("oauth_version=1.0");
		args.add("oauth_nonce=" + UUID.randomUUID().toString().replace("-", ""));
		Collections.sort(args, String.CASE_INSENSITIVE_ORDER); // required!
		args.add("oauth_signature=" + get_signature("GET", url, args, null)); // must be the last one!
		return url + "?" + TextUtils.join("&", args);
	}
	
	public static Token get_access_token(String user, String pass) throws Exception {
		String url = get_access_token_url(user, pass);
		Log.v("FFOAuth", url);
		HttpClient httpclient = new DefaultHttpClient();
		HttpResponse response = httpclient.execute(new HttpGet(url));
		StatusLine status = response.getStatusLine();
		if (status.getStatusCode() == HttpStatus.SC_OK) {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			response.getEntity().writeTo(out);
			out.close();
			String res = out.toString();
			Log.v("FFOAuth", res);
			JSONObject json = new JSONObject(res);
			return new Token(json.getString("oauth_token"), json.getString("oauth_token_secret"));
		} else {
			response.getEntity().getContent().close();
			throw new IOException(status.getReasonPhrase());
		}
	}
}