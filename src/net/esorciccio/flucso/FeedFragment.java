package net.esorciccio.flucso;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import net.esorciccio.flucso.Commons.PK;
import net.esorciccio.flucso.FFAPI.Entry;
import net.esorciccio.flucso.FFAPI.Entry.Like;
import net.esorciccio.flucso.FFAPI.Feed;
import net.esorciccio.flucso.FFAPI.SimpleResponse;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso.LoadedFrom;
import com.squareup.picasso.Target;

public class FeedFragment extends BaseFragment implements SwipeRefreshLayout.OnRefreshListener, OnClickListener {
	public static final String FRAGMENT_TAG = "net.esorciccio.flucso.FeedFragment";
	
	private static final int AMOUNT_BASE = 30;
	private static final int AMOUNT_INCR = 20;
	
	private FeedAdapter adapter;
	private String cursor;
	private String eid;
	private int amount;
	private int lastext = -1;
	private boolean paused = false;
	private Timer timer;
	private LoaderTask loader;
	private UpdaterTask updater;
	private ExtenderTask extender;
	private DialogInterface.OnDismissListener onDismissDialog;
	
	private SwipeRefreshLayout srl;
	private ListView lvFeed;
	private LinearLayout llFooter;
	private ImageView imgGoUp;
	private TextView txtFooter;
	private MenuItem miWrite;
	private MenuItem miAutoU;
	private MenuItem miPause;
	private MenuItem miSubsc;

	public String fid;
	public String fname;
	public String fquery;
	
	public static FeedFragment newInstance(String name, String feed_id, String query) {
		FeedFragment fragment = new FeedFragment();
		Bundle args = new Bundle();
		args.putString("id", feed_id);
		args.putString("name", name);
		args.putString("query", query);
		fragment.setArguments(args);
		return fragment;
	}
	
