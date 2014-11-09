package net.esorciccio.flucso;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import net.esorciccio.flucso.Commons.PK;
import net.esorciccio.flucso.FFAPI.BaseFeed;
import net.esorciccio.flucso.FFAPI.Entry;
import net.esorciccio.flucso.FFAPI.Entry.Comment;
import net.esorciccio.flucso.FFAPI.Entry.Like;
import net.esorciccio.flucso.FFAPI.Feed;
import net.esorciccio.flucso.FFAPI.SimpleResponse;
import net.sf.classifier4J.summariser.SimpleSummariser;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

public class EntryFragment extends BaseFragment implements OnClickListener {
	public static final String FRAGMENT_TAG = "net.esorciccio.flucso.EntryFragment";
	
	private Entry entry;
	private Timer timer;
	private UpdaterTask updater;
	private String from;
	private String body;
	private int currentTab;
	private EntryBaseAdapter[] adapters;
	
	private ImageView imgFromB;
	private TextView txtFromB;
	private TextView txtToB;
	private TextView txtTimeB;
	private TextView txtBodyB;
	private ImageView imgIsDM;
	private ImageView imgFromS;
	private TextView txtFromS;
	private TextView txtToS;
	private TextView txtBodyS;
	private TabHost tabh;
	private TabWidget tabw;
	private EditText edtNewCom;
	private LinearLayout llHeader;
	private LinearLayout llFloat;
	private ImageView imgGoDn;
	private ListView listView;
	
	private MenuItem miSpeedC;
	private MenuItem miLike;
	private MenuItem miUnlike;
	private MenuItem miSummC;
	private MenuItem miShare;
	private MenuItem miEdit;
	private MenuItem miDelete;
	private MenuItem miBrowse;
	
	private OnClickListener onClickFrom;
	private OnItemLongClickListener[] onLongClickItem;
	private PopupMenu.OnDismissListener onDismissPopup;
	private DialogInterface.OnDismissListener onDismissDialog;
	
	public String eid;
	
	public static EntryFragment newInstance(String entry_id) {
		EntryFragment fragment = new EntryFragment();
		Bundle args = new Bundle();
		args.putString("eid", entry_id);
		fragment.setArguments(args);
		return fragment;
	}
	
