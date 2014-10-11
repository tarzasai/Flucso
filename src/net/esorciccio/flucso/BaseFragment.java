package net.esorciccio.flucso;

import android.app.Activity;
import android.app.Fragment;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;

public abstract class BaseFragment extends Fragment {
	
	protected FFSession session;
	protected BroadcastReceiver mReceiver;
	protected OnFFReqsListener mContainer;
	protected ProgressDialog mProgress;
	protected AlphaAnimation blink;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Log.v(logTag(), "onCreate");
		
		session = FFSession.getInstance(getActivity());
		
		mProgress = new ProgressDialog(getActivity());
		mProgress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		mProgress.setIndeterminate(true);

		blink = new AlphaAnimation(0.0f, 1.0f);
		blink.setDuration(400);
		blink.setStartOffset(20);
		blink.setRepeatMode(Animation.REVERSE);
		blink.setRepeatCount(2);
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		Log.v(logTag(), "onResume");

		if (session.hasProfile())
			initFragment();
		else {
			mProgress.setTitle(R.string.waiting_profile);
			mProgress.show();
			mReceiver = new BroadcastReceiver() {
				@Override
				public void onReceive(Context context, Intent intent) {
					LocalBroadcastManager.getInstance(context).unregisterReceiver(mReceiver);
					mProgress.dismiss();
					initFragment();
				}
			};
			IntentFilter filter = new IntentFilter();
			filter.addAction(FFService.PROFILE_READY);
			LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mReceiver, filter);
		}
	}
	
	@Override
	public void onPause() {
		super.onPause();
		
		Log.v(logTag(), "onPause");
		
		mProgress.dismiss();
		if (mReceiver != null)
			LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mReceiver);
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		Log.v(logTag(), "onSaveInstanceState");
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		Log.v(logTag(), "onActivityCreated");
	}
	
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		
		Log.v(logTag(), "onAttach");
		
		try {
			mContainer = (OnFFReqsListener) activity;
		} catch (ClassCastException e) {
			throw new ClassCastException(activity.toString() + " must implement OnFFReqsListener");
		}
	}
	
	@Override
	public void onDetach() {
		super.onDetach();
		
		Log.v(logTag(), "onDetach");
		
		mContainer = null;
	}
	
	protected String logTag() {
		return this.getClass().getSimpleName();
	}
	
	protected abstract void initFragment();
}