	public FeedFragment() {
		// Required empty public constructor
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setHasOptionsMenu(true);
		
		adapter = new FeedAdapter(getActivity(), this);
		adapter.registerDataSetObserver(new DataSetObserver() {
			@Override
			public void onChanged() {
				if (fquery == "")
					session.cachedFeed = adapter.feed;
			}
		});
		
		Bundle args = getArguments();
		fid = args.getString("id");
		fname = args.getString("name");
		fquery = args.getString("query");
		cursor = args.getString("cursor", null);
		amount = args.getInt("amount", AMOUNT_BASE);
		paused = args.getBoolean("paused");
		
		onDismissDialog = new DialogInterface.OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				resumeUpdates(true, false);
			}
		};
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_feed, container, false);
		
		getActivity().setTitle(fname);
		
		srl = (SwipeRefreshLayout) view.findViewById(R.id.swipe_feed);
		lvFeed = (ListView) view.findViewById(R.id.lv_feed);
		imgGoUp = (ImageView) view.findViewById(R.id.img_feed_first);
		llFooter = (LinearLayout) inflater.inflate(R.layout.footer_feed, null);
		txtFooter = (TextView) llFooter.findViewById(R.id.txt_fetch_info);
		
		srl.setOnRefreshListener(this);
		srl.setColorSchemeResources(android.R.color.holo_blue_bright, android.R.color.holo_green_light,
			android.R.color.holo_orange_light, android.R.color.holo_red_light);
		
		lvFeed.addHeaderView(inflater.inflate(R.layout.header_feed, null));
		lvFeed.addFooterView(llFooter);
		lvFeed.setAdapter(adapter);
		lvFeed.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				if (view == llFooter)
					return;
				Entry entry = adapter.getItem(position - 1); // because of the header.
				if (entry.hidden) {
					doHideUnhide(entry, true);
				} else {
					session.cachedEntry = entry;
					mContainer.openEntry(entry.id);
				}
			}
		});
		lvFeed.setOnScrollListener(new OnScrollListener() {
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				switch (scrollState) {
					case OnScrollListener.SCROLL_STATE_IDLE:
						imgGoUp.setAlpha((float) 1.0);
						break;
					case OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
						imgGoUp.setAlpha((float) 0.5);
						break;
					case OnScrollListener.SCROLL_STATE_FLING:
						imgGoUp.setAlpha((float) 0.2);
						break;
				}
			}
			@Override
			public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
				if (adapter.feed == null)
					return;
				imgGoUp.setVisibility(firstVisibleItem > 0 ? View.VISIBLE : View.GONE);
				if (firstVisibleItem + visibleItemCount >= totalItemCount && lastext != 0) {
					if (extender == null) {
						txtFooter.setText(R.string.fetching_wait);
						extender = (ExtenderTask) new ExtenderTask().execute();
					}
				} else
					txtFooter.setText(R.string.fetching_none);
			}
		});
		
		imgGoUp.setVisibility(View.GONE);
		imgGoUp.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				lvFeed.smoothScrollToPosition(0);
			}
		});
		
		return view;
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		
		if (savedInstanceState == null)
			return;

		eid = savedInstanceState.getString("eid", null);
		cursor = savedInstanceState.getString("cursor", null);
		amount = savedInstanceState.getInt("amount", AMOUNT_BASE);
		paused = savedInstanceState.getBoolean("paused");
	}
	
	@Override
	public void onResume() {
		super.onResume();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		
		pauseUpdates(false);
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		outState.putString("eid", lvFeed != null && adapter.feed != null && adapter.feed.entries.size() > 0 ?
			adapter.getItem(lvFeed.getFirstVisiblePosition()).id : "");
		outState.putString("name", fname);
		outState.putString("cursor", cursor);
		outState.putInt("amount", amount);
		outState.putBoolean("paused", paused);
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.feed, menu);
		
		miWrite = menu.findItem(R.id.action_feed_post);
		miAutoU = menu.findItem(R.id.action_feed_auto);
		miPause = menu.findItem(R.id.action_feed_pause);
		miSubsc = menu.findItem(R.id.action_feed_subscr);
	}
	
	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		checkMenu();
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item == miWrite) {
			mContainer.openPostNew(new String[] { fid }, null, null, null);
			return true;
		}
		if (item == miAutoU) {
			resumeUpdates(true, false);
			return true;
		}
		if (item == miPause) {
			pauseUpdates(false);
			return true;
		}
		if (item == miSubsc) {
			pauseUpdates(false);
			final LayoutInflater inflater = getActivity().getLayoutInflater();
			Commons.picasso(getActivity()).load(adapter.feed.getAvatarUrl()).placeholder(R.drawable.nomugshot).into(
				new Target() {
				@Override
				public void onPrepareLoad(Drawable arg0) {
				}
				@Override
				public void onBitmapLoaded(Bitmap bitmap, LoadedFrom arg1) {
					View view = inflater.inflate(R.layout.dialog_subscr, null);
					ListView lvSubsc = (ListView) view.findViewById(R.id.lv_subs_lists);
					final AlertDialog dlg = new AlertDialog.Builder(getActivity()).setTitle(
						adapter.feed.getName()).setView(view).setOnDismissListener(onDismissDialog).setCancelable(
						true).setIcon(new BitmapDrawable(getActivity().getResources(), bitmap)).create();
					lvSubsc.setAdapter(new SubscrListAdapter(getActivity(), adapter.feed));
					dlg.show();
				}
				@Override
				public void onBitmapFailed(Drawable arg0) {
				}
			});
			return true;
		}
		Toast.makeText(getActivity(), item.getTitle(), Toast.LENGTH_SHORT).show();
		return false;
	}
	
	@Override
	public void onRefresh() {
		checkMenu();
		if ((updater != null && updater.scheduledExecutionTime() >= new Date().getTime()) ||
			(loader != null && loader.scheduledExecutionTime() >= new Date().getTime()))
			return;
		amount = AMOUNT_BASE;
		if (timer != null)
			timer.cancel();
		timer = new Timer(true);
		loader = new LoaderTask();
		timer.schedule(loader, 500);
		if (isAutoUpdGoing()) {
			updater = new UpdaterTask();
			timer.schedule(updater, 30000, 30000);
		}
	}
	
	@Override
	public void onClick(View v) {
		int pos;
		try {
			pos = (Integer) v.getTag();
		} catch (Exception err) {
			return; // wtf?
		}
		final Entry entry = adapter.getItem(pos);
		switch (v.getId()) {
			case R.id.img_entry_from:
				mContainer.openFeed(entry.from.name, entry.from.id, null);
				break;
			case R.id.img_feed_thumb:
			case R.id.txt_feed_files:
				session.cachedEntry = entry;
				mContainer.openGallery(entry.id, 0);
				break;
			case R.id.txt_feed_likes:
				if (entry.canLike())
					doLike(entry);
				else if (entry.canUnlike())
					doUnlike(entry);
				break;
			case R.id.txt_feed_fwd:
				final String[] dsts = new String[] { session.getUsername() };
				final String body = "FWD: " + entry.rawBody;
				final String link = entry.url;
				final String[] imgs = entry.getMediaUrls(false);
				mContainer.openPostNew(dsts, body, link, imgs);
				break;
			case R.id.txt_feed_hide:
				if (entry.canHide())
					doHideUnhide(entry, false);
				break;
		}
	}
	
	@Override
	protected void initFragment() {
		if (adapter != null && session.cachedFeed != null && session.cachedFeed.isIt(fid)) {
			adapter.feed = session.cachedFeed;
			adapter.notifyDataSetChanged();
			cursor = adapter.feed.realtime.cursor;
		}
		if (adapter != null && adapter.feed != null)
			adapter.feed.setLocalHide();
		resumeUpdates(false, false);
	}
	
	private void checkMenu() {
		if (miWrite == null || miAutoU == null || miPause == null || miSubsc == null)
			return;
		boolean fl = adapter != null && adapter.feed != null;
		miWrite.setVisible(fl);
		miWrite.setIcon(fl && adapter.feed.canDM() ? R.drawable.menu_dm : R.drawable.menu_post);
		miAutoU.setVisible(fl && !isAutoUpdGoing());
		miPause.setVisible(fl && !miAutoU.isVisible());
		miSubsc.setVisible(fl && adapter.feed.canSetSubscriptions());
	}
	
	private boolean isAutoUpdSet() {
		return session.getPrefs().getBoolean(PK.FEED_UPD, true);
	}
	
	private boolean isAutoUpdGoing() {
		return isAutoUpdSet() && !paused;
	}
	
	private void pauseUpdates(boolean showHourglass) {
		Log.v(logTag(), "pauseUpdates");
		paused = true;
		if (timer != null)
			timer.cancel();
		checkMenu();
		if (showHourglass)
			getActivity().setProgressBarIndeterminateVisibility(true);
	}
	
	private void resumeUpdates(boolean removeHourglass, boolean reload) {
		Log.v(logTag(), "resumeUpdates");
		paused = false;
		reload = reload || adapter.feed == null || TextUtils.isEmpty(cursor);
		if (timer != null)
			timer.cancel();
		timer = new Timer(true);
		loader = new LoaderTask();
		updater = new UpdaterTask();
		if (reload)
			timer.schedule(loader, 500);
		if (isAutoUpdGoing())
			timer.schedule(updater, reload ? 30000 : 500, 30000);
		checkMenu();
		if (removeHourglass)
			getActivity().setProgressBarIndeterminateVisibility(false);
	}
	
	private void doLike(final Entry entry) {
		pauseUpdates(true);
		Callback<Like> callback = new Callback<Like>() {
			@Override
			public void success(Like result, Response response) {
				Toast.makeText(getActivity(), getString(R.string.res_inslike_ok), Toast.LENGTH_SHORT).show();
				resumeUpdates(true, false);
			}
			@Override
			public void failure(RetrofitError error) {
				getActivity().setProgressBarIndeterminateVisibility(false);
				new AlertDialog.Builder(getActivity()).setTitle(R.string.res_rfcall_failed).setMessage(
					Commons.retrofitErrorText(error)).setOnDismissListener(onDismissDialog).setPositiveButton(
					R.string.dlg_btn_retry, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							doLike(entry);
						}
					}).setIcon(android.R.drawable.ic_dialog_alert).setCancelable(true).create().show();
			}
		};
		FFAPI.client_write(session).ins_like(entry.id, callback);
	}
	
	private void doUnlike(final Entry entry) {
		pauseUpdates(true);
		Callback<SimpleResponse> callback = new Callback<SimpleResponse>() {
			@Override
			public void success(SimpleResponse result, Response response) {
				Toast.makeText(getActivity(), getString(R.string.res_dellike_ok), Toast.LENGTH_SHORT).show();
				int i = entry.indexOfLike(session.getUsername());
				if (i >= 0) {
					entry.likes.remove(i);
					entry.commands.set(entry.commands.indexOf("unlike"), "like"); // sadly, we need to do this.
				}
				resumeUpdates(true, false);
			}
			@Override
			public void failure(RetrofitError error) {
				getActivity().setProgressBarIndeterminateVisibility(false);
				new AlertDialog.Builder(getActivity()).setTitle(R.string.res_rfcall_failed).setMessage(
					Commons.retrofitErrorText(error)).setOnDismissListener(onDismissDialog).setPositiveButton(
					R.string.dlg_btn_retry, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							doUnlike(entry);
						}
					}).setIcon(android.R.drawable.ic_dialog_alert).setCancelable(true).create().show();
			}
		};
		FFAPI.client_write(session).del_like(entry.id, callback);
	}
	
	private void doHideUnhide(final Entry entry, final boolean unhide) {
		pauseUpdates(true);
		Callback<Entry> callback = new Callback<Entry>() {
			@Override
			public void success(Entry result, Response response) {
				Toast.makeText(getActivity(), getString(unhide ? R.string.res_unhide_ok : R.string.res_hidden_ok),
					Toast.LENGTH_SHORT).show();
				entry.hidden = !unhide;
				resumeUpdates(true, false);
			}
			@Override
			public void failure(RetrofitError error) {
				getActivity().setProgressBarIndeterminateVisibility(false);
				new AlertDialog.Builder(getActivity()).setTitle(R.string.res_rfcall_failed).setMessage(
					Commons.retrofitErrorText(error)).setOnDismissListener(onDismissDialog).setPositiveButton(
					R.string.dlg_btn_retry, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							doHideUnhide(entry, unhide);
						}
					}).setIcon(android.R.drawable.ic_dialog_alert).setCancelable(true).create().show();
			}
		};
		if (unhide)
			FFAPI.client_write(session).set_unhide(entry.id, 1, callback);
		else
			FFAPI.client_write(session).set_hidden(entry.id, callback);
	}
	
	public void subscribe() {
		Callback<SimpleResponse> callback = new Callback<SimpleResponse>() {
			@Override
			public void success(SimpleResponse result, Response response) {
				if (result.status.equals("subscribed")) {
					Toast.makeText(getActivity(), result.status, Toast.LENGTH_LONG).show();
					resumeUpdates(true, true);
				} else if (result.status.equals("requested")) {
					Toast.makeText(getActivity(), result.status, Toast.LENGTH_LONG).show();
					getActivity().setProgressBarIndeterminateVisibility(false);
					getActivity().getFragmentManager().popBackStack();
				}
			}
			@Override
			public void failure(RetrofitError error) {
				getActivity().setProgressBarIndeterminateVisibility(false);
				new AlertDialog.Builder(getActivity()).setTitle(R.string.res_rfcall_failed).setMessage(
					Commons.retrofitErrorText(error)).setOnCancelListener(new DialogInterface.OnCancelListener() {
						@Override
						public void onCancel(DialogInterface dialog) {
							getActivity().getFragmentManager().popBackStack();
						}
					}).setPositiveButton(R.string.dlg_btn_retry, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							subscribe();
						}
					}).setIcon(android.R.drawable.ic_dialog_alert).setCancelable(true).create().show();
			}
		};
		FFAPI.client_write(session).subscribe(fid, "home", callback);
	}
	
	private class LoaderTask extends TimerTask {
		@Override
		public void run() {
			final Activity context = getActivity();
			try {
				srl.setRefreshing(true);
				Log.v(logTag(), "(loader) fetching " + Integer.toString(amount) + " items...");
				// According to API documentation, when cursor="" we should get all the entries, but we don't. FF
				// returns an empty feed (with the cursor we'll use for the next call), so we have to make a call
				// for a complete feed anyway.
				final Feed updates;
				if (TextUtils.isEmpty(fquery)) {
					adapter.feed = FFAPI.client_feed(session).get_feed_normal(fid, 0, amount);
					updates = FFAPI.client_feed(session).get_feed_updates(fid, amount, "", 0, 1);
				} else {
					adapter.feed = FFAPI.client_feed(session).get_search_normal(fquery, 0, amount);
					updates = FFAPI.client_feed(session).get_search_updates(fquery, amount, "", 0, 1);
				}
				context.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (!FeedFragment.this.isVisible() || FeedFragment.this.isRemoving())
							return;
						srl.setRefreshing(false);
						adapter.feed.setLocalHide();
						adapter.feed.update(updates);
						fname = adapter.feed.name;
						cursor = updates.realtime.cursor;
						amount = adapter.feed.entries.size();
						adapter.notifyDataSetChanged();
						int i = TextUtils.isEmpty(eid) ? 0 : adapter.feed.indexOf(eid);
						lvFeed.smoothScrollToPosition(i > 0 ? i : 0);
						context.setTitle(fname);
						checkMenu();
						lastext = -1; // reset the extender task.
						eid = null; // reset the saved pos.
					}
				});
			} catch (final Exception error) {
				context.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						srl.setRefreshing(false);
						if (error instanceof RetrofitError && Commons.retrofitErrorCode((RetrofitError) error) == 403) {
							new AlertDialog.Builder(context).setTitle(fname).setMessage(R.string.ask_subscr_private
								).setOnCancelListener(new DialogInterface.OnCancelListener() {
									@Override
									public void onCancel(DialogInterface dialog) {
										context.getFragmentManager().popBackStack();
									}
								}).setPositiveButton(R.string.dlg_btn_ok, new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										subscribe();
									}
								}).setIcon(R.drawable.ic_action_add_group).setCancelable(true).create().show();
							return;
						}
						if (!FeedFragment.this.isVisible() || FeedFragment.this.isRemoving())
							return;
						if (adapter == null || adapter.feed == null)
							context.getFragmentManager().popBackStack();
						new AlertDialog.Builder(context).setTitle("LoaderTask").setMessage(
							error instanceof RetrofitError ? Commons.retrofitErrorText((RetrofitError) error)
								: error.getMessage()).setIcon(android.R.drawable.ic_dialog_alert).show();
					}
				});
			}
		}
	}
	
	private class UpdaterTask extends TimerTask {
		@Override
		public void run() {
			final Activity context = getActivity();
			if (TextUtils.isEmpty(cursor)) {
				Log.v(logTag(), "invalid cursor, rescheduling...");
				context.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (loader == null || loader.scheduledExecutionTime() <= 0)
							timer.schedule(loader, 2000);
					}
				});
				return;
			}
			try {
				Log.v(logTag(), "(updater) fetching " + Integer.toString(amount) + " items...");
				final Feed updates;
				if (TextUtils.isEmpty(fquery))
					updates = FFAPI.client_feed(session).get_feed_updates(fid, amount, cursor, 0, 1);
				else
					updates = FFAPI.client_feed(session).get_search_updates(fquery, amount, cursor, 0, 1);
				context.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (!FeedFragment.this.isVisible() || FeedFragment.this.isRemoving())
							return;
						srl.setRefreshing(false);
						int added = adapter.feed.update(updates);
						fname = adapter.feed.name;
						cursor = updates.realtime.cursor;
						amount = adapter.feed.entries.size();
						int offset = lvFeed.getFirstVisiblePosition();
						adapter.notifyDataSetChanged();
						if (added > 0) {
							lvFeed.smoothScrollToPosition(added + offset);
							if (imgGoUp.getVisibility() == View.VISIBLE)
								imgGoUp.startAnimation(blink);
							lastext = -1; // reset the extender task.
						}
						context.setTitle(fname);
						checkMenu();
					}
				});
			} catch (Exception error) {
				final String text = error instanceof RetrofitError ? Commons.retrofitErrorText((RetrofitError) error)
					: error.getMessage();
				context.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (!FeedFragment.this.isVisible() || FeedFragment.this.isRemoving())
							return;
						srl.setRefreshing(false);
						new AlertDialog.Builder(context).setTitle("UpdaterTask").setMessage(text).setIcon(
							android.R.drawable.ic_dialog_alert).show();
					}
				});
			}
		}
	}
	
	private class ExtenderTask extends AsyncTask<Void, Void, Feed> {
		private String error = null;
        @Override
        protected void onPreExecute() {
        	srl.setRefreshing(true);
        	Log.v(logTag(), "(extender) fetching more items starting from " + Integer.toString(amount + 1) + "...");
        }
		@Override
		protected Feed doInBackground(Void... params) {
			try {
				return TextUtils.isEmpty(fquery) ?
					FFAPI.client_feed(session).get_feed_normal(fid, amount + 1, AMOUNT_INCR) :
					FFAPI.client_feed(session).get_search_normal(fquery, amount + 1, AMOUNT_INCR);
			} catch (Exception e) {
				error = e instanceof RetrofitError ? Commons.retrofitErrorText((RetrofitError) e) : e.getMessage();
				return null;
			}
		}
        @Override
        protected void onPostExecute(Feed result) {
			Context context = getActivity();
			if (context == null || !isAdded() || isDetached() || !isVisible() || isRemoving())
				return;
			try {
				srl.setRefreshing(false);
	        	if (!TextUtils.isEmpty(error))
	        		new AlertDialog.Builder(context).setTitle("ExtenderTask").setMessage(error).setIcon(
						android.R.drawable.ic_dialog_alert).show();
	        	else {
	        		Log.v(logTag(), "got " + Integer.toString(result.entries.size()));
	        		lastext = adapter.feed.append(result);
					amount = adapter.feed.entries.size();
					Log.v(logTag(), "appended " + Integer.toString(lastext));
					int y = lvFeed.getScrollY();
					adapter.notifyDataSetChanged();
					lvFeed.scrollTo(0, y);
					checkMenu();
	        	}
			} finally {
				extender = null;
			}
        }
	}
}