	public EntryFragment() {
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setHasOptionsMenu(true);
		
		Bundle args = getArguments();
		eid = args.getString("eid");
		from = args.getString("from", getString(R.string.title_loading));
		body = args.getString("body", getString(R.string.title_loading));
		currentTab = args.getInt("tab", -1);
		
		adapters = new EntryBaseAdapter[] { new EntryRecpsAdapter(getActivity(), this),
			new EntryFilesAdapter(getActivity(), this), new EntryLikesAdapter(getActivity(), this),
			new EntryCommsAdapter(getActivity(), this) };
		
		onClickFrom = new OnClickListener() {
			@Override
			public void onClick(View v) {
				mContainer.openFeed(entry.from.name, entry.from.id, null);
			}
		};
		
		onLongClickItem = new OnItemLongClickListener[] { new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				BaseFeed f = (BaseFeed) adapters[0].getItem(position - 1);
				mContainer.openFeed(f.name, f.id, null);
				return true;
			}
		}, new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				session.cachedEntry = entry;
				mContainer.openGallery(entry.id, position - 1);
				return true;
			}
		}, new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				Like l = (Like) adapters[2].getItem(position - 1);
				if (!l.placeholder) {
					BaseFeed f = l.from;
					mContainer.openFeed(f.name, f.id, null);
					return true;
				}
				return false;
			}
		}, new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				Comment c = (Comment) adapters[3].getItem(position - 1);
				if (!c.placeholder) {
					BaseFeed f = c.from;
					mContainer.openFeed(f.name, f.id, null);
					return true;
				}
				return false;
			}
		} };
		
		onDismissPopup = new PopupMenu.OnDismissListener() {
			@Override
			public void onDismiss(PopupMenu menu) {
				resumeUpdates(true);
			}
		};
		onDismissDialog = new DialogInterface.OnDismissListener() {
			@Override
			public void onDismiss(DialogInterface dialog) {
				resumeUpdates(true);
			}
		};
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		// Inflate the layout for this fragment
		View view = inflater.inflate(R.layout.fragment_entry, container, false);
		
		imgFromS = (ImageView) view.findViewById(R.id.img_entry_from_small);
		txtFromS = (TextView) view.findViewById(R.id.txt_entry_from_small);
		txtToS = (TextView) view.findViewById(R.id.txt_entry_to_small);
		txtBodyS = (TextView) view.findViewById(R.id.txt_entry_body_small);
		listView = (ListView) view.findViewById(R.id.lv_entry_items);
		llFloat = (LinearLayout) view.findViewById(R.id.l_entry_hdr_float);
		imgGoDn = (ImageView) view.findViewById(R.id.img_entry_last);
		edtNewCom = (EditText) view.findViewById(R.id.edt_entry_comm);
		
		llHeader = (LinearLayout) inflater.inflate(R.layout.header_entry, null);
		listView.addHeaderView(llHeader);
		
		imgFromB = (ImageView) llHeader.findViewById(R.id.img_entry_from);
		txtFromB = (TextView) llHeader.findViewById(R.id.txt_entry_from);
		txtToB = (TextView) llHeader.findViewById(R.id.txt_entry_to);
		txtTimeB = (TextView) llHeader.findViewById(R.id.txt_entry_time);
		imgIsDM = (ImageView) llHeader.findViewById(R.id.img_entry_dm);
		txtBodyB = (TextView) llHeader.findViewById(R.id.txt_entry_body_big);
		tabh = (TabHost) llHeader.findViewById(android.R.id.tabhost);
		tabw = (TabWidget) llHeader.findViewById(android.R.id.tabs);
		
		edtNewCom.setHorizontallyScrolling(false);
		edtNewCom.setMaxLines(Integer.MAX_VALUE);
		
		// resets
		
		getActivity().setTitle(from);
		
		txtFromB.setText(from);
		txtFromS.setText(from);
		txtBodyB.setText(Html.fromHtml(body));
		txtBodyS.setText(Html.fromHtml(body));
		
		txtToB.setVisibility(View.GONE);
		txtToS.setVisibility(View.GONE);
		imgIsDM.setVisibility(View.GONE);
		
		tabh.setup();
		
		final List<String> tids = new ArrayList<String>(
			Arrays.asList(new String[] { "recps", "files", "likes", "comms" }));
		
		tabh.addTab(tabh.newTabSpec(tids.get(0)).setContent(R.id.txt_dummy1).setIndicator("0"));
		tabh.addTab(tabh.newTabSpec(tids.get(1)).setContent(R.id.txt_dummy2).setIndicator("0"));
		tabh.addTab(tabh.newTabSpec(tids.get(2)).setContent(R.id.txt_dummy3).setIndicator("0"));
		tabh.addTab(tabh.newTabSpec(tids.get(3)).setContent(R.id.txt_dummy4).setIndicator("0"));
		
		setTabButton(0, false);
		setTabButton(1, false);
		setTabButton(2, false);
		setTabButton(3, false);
		
		// events
		
		imgFromB.setOnClickListener(onClickFrom);
		imgFromS.setOnClickListener(onClickFrom);
		
		llFloat.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				listView.setSelection(0);
				checkFloatingStuff();
			}
		});
		imgGoDn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				listView.setSelection(listView.getCount() - 1);
				checkFloatingStuff();
			}
		});
		
		tabh.setOnTabChangedListener(new OnTabChangeListener() {
			@Override
			public void onTabChanged(String tabId) {
				int idx = tids.indexOf(tabId);
				listView.setAdapter(adapters[idx]);
				// listView.setOnItemClickListener(onClickItem[idx]);
				listView.setOnItemLongClickListener(onLongClickItem[idx]);
				currentTab = idx;
				checkFloatingStuff();
			}
		});
		
		listView.setOnScrollListener(new OnScrollListener() {
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				switch (scrollState) {
					case OnScrollListener.SCROLL_STATE_IDLE:
						imgGoDn.setAlpha((float) 1.0);
						break;
					case OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
						imgGoDn.setAlpha((float) 0.5);
						break;
					case OnScrollListener.SCROLL_STATE_FLING:
						imgGoDn.setAlpha((float) 0.2);
						break;
				}
			}
			@Override
			public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
				checkFloatingStuff();
			}
		});
		
		return view;
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
		
		outState.putString("from", from);
		outState.putString("body", body);
		// state
		outState.putInt("tab", currentTab);
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.entry, menu);
		
		miLike = menu.findItem(R.id.action_entry_like);
		miUnlike = menu.findItem(R.id.action_entry_unlike);
		miSpeedC = menu.findItem(R.id.action_entry_speedc);
		miSummC = menu.findItem(R.id.action_entry_summ);
		miShare = menu.findItem(R.id.action_entry_share);
		miBrowse = menu.findItem(R.id.action_entry_web);
		miEdit = menu.findItem(R.id.action_entry_edit);
		miDelete = menu.findItem(R.id.action_entry_delete);
	}
	
	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		boolean ctx = (entry != null);
		
		miEdit.setVisible(ctx && entry.canEdit());
		miDelete.setVisible(ctx && entry.canDelete());
		miLike.setVisible(ctx && entry.canLike());
		miUnlike.setVisible(ctx && entry.canUnlike());
		miSpeedC.setVisible(ctx && entry.canComment());
		miSummC.setVisible(ctx && entry.comments.size() >= 30);
		miShare.setVisible(ctx);
		miBrowse.setVisible(ctx);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.equals(miEdit)) {
			mContainer.openPostEdit(entry.id, entry.rawBody);
			return true;
		}
		if (item.equals(miDelete)) {
			new AlertDialog.Builder(getActivity()).setTitle(R.string.action_entry_delete).setMessage(
				R.string.ask_delete_entry).setPositiveButton(R.string.dlg_btn_ok,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						doDelete();
					}
				}).setIcon(android.R.drawable.ic_dialog_alert).setCancelable(true).create().show();
			return true;
		}
		if (item.equals(miLike)) {
			doLike();
			return true;
		}
		if (item.equals(miUnlike)) {
			doUnlike();
			return true;
		}
		if (item.equals(miSpeedC)) {
			new AlertDialog.Builder(getActivity()).setTitle(R.string.action_entry_speedc).setItems(
				R.array.speed_comments, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						doInsComment(getResources().getStringArray(R.array.speed_comments)[which]);
					}
				}).setIcon(R.drawable.entry_comment).setCancelable(true).create().show();
			return true;
		}
		if (item.equals(miSummC)) {
			List<String> sl = new ArrayList<String>();
			for (Comment c : entry.comments)
				sl.add(c.rawBody);
			String st = Html.fromHtml(TextUtils.join("\n", sl)).toString().replace("..", ".").replace("\n", ". ").trim();
			int sn = new Random().nextInt((5 - 3) + 1) + 3;
			String sr = new SimpleSummariser().summarise(st, sn);
			new AlertDialog.Builder(getActivity()).setTitle(R.string.comms_summary).setMessage(sr).setIcon(
				R.drawable.entry_comment).setCancelable(true).create().show();
			return true;
		}
		if (item.equals(miShare)) {
			final String[] dsts = new String[] { session.getUsername() };
			final String body = "FWD: " + entry.rawBody;
			final String link = entry.url;
			final String[] imgs = entry.getMediaUrls(false);
			mContainer.openPostNew(dsts, body, link, imgs);
			return true;
		}
		if (item.equals(miBrowse)) {
			startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(entry.url)));
			return true;
		}
		Toast.makeText(getActivity(), item.getTitle(), Toast.LENGTH_SHORT).show();
		return false;
	}
	
	@Override
	public void onClick(View v) {
		int pos;
		try {
			pos = (Integer) v.getTag();
		} catch (Exception err) {
			return; // wtf?
		}
		switch (v.getId()) {
			case R.id.img_recp_fv:
				BaseFeed recp = (BaseFeed) adapters[0].getItem(pos);
				String msg;
				if (Entry.bFeeds.contains(recp.id)) {
					Entry.bFeeds.remove(recp.id);
					msg = getString(R.string.res_feed_unhidden);
				} else {
					Entry.bFeeds.add(recp.id);
					msg = getString(R.string.res_feed_hidden);
				}
				String flt = TextUtils.join(", ", Entry.bFeeds);
				SharedPreferences.Editor editor = session.getPrefs().edit();
				if (TextUtils.isEmpty(flt))
					editor.remove(PK.FEED_HBF);
				else
					editor.putString(PK.FEED_HBF, flt);
				editor.commit();
				Toast.makeText(getActivity(), msg.replace("@", recp.id), Toast.LENGTH_LONG).show();
				adapters[0].notifyDataSetChanged();
				break;
			case R.id.img_comm_popup:
				pauseUpdates(false);
				final Comment comm = (Comment) adapters[3].getItem(pos);
				PopupMenu popup = new PopupMenu(getActivity(), v);
				final MenuItem micedt = popup.getMenu().add(getString(R.string.action_comm_edit));
				final MenuItem micdel = popup.getMenu().add(getString(R.string.action_comm_delete));
				micedt.setVisible(comm.canEdit());
				micdel.setVisible(comm.canDelete());
				popup.setOnDismissListener(onDismissPopup);
				popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem item) {
						if (item.equals(micedt)) {
							pauseUpdates(false);
							LayoutInflater inflater = getActivity().getLayoutInflater();
							final View view = inflater.inflate(R.layout.dialog_comm_edit, null);
							final EditText edt = (EditText) view.findViewById(R.id.edt_comment);
							final Context ctx = getActivity();
							ctx.setTheme(android.R.style.Theme_Holo_Light); // setting the theme again to fix invisible CAB icons
							final AlertDialog dlg = new AlertDialog.Builder(ctx).setTitle(
								R.string.action_comm_edit).setView(view).setOnDismissListener(onDismissDialog).setPositiveButton(
								R.string.dlg_btn_ok, new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										doUpdComment(comm.id, edt.getText().toString());
									}
								}).setIcon(R.drawable.entry_comment).setCancelable(true).create();
							edt.setText(comm.rawBody);
							edt.setTextIsSelectable(true);
							edt.setOnFocusChangeListener(new View.OnFocusChangeListener() {
								@Override
								public void onFocusChange(View v, boolean hasFocus) {
									if (hasFocus)
										dlg.getWindow().setSoftInputMode(
											WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
								}
							});
							dlg.show();
							return true;
						}
						if (item.equals(micdel)) {
							pauseUpdates(false);
							LayoutInflater inflater = getActivity().getLayoutInflater();
							View view = inflater.inflate(R.layout.dialog_comm_show, null);
							Commons.picasso(getActivity()).load(comm.from.getAvatarUrl()).placeholder(
								R.drawable.nomugshot).into((ImageView) view.findViewById(R.id.img_comm_from));
							((TextView) view.findViewById(R.id.txt_comm_from)).setText(comm.from.getName());
							((TextView) view.findViewById(R.id.txt_comm_body)).setText(Html.fromHtml(comm.body));
							new AlertDialog.Builder(getActivity()).setTitle(R.string.action_comm_delete).setView(view).setOnDismissListener(
								onDismissDialog).setPositiveButton(R.string.dlg_btn_ok,
								new DialogInterface.OnClickListener() {
									@Override
									public void onClick(DialogInterface dialog, int which) {
										doDelComment(comm.id, false);
									}
								}).setIcon(R.drawable.entry_comment).setCancelable(true).create().show();
							return true;
						}
						Toast.makeText(getActivity(), item.getTitle(), Toast.LENGTH_SHORT).show();
						return false;
					}
				});
				popup.show();
				break;
		}
	}
	
	@Override
	protected void initFragment() {
		entry = session.cachedEntry;
		if (entry == null || !entry.isIt(eid))
			loadEntry();
		else {
			updateView();
			resumeUpdates(false);
		}
	}
	
	private void loadEntry() {
		mProgress.setTitle(R.string.waiting_entry);
		mProgress.show();
		Callback<Entry> callback = new Callback<Entry>() {
			@Override
			public void success(Entry result, Response response) {
				mProgress.dismiss();
				entry = result;
				updateView();
				resumeUpdates(false);
			}
			
			@Override
			public void failure(RetrofitError error) {
				mProgress.dismiss();
				new AlertDialog.Builder(getActivity()).setTitle(R.string.res_rfcall_failed).setMessage(
					Commons.retrofitErrorText(error)).setOnDismissListener(new OnDismissListener() {
					@Override
					public void onDismiss(DialogInterface dialog) {
						getActivity().getFragmentManager().popBackStack();
					}
				}).setPositiveButton(R.string.dlg_btn_retry, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						loadEntry();
					}
				}).setIcon(android.R.drawable.ic_dialog_alert).setCancelable(true).create().show();
			}
		};
		FFAPI.client_entry(session).get_entry_async(eid, callback);
	}
	
	private void pauseUpdates(boolean showHourglass) {
		if (timer != null)
			timer.cancel();
		if (showHourglass)
			getActivity().setProgressBarIndeterminateVisibility(true);
	}
	
	private void resumeUpdates(boolean removeHourglass) {
		if (getActivity() == null) // ???
			return;
		if (timer != null)
			timer.cancel();
		timer = new Timer(true);
		updater = new UpdaterTask();
		timer.schedule(updater, 500, 10000);
		if (removeHourglass)
			getActivity().setProgressBarIndeterminateVisibility(false);
	}
	
	private void setTabButton(int index, boolean changed) {
		LinearLayout ll = (LinearLayout) tabw.getChildAt(index);
		TextView tv = (TextView) ll.getChildAt(1);
		tv.setText(adapters[index].getCount() > 0 ? Integer.toString(adapters[index].getCount()) : "");
		tv.setCompoundDrawablePadding(15);
		tv.setCompoundDrawablesWithIntrinsicBounds(adapters[index].getIcon(), 0, 0, 0);
		if (changed)
			tv.startAnimation(blink);
	}
	
	private void setCommentView() {
		if (entry.canComment()) {
			edtNewCom.setEnabled(true);
			edtNewCom.setOnEditorActionListener(new OnEditorActionListener() {
				@Override
				public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
					if (actionId == EditorInfo.IME_ACTION_SEND && !TextUtils.isEmpty(edtNewCom.getText().toString()))
						doInsComment(edtNewCom.getText().toString());
					return false;
				}
			});
		} else {
			edtNewCom.setEnabled(false);
			edtNewCom.setOnEditorActionListener(null);
			edtNewCom.setText(R.string.entry_cant_comm);
		}
	}
	
	private void checkFloatingStuff() {
		if (listView.getCount() <= 0) {
			llFloat.setVisibility(View.GONE);
			imgGoDn.setVisibility(View.GONE);
		} else {
			if (listView.getFirstVisiblePosition() > 1)
				llFloat.setVisibility(View.VISIBLE);
			else
				llFloat.setVisibility(View.GONE);
			if (listView.getLastVisiblePosition() < listView.getCount() - 1)
				imgGoDn.setVisibility(View.VISIBLE);
			else
				imgGoDn.setVisibility(View.GONE);
		}
	}
	
	private void updateView() {
		if (getActivity() == null)
			return; // wtf?
			
		Commons.picasso(getActivity()).load(entry.from.getAvatarUrl()).placeholder(R.drawable.nomugshot).into(imgFromB);
		Commons.picasso(getActivity()).load(entry.from.getAvatarUrl()).placeholder(R.drawable.nomugshot).into(imgFromS);
		
		from = entry.from.getName();
		getActivity().setTitle(from);
		txtFromB.setText(from);
		txtFromS.setText(from);
		
		txtFromB.setCompoundDrawablesWithIntrinsicBounds(entry.from.locked ? R.drawable.entry_private : 0, 0, 0, 0);
		txtFromS.setCompoundDrawablesWithIntrinsicBounds(entry.from.locked ? R.drawable.entry_private : 0, 0, 0, 0);
		
		imgIsDM.setVisibility(entry.isDM() ? View.VISIBLE : View.GONE);
		
		String tmp = entry.getToLine();
		if (tmp == null) {
			txtToB.setVisibility(View.GONE);
			txtToS.setVisibility(View.GONE);
		} else {
			txtToB.setText(tmp);
			txtToB.setVisibility(View.VISIBLE);
			txtToS.setText(tmp);
			txtToS.setVisibility(View.VISIBLE);
		}

		String tl = entry.getFuzzyTime();
		if (entry.via != null && !TextUtils.isEmpty(entry.via.name.trim()))
			tl += new StringBuilder().append(" ").append(getString(R.string.source_prefix)).append(" ").append(
				entry.via.name.trim()).toString();
		txtTimeB.setText(tl);
		
		body = entry.body;
		txtBodyB.setText(Html.fromHtml(body));
		txtBodyS.setText(Html.fromHtml(body));
		
		int y = listView.getScrollY();
		int n = listView.getCount();
		boolean last = (listView.getLastVisiblePosition() == n - 1);
		setTabButton(0, adapters[0].setEntry(entry) != 0);
		setTabButton(1, adapters[1].setEntry(entry) != 0);
		setTabButton(2, adapters[2].setEntry(entry) != 0);
		setTabButton(3, adapters[3].setEntry(entry) != 0);
		if (currentTab < 0 || tabh.getCurrentTab() < 0 || listView.getAdapter() == null)
			tabh.setCurrentTab(entry.comments.size() <= 0 && entry.thumbnails.length > 0 ? 1 : 3);
		else if (last && listView.getCount() > n)
			listView.smoothScrollToPosition(n);
		else
			listView.scrollTo(0, y);
		
		setCommentView();
		
		if (miShare != null)
			onPrepareOptionsMenu(null);
	}
	
	private void doDelete() {
		pauseUpdates(true);
		Callback<Entry> callback = new Callback<Entry>() {
			@Override
			public void success(Entry result, Response response) {
				Feed.lastDeletedEntry = result.id;
				Toast.makeText(getActivity(), getString(R.string.res_delentr_ok), Toast.LENGTH_LONG).show();
				getActivity().setProgressBarIndeterminateVisibility(false);
				getActivity().getFragmentManager().popBackStack();
			}
			
			@Override
			public void failure(RetrofitError error) {
				getActivity().setProgressBarIndeterminateVisibility(false);
				new AlertDialog.Builder(getActivity()).setTitle(R.string.res_rfcall_failed).setMessage(
					Commons.retrofitErrorText(error)).setOnDismissListener(onDismissDialog).setPositiveButton(
					R.string.dlg_btn_retry, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							doDelete();
						}
					}).setIcon(android.R.drawable.ic_dialog_alert).setCancelable(true).create().show();
			}
		};
		FFAPI.client_write(session).del_entry(entry.id, callback);
	}
	
	private void doLike() {
		pauseUpdates(true);
		Callback<Like> callback = new Callback<Like>() {
			@Override
			public void success(Like result, Response response) {
				entry.likes.add(result);
				resumeUpdates(true);
				Toast.makeText(getActivity(), getString(R.string.res_inslike_ok), Toast.LENGTH_SHORT).show();
			}
			
			@Override
			public void failure(RetrofitError error) {
				getActivity().setProgressBarIndeterminateVisibility(false);
				new AlertDialog.Builder(getActivity()).setTitle(R.string.res_rfcall_failed).setMessage(
					Commons.retrofitErrorText(error)).setOnDismissListener(onDismissDialog).setPositiveButton(
					R.string.dlg_btn_retry, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							doLike();
						}
					}).setIcon(android.R.drawable.ic_dialog_alert).setCancelable(true).create().show();
			}
		};
		FFAPI.client_write(session).ins_like(entry.id, callback);
	}
	
	private void doUnlike() {
		pauseUpdates(true);
		Callback<SimpleResponse> callback = new Callback<SimpleResponse>() {
			@Override
			public void success(SimpleResponse result, Response response) {
				int i = entry.indexOfLike(session.getUsername());
				if (i >= 0) {
					entry.likes.remove(i);
					entry.commands.set(entry.commands.indexOf("unlike"), "like"); // sadly, we need to do this.
				}
				resumeUpdates(true);
				Toast.makeText(getActivity(), getString(R.string.res_dellike_ok), Toast.LENGTH_SHORT).show();
			}
			
			@Override
			public void failure(RetrofitError error) {
				getActivity().setProgressBarIndeterminateVisibility(false);
				new AlertDialog.Builder(getActivity()).setTitle(R.string.res_rfcall_failed).setMessage(
					Commons.retrofitErrorText(error)).setOnDismissListener(onDismissDialog).setPositiveButton(
					R.string.dlg_btn_retry, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							doUnlike();
						}
					}).setIcon(android.R.drawable.ic_dialog_alert).setCancelable(true).create().show();
			}
		};
		FFAPI.client_write(session).del_like(entry.id, callback);
	}
	
	private void doInsCommentExt(final String entry_id, final String body) {
		pauseUpdates(true);
		Callback<Comment> callback = new Callback<Comment>() {
			@Override
			public void success(Comment result, Response response) {
				edtNewCom.setText(null);
				entry.comments.add(result);
				tabh.setCurrentTab(3);
				adapters[3].notifyDataSetChanged();
				listView.smoothScrollToPosition(listView.getCount());
				resumeUpdates(true);
				Toast.makeText(getActivity(), getString(R.string.res_inscomm_ok), Toast.LENGTH_SHORT).show();
			}
			
			@Override
			public void failure(RetrofitError error) {
				getActivity().setProgressBarIndeterminateVisibility(false);
				new AlertDialog.Builder(getActivity()).setTitle(R.string.res_rfcall_failed).setMessage(
					Commons.retrofitErrorText(error)).setOnDismissListener(onDismissDialog).setPositiveButton(
					R.string.dlg_btn_retry, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							doInsComment(body);
						}
					}).setIcon(android.R.drawable.ic_dialog_alert).setCancelable(true).create().show();
			}
		};
		FFAPI.client_write(session).ins_comment(entry_id, body, callback);
	}
	
	private void doInsComment(final String body) {
		doInsCommentExt(entry.id, body);
	}
	
	private void doUpdComment(final String cid, final String body) {
		pauseUpdates(true);
		Callback<Comment> callback = new Callback<Comment>() {
			@Override
			public void success(Comment result, Response response) {
				int i = entry.indexOfComment(cid);
				if (i >= 0)
					entry.comments.set(i, result);
				resumeUpdates(true);
				Toast.makeText(getActivity(), getString(R.string.res_updcomm_ok), Toast.LENGTH_SHORT).show();
			}
			
			@Override
			public void failure(RetrofitError error) {
				getActivity().setProgressBarIndeterminateVisibility(false);
				new AlertDialog.Builder(getActivity()).setTitle(R.string.res_rfcall_failed).setMessage(
					Commons.retrofitErrorText(error)).setOnDismissListener(onDismissDialog).setPositiveButton(
					R.string.dlg_btn_retry, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							doUpdComment(cid, body);
						}
					}).setIcon(android.R.drawable.ic_dialog_alert).setCancelable(true).create().show();
			}
		};
		FFAPI.client_write(session).upd_comment(entry.id, cid, body, callback);
	}
	
	private void doDelComment(final String cid, final boolean undelete) {
		pauseUpdates(true);
		Callback<Comment> callback = new Callback<Comment>() {
			@Override
			public void success(Comment result, Response response) {
				if (undelete)
					entry.comments.add(result);
				else {
					int i = entry.indexOfComment(cid);
					if (i >= 0)
						entry.comments.remove(i);
				}
				resumeUpdates(true);
				Toast.makeText(getActivity(), getString(R.string.res_delcomm_ok), Toast.LENGTH_SHORT).show();
			}
			
			@Override
			public void failure(RetrofitError error) {
				getActivity().setProgressBarIndeterminateVisibility(false);
				new AlertDialog.Builder(getActivity()).setTitle(R.string.res_rfcall_failed).setMessage(
					Commons.retrofitErrorText(error)).setOnDismissListener(onDismissDialog).setPositiveButton(
					R.string.dlg_btn_retry, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							doDelComment(cid, undelete);
						}
					}).setIcon(android.R.drawable.ic_dialog_alert).setCancelable(true).create().show();
			}
		};
		if (undelete)
			FFAPI.client_write(session).und_comment(cid, 1, callback);
		else
			FFAPI.client_write(session).del_comment(cid, callback);
	}
	
	private class UpdaterTask extends TimerTask {
		@Override
		public void run() {
			final Activity context = getActivity();
			try {
				final Entry upd = FFAPI.client_entry(session).get_entry(eid);
				context.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (!EntryFragment.this.isVisible() || EntryFragment.this.isRemoving())
							return;
						entry = upd;
						updateView();
					}
				});
			} catch (Exception error) {
				final String text = error instanceof RetrofitError ? Commons.retrofitErrorText((RetrofitError) error)
					: error.getMessage();
				context.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						if (!EntryFragment.this.isVisible() || EntryFragment.this.isRemoving())
							return;
						new AlertDialog.Builder(context).setTitle("UpdaterTask").setMessage(text).setIcon(
							android.R.drawable.ic_dialog_alert).show();
					}
				});
			}
		}
	}
}