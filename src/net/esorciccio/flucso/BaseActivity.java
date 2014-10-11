package net.esorciccio.flucso;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Window;
import android.widget.Toast;

public abstract class BaseActivity extends Activity {
	
	private BroadcastReceiver receiver;
	private ProgressDialog progress;
	
	protected FFSession session;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		
		Log.v(logTag(), "onCreate");
		
		session = FFSession.getInstance(this);
		
		progress = new ProgressDialog(this);
		progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		progress.setIndeterminate(true);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		Log.v(logTag(), "onDestroy");
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		Log.v(logTag(), "onResume");
		
		receiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				switch (intent.getAction()) {
					case FFService.SERVICE_ERROR:
						Toast.makeText(BaseActivity.this, intent.getStringExtra("message"), Toast.LENGTH_LONG).show();
						break;
					case FFService.PROFILE_READY:
						hideWaitingBox();
						profileReady();
						break;
					default:
						Toast.makeText(BaseActivity.this, "Unknown service request: " + intent.getAction(),
							Toast.LENGTH_SHORT).show();
						break;
				}
			}
		};
		IntentFilter filters = new IntentFilter();
		filters.addAction(FFService.SERVICE_ERROR);
		filters.addAction(FFService.PROFILE_READY);
		LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filters);
		
		if (session.hasProfile())
			profileReady();
		else if (session.hasAccount())
			showWaitingBox(R.string.waiting_profile);
		
		startService(new Intent(this, FFService.class));
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		
		Log.v(logTag(), "onPause");
		
		LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		Log.v(logTag(), "onSaveInstanceState");
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		
		Log.v(logTag(), "onRestoreInstanceState");
	}
	
	protected String logTag() {
		return this.getClass().getSimpleName();
	}
	
	protected abstract void profileReady();
	
	protected void showWaitingBox(String msg) {
		progress.setTitle(msg);
		progress.show();
	}
	
	protected void showWaitingBox(int stringResId) {
		showWaitingBox(getString(stringResId));
	}
	
	protected void hideWaitingBox() {
		progress.dismiss();
	}
}