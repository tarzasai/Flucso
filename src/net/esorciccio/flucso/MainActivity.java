package net.esorciccio.flucso;

import net.esorciccio.flucso.Commons.PK;
import net.esorciccio.flucso.FFAPI.Entry;
import net.esorciccio.flucso.FFAPI.FeedList;
import net.esorciccio.flucso.FFAPI.FeedList.SectionItem;
import net.esorciccio.flucso.FFAPI.SimpleResponse;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements OnFFReqsListener {

	private static final int REQ_SEARCH = 100;
	private static final int REQ_NEWPOST = 110;
	
	private FFSession session;
	private DrawerAdapter adapter;
	private CharSequence mTitle;
	private CharSequence mDrawerTitle;
	private DrawerLayout mDrawerLayout;
	private View mDrawerView;
	private ListView mDrawerList;
	private ActionBarDrawerToggle mDrawerToggle;
	private LinearLayout mUserBox;
	private ImageView mUserIcon;
	private TextView mUserName;
	private TextView mUserLogin;
	private MenuItem miDrwUpd;
	private MenuItem miDrwCfg;
	private MenuItem miDrwAbo;
	private MenuItem miSearch;
	private MenuItem miConfig;
	private MenuItem miAboutD;
	
	private BroadcastReceiver srvMsgsReceiver;
	private IntentFilter srvMsgsFilters;
	
	private String lastFeed;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setContentView(R.layout.activity_main);
		
		Log.v("stack", this.getClass().getName() + ".onCreate");
		
		session = FFSession.getInstance(this);
		adapter = new DrawerAdapter(this);
		
		lastFeed = session.getPrefs().getString(PK.STARTUP, "home");
		
		mTitle = mDrawerTitle = getTitle();
		
		mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
		mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
		
		mDrawerView = findViewById(R.id.drawer_view);
		
		mDrawerList = (ListView) findViewById(R.id.lv_feed);
		mDrawerList.setAdapter(adapter);
		mDrawerList.setOnItemClickListener(new DrawerItemClickListener());
		
		mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.drawable.ic_drawer, R.string.drawer_open,
			R.string.drawer_close) {
			@Override
			public void onDrawerClosed(View view) {
				getActionBar().setTitle(mTitle);
				invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
			}
			@Override
			public void onDrawerOpened(View drawerView) {
				getActionBar().setTitle(mDrawerTitle);
				invalidateOptionsMenu(); // creates call to onPrepareOptionsMenu()
			}
		};
		
		mDrawerLayout.setDrawerListener(mDrawerToggle);
		
		mUserBox = (LinearLayout) findViewById(R.id.user_box);
		mUserIcon = (ImageView) findViewById(R.id.user_icon);
		mUserName = (TextView) findViewById(R.id.user_name);
		mUserLogin = (TextView) findViewById(R.id.user_login);
		
		getActionBar().setDisplayHomeAsUpEnabled(true);
		getActionBar().setHomeButtonEnabled(true);
		
		// Commons.picasso(getApplicationContext()).setIndicatorsEnabled(true);
		
		srvMsgsReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				String msg = intent.getStringExtra("message");
				switch (intent.getAction()) {
					case FFService.SERVICE_ERROR:
						Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
						break;
					case FFService.PROFILE_READY:
						profileReady();
						loadNavigation();
						break;
					/*case FFService.DM_BASE_NOTIF:
						String fid = "filter/direct";
						SectionItem si = session.navigation != null ? session.navigation.getSectionByFeed(fid) : null;
						openFeed(si != null ? si.name : "Direct Messages", fid, null);
						break;*/
					default:
						Toast.makeText(MainActivity.this, "Unknown intent from service: " + intent.getAction(),
							Toast.LENGTH_SHORT).show();
						break;
				}
			}
		};
		srvMsgsFilters = new IntentFilter();
		srvMsgsFilters.addAction(FFService.SERVICE_ERROR);
		srvMsgsFilters.addAction(FFService.PROFILE_READY);
		
		startService(new Intent(this, FFService.class));
		
		Intent intent = getIntent();
		if (!intent.getAction().equals(Intent.ACTION_MAIN))
			onNewIntent(intent);
	}
	
	@Override
	protected void onDestroy() {
		
		Log.v("stack", this.getClass().getName() + ".onDestroy");
		
		//LocalBroadcastManager.getInstance(this).unregisterReceiver(serviceMessages);
		
		super.onDestroy();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		
		//Log.v("stack", this.getClass().getName() + ".onResume");

		LocalBroadcastManager.getInstance(this).registerReceiver(srvMsgsReceiver, srvMsgsFilters);
		
		if (!session.hasAccount()) {
			mUserIcon.setImageResource(R.drawable.nomugshot);
			mUserName.setText(R.string.noaccount_name);
			mUserLogin.setText(R.string.noaccount_login);
			mUserBox.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					v.getContext().startActivity(new Intent(MainActivity.this, SettingsActivity.class));
				}
			});
		} else if (!session.hasProfile()) {
			setProgressBarIndeterminateVisibility(true);
			mUserLogin.setText(session.getUsername());
			mUserName.setText(R.string.waiting_profile);
			mUserBox.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					SectionItem me = new SectionItem();
					me.id = "me";
					selectDrawerItem(me);
					mDrawerLayout.closeDrawer(mDrawerView);
				}
			});
		} else if (session.navigation == null) {
			profileReady();
			loadNavigation();
		} else {
			profileReady();
			navigationReady();
		}
		adapter.notifyDataSetChanged();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		
		//Log.v("stack", this.getClass().getName() + ".onPause");
		
		LocalBroadcastManager.getInstance(this).unregisterReceiver(srvMsgsReceiver);
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		
		lastFeed = savedInstanceState.getString("lastFeed", session.getPrefs().getString(PK.STARTUP, "home"));
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		outState.putString("lastFeed", lastFeed);
	}
	
	@Override
	public void setTitle(CharSequence title) {
		mTitle = title;
		getActionBar().setTitle(mTitle);
	}
	
	@Override
	public void onBackPressed() {
		if (mDrawerLayout.isDrawerVisible(mDrawerView))
			mDrawerLayout.closeDrawer(mDrawerView);
		else
			super.onBackPressed();
	}
	
	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
		mDrawerToggle.syncState();
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		// Pass any configuration change to the drawer toggles
		mDrawerToggle.onConfigurationChanged(newConfig);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);

		miDrwUpd = menu.findItem(R.id.drawer_refresh);
		miDrwCfg = menu.findItem(R.id.drawer_settings);
		miDrwAbo = menu.findItem(R.id.drawer_about);
		miSearch = menu.findItem(R.id.action_search);
		miConfig = menu.findItem(R.id.action_settings);
		miAboutD = menu.findItem(R.id.action_about);
		
		miDrwUpd.setVisible(false);
		miDrwCfg.setVisible(false);
		
		return true;
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		miDrwUpd.setVisible(mDrawerLayout.isDrawerVisible(mDrawerView) && session.hasProfile());
		miDrwCfg.setVisible(mDrawerLayout.isDrawerVisible(mDrawerView));
		miDrwAbo.setVisible(mDrawerLayout.isDrawerVisible(mDrawerView));
		miSearch.setVisible(session.hasProfile());
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// The action bar home/up action should open or close the drawer.
		// ActionBarDrawerToggle will take care of this.
		if (mDrawerToggle.onOptionsItemSelected(item))
			return true;
		if (item.equals(miDrwUpd)) {
			session.updateProfile();
			loadNavigation();
			return true;
		}
		if (item.equals(miDrwCfg) || item.equals(miConfig)) {
			startActivity(new Intent(this, SettingsActivity.class));
			return true;
		}
		if (item.equals(miDrwAbo) || item.equals(miAboutD)) {
			AboutBox.Show(MainActivity.this);
			return true;
		}
		if (item.equals(miSearch)) {
			startActivityForResult(new Intent(this, SearchActivity.class), REQ_SEARCH);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	@Override
	protected void onNewIntent(Intent intent) {
		if (intent.getAction().equals(FFService.DM_BASE_NOTIF)) {
			String fid = "filter/direct";
			SectionItem si = session.navigation != null ? session.navigation.getSectionByFeed(fid) : null;
			if (si == null) {
				si = new SectionItem();
				si.id = fid;
				si.type = "special";
				si.name = "Direct Messages";
			}
			if (getFragmentManager().findFragmentByTag(FeedFragment.FRAGMENT_TAG) == null)
				selectDrawerItem(si);
			else
				openFeed(si.name, fid, null);
			NotificationManager nmg = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
			nmg.cancel(FFService.NOTIFICATION_ID); // remove from notification bar
			return;
		}
		if (intent.getAction().equals(FFService.ADSC_BASE_NOTIF)) {
			String fid = "filter/discussions";
			SectionItem si = session.navigation != null ? session.navigation.getSectionByFeed(fid) : null;
			if (si == null) {
				si = new SectionItem();
				si.id = fid;
				si.type = "special";
				si.name = "My Discussions";
			}
			if (getFragmentManager().findFragmentByTag(FeedFragment.FRAGMENT_TAG) == null)
				selectDrawerItem(si);
			else
				openFeed(si.name, fid, null);
			return;
		}
		if (intent.getAction().equals(FFService.DSC_BASE_NOTIF)) {
			openEntry(intent.getExtras().getString("id"));
			return;
		}
		if (intent.getAction().equals(Intent.ACTION_VIEW)) {
			Uri data = intent.getData();
			if (data.getHost().equals("ff.im")) {
				reverseShort(data.getPath().substring(1));
				return;
			}
			if (data.getHost().equals("friendfeed.com") && data.getPath().startsWith("/search")) {
				openFeed("search", "search", data.getQuery().substring(2));
				return;
			}
			Toast.makeText(this, "Unhandled intent (data: \"" + data.toString() + "\")", Toast.LENGTH_LONG).show();
			return;
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQ_SEARCH && resultCode == RESULT_OK) {
			if (data.hasExtra("query"))
				openFeed("search", "search", data.getStringExtra("query"));
			else if (data.hasExtra("feed"))
				openFeed(data.getStringExtra("name"), data.getStringExtra("feed"), null);
		} else if (requestCode == REQ_NEWPOST && resultCode == RESULT_OK)
			openEntry(data.getStringExtra("eid"));
	}
	
	@Override
	public void openInfo(String feed_id) {
		Toast.makeText(this, "openInfo: " + feed_id, Toast.LENGTH_LONG).show();
	}
	
	@Override
	public void openFeed(String name, String feed_id, String query) {
		lastFeed = feed_id;
		FragmentManager fm = getFragmentManager();
		fm.beginTransaction().replace(R.id.content_frame, FeedFragment.newInstance(name, feed_id, query),
			FeedFragment.FRAGMENT_TAG).addToBackStack(null).commit();
	}
	
	@Override
	public void openEntry(String entry_id) {
		getFragmentManager().beginTransaction().replace(R.id.content_frame, EntryFragment.newInstance(entry_id),
			EntryFragment.FRAGMENT_TAG).addToBackStack(null).commit();
	}
	
	@Override
	public void openGallery(String entry_id, int position) {
		getFragmentManager().beginTransaction().replace(R.id.content_frame,
			GalleryFragment.newInstance(entry_id, position), GalleryFragment.FRAGMENT_TAG).addToBackStack(null).commit();
	}

	@Override
	public void openPostNew(String[] dsts, String body, String link, String[] tmbs) {
		Intent intent = new Intent(this, PostActivity.class);
		Bundle prms = new Bundle();
		prms.putStringArray("dsts", dsts);
		prms.putString("body", body);
		prms.putString("link", link);
		prms.putStringArray("tmbs", tmbs);
		intent.putExtras(prms);
		startActivityForResult(intent, REQ_NEWPOST);
	}

	@Override
	public void openPostEdit(String entry_id, String body) {
		Intent intent = new Intent(this, PostActivity.class);
		Bundle prms = new Bundle();
		prms.putString("eid", entry_id);
		prms.putString("body", body);
		intent.putExtras(prms);
		startActivity(intent);
	}
	
	private void profileReady() {
		Commons.picasso(getApplicationContext()).load(session.profile.getAvatarUrl()).placeholder(
			R.drawable.nomugshot).into(mUserIcon);
		mUserLogin.setText(session.getUsername());
		mUserName.setText(session.profile.name);
		mUserBox.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				SectionItem me = new SectionItem();
				me.id = "me";
				me.name = session.profile.name;
				selectDrawerItem(me);
				mDrawerLayout.closeDrawer(mDrawerView);
			}
		});
		setProgressBarIndeterminateVisibility(false);
	}
	
	private void navigationReady() {
		if (getFragmentManager().findFragmentByTag(FeedFragment.FRAGMENT_TAG) == null) {
			// Open the startup feed.
			SectionItem si = session.navigation.getSectionByFeed(lastFeed);
			selectDrawerItem(si != null ? si : session.navigation.sections[0].feeds[0]);
		}
	}
	
	private void loadNavigation() {
		setProgressBarIndeterminateVisibility(true);
		Callback<FeedList> callback = new Callback<FeedList>() {
			@Override
			public void success(FeedList result, Response response) {
				setProgressBarIndeterminateVisibility(false);
				session.navigation = result;
				adapter.notifyDataSetChanged();
				navigationReady();
			}
			@Override
			public void failure(RetrofitError error) {
				setProgressBarIndeterminateVisibility(false);
				new AlertDialog.Builder(MainActivity.this).setTitle(R.string.res_rfcall_failed).setMessage(
					Commons.retrofitErrorText(error)).setIcon(android.R.drawable.ic_dialog_alert).setPositiveButton(
					R.string.dlg_btn_retry, new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							loadNavigation();
						}
					}).setCancelable(true).create().show();
			}
		};
		FFAPI.client_profile(session).get_navigation(callback);
	}
	
	private void reverseShort(String shcode) {
		setProgressBarIndeterminateVisibility(true);
		Callback<Entry> callback = new Callback<Entry>() {
			@Override
			public void success(Entry result, Response response) {
				setProgressBarIndeterminateVisibility(false);
				session.cachedEntry = result;
				openEntry(result.id);
			}
			@Override
			public void failure(RetrofitError error) {
				setProgressBarIndeterminateVisibility(false);
				new AlertDialog.Builder(MainActivity.this).setTitle(R.string.res_rfcall_failed).setMessage(
					Commons.retrofitErrorText(error)).setIcon(android.R.drawable.ic_dialog_alert).show();
			}
		};
		FFAPI.client_entry(session).rev_short(shcode, callback);
	}
	
	private void selectDrawerItem(SectionItem selection) {
		FragmentManager fm = getFragmentManager();
		// Always clear the fragments stack when we open a feed from the menu.
		if (fm.getBackStackEntryCount() > 0)
			fm.popBackStack(fm.getBackStackEntryAt(0).getId(), FragmentManager.POP_BACK_STACK_INCLUSIVE);
		// Check if the current fragment (if any) is on the same feed we want to open.
		Fragment chk = fm.findFragmentByTag(FeedFragment.FRAGMENT_TAG);
		if (chk != null && ((FeedFragment) chk).fid.equals(selection.id))
			return;
		// Open new feed fragment.
		lastFeed = selection.id;
		fm.beginTransaction().replace(R.id.content_frame, FeedFragment.newInstance(selection.name, lastFeed, selection.query),
			FeedFragment.FRAGMENT_TAG).commit(); // no backstack
	}
	
	public void subscribe(final String feed_id, final String list_id) {
		setProgressBarIndeterminateVisibility(true);
		Callback<SimpleResponse> callback = new Callback<SimpleResponse>() {
			@Override
			public void success(SimpleResponse result, Response response) {
				setProgressBarIndeterminateVisibility(false);
				Toast.makeText(MainActivity.this, result.status, Toast.LENGTH_LONG).show();
			}
			@Override
			public void failure(RetrofitError error) {
				setProgressBarIndeterminateVisibility(false);
				new AlertDialog.Builder(MainActivity.this).setTitle(R.string.res_rfcall_failed).setMessage(
					Commons.retrofitErrorText(error)).setIcon(android.R.drawable.ic_dialog_alert).setPositiveButton(
					R.string.dlg_btn_retry, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							subscribe(feed_id, list_id);
						}
					}).setCancelable(true).create().show();
			}
		};
		FFAPI.client_write(session).subscribe(feed_id, list_id, callback);
	}
	
	public void unsubscribe(final String feed_id, final String list_id) {
		setProgressBarIndeterminateVisibility(true);
		Callback<SimpleResponse> callback = new Callback<SimpleResponse>() {
			@Override
			public void success(SimpleResponse result, Response response) {
				setProgressBarIndeterminateVisibility(false);
				Toast.makeText(MainActivity.this,
					!TextUtils.isEmpty(result.status) ? result.status : result.success ? "ok" : "wtf?",
					Toast.LENGTH_LONG).show();
			}
			@Override
			public void failure(RetrofitError error) {
				setProgressBarIndeterminateVisibility(false);
				new AlertDialog.Builder(MainActivity.this).setTitle(R.string.res_rfcall_failed).setMessage(
					Commons.retrofitErrorText(error)).setIcon(android.R.drawable.ic_dialog_alert).setPositiveButton(
					R.string.dlg_btn_retry, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							unsubscribe(feed_id, list_id);
						}
					}).setCancelable(true).create().show();
			}
		};
		FFAPI.client_write(session).unsubscribe(feed_id, list_id, callback);
	}
	
	private class DrawerItemClickListener implements ListView.OnItemClickListener {
		@Override
		public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
			selectDrawerItem((SectionItem) adapter.getItem(position));
			mDrawerList.setItemChecked(position, true);
			mDrawerLayout.closeDrawer(mDrawerView);
		}
	}
}