package net.esorciccio.flucso;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager.NameNotFoundException;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class AboutBox {
	
	static String VersionName(Context context) {
		try {
			return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
		} catch (NameNotFoundException e) {
			return "Unknown";
		}
	}
	
	public static void Show(Activity context) {
		StringBuilder sb = new StringBuilder();
		sb.append("Retrofit, http://square.github.io/retrofit").append("\n");
		sb.append("Picasso, http://square.github.io/picasso").append("\n");
		sb.append("Gson, https://code.google.com/p/google-gson").append("\n");
		sb.append("Jsoup, http://jsoup.org").append("\n");
		sb.append("Classifier4J, http://classifier4j.sourceforge.net").append("\n");
		sb.append("Batch icons, http://adamwhitcroft.com/batch");
		
		LayoutInflater inflater = context.getLayoutInflater();
		View about = inflater.inflate(R.layout.dialog_about, (ViewGroup) context.findViewById(R.id.aboutView));
		((TextView) about.findViewById(R.id.txt_credits)).setText(sb.toString());
		
		new AlertDialog.Builder(context).setView(about).
			setTitle(context.getString(R.string.app_name) + " " + VersionName(context)).
//			setIcon(R.drawable.ic_launcher).
			setCancelable(true).
			show();
	}
}