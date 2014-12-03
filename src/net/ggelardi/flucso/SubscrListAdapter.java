package net.ggelardi.flucso;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.ggelardi.flucso.R;
import net.ggelardi.flucso.FFAPI.BaseFeed;
import net.ggelardi.flucso.FFAPI.FeedInfo;
import net.ggelardi.flucso.FFAPI.SimpleResponse;
import net.ggelardi.flucso.FFAPI.FeedList.SectionItem;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;
import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;

public class SubscrListAdapter extends BaseAdapter {

	private final BaseFeed target;
	private final Context context;
	private final FFSession session;
	private final List<ListRef> lstsubs;
	private final LayoutInflater inflater;
	
	public SubscrListAdapter(Context context, BaseFeed target) {
		super();

		this.target = target;
		this.context = context;
		session = FFSession.getInstance(context);
		lstsubs = new ArrayList<ListRef>();
		inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		
		loadData();
	}
	
	@Override
	public int getCount() {
		return lstsubs.size();
	}
	
	@Override
	public ListRef getItem(int position) {
		return lstsubs.get(position);
	}
	
	@Override
	public long getItemId(int position) {
		return position;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder vh;
		View view = convertView;
		if (view == null) {
			view = inflater.inflate(R.layout.item_subscr, parent, false);
			vh = new ViewHolder();
			vh.img = (ImageView) view.findViewById(R.id.img_subscr);
			vh.sw = (Switch) view.findViewById(R.id.sw_subscr);
			vh.sw.setOnCheckedChangeListener(new OnCheckedChangeListener() {
				@Override
				public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
					int pos;
					try {
						pos = (Integer) buttonView.getTag();
					} catch (Exception err) {
						return; // wtf?
					}
					getItem(pos).toggle(isChecked);
				}});
			view.setTag(vh);
		} else {
			vh = (ViewHolder) view.getTag();
		}
		ListRef item = getItem(position);
		Commons.picasso(context).load("http://friendfeed-api.com/v2/picture/" + item.fid + "?size=large").placeholder(
			R.drawable.nomugshot).into(vh.img);
		vh.sw.setTag(Integer.valueOf(position));
		vh.sw.setText(item.name);
		vh.sw.setChecked(item.active);
		vh.sw.setEnabled(!item.checking);
		vh.sw.setCompoundDrawablesRelativeWithIntrinsicBounds(item.checking ? R.drawable.ic_action_time : 0, 0, 0, 0);
		return view;
	}
	
	private void loadData() {
		if (target.isList()) {
			Callback<FeedInfo> callback = new Callback<FeedInfo>() {
				@Override
				public void success(FeedInfo result, Response response) {
					List<BaseFeed> lst = Arrays.asList(result.feeds);
					Collections.sort(lst, new Comparator<BaseFeed>() {
						@Override
						public int compare(BaseFeed lhs, BaseFeed rhs) {
							return lhs.name.compareToIgnoreCase(rhs.name);
						}
					});
					for (BaseFeed feed: lst)
						lstsubs.add(new ListRef(feed.id, feed.name, true));
					notifyDataSetChanged();
				}
				@Override
				public void failure(RetrofitError error) {
					notifyDataSetChanged();
					new AlertDialog.Builder(context).setTitle(R.string.res_rfcall_failed).setMessage(
						Commons.retrofitErrorText(error)).setIcon(android.R.drawable.ic_dialog_alert).create().show();
				}
			};
			FFAPI.client_profile(session).get_profile(target.id, callback);
		} else {
			lstsubs.add(new ListRef("home", "Home", false));
			for (SectionItem s: session.navigation.lists)
				lstsubs.add(new ListRef(s.id, s.name, false));
			checkTargetPresence(0, true);
		}
	}

	private void checkTargetPresence(final int position, final boolean next) {
		if (position > getCount() - 1)
			return;
		final ListRef item = getItem(position);
		item.checking = true;
		Callback<FeedInfo> callback = new Callback<FeedInfo>() {
			@Override
			public void success(FeedInfo result, Response response) {
				item.active = false;
				item.checking = false;
				for (BaseFeed f: result.feeds)
					if (f.isIt(target.id)) {
						item.active = true;
						break;
					}
				if (next)
					checkTargetPresence(position+1, next);
				notifyDataSetChanged();
			}
			@Override
			public void failure(RetrofitError error) {
				item.checking = false;
				notifyDataSetChanged();
				new AlertDialog.Builder(context).setTitle(R.string.res_rfcall_failed).setMessage(
					Commons.retrofitErrorText(error)).setIcon(android.R.drawable.ic_dialog_alert).create().show();
			}
		};
		FFAPI.client_profile(session).get_profile(item.fid, callback);
	}
	
	class ListRef {
		String fid;
		String name;
		boolean active = false;
		boolean checking = false;
		
		public ListRef(String fid, String name, boolean active) {
			this.fid = fid;
			this.name = name;
			this.active = active;
		}
		
		public void toggle(boolean value) {
			if (checking)
				return;
			checking = true;
			Callback<SimpleResponse> callback = new Callback<SimpleResponse>() {
				@Override
				public void success(SimpleResponse result, Response response) {
					Log.v("subscr", result.status);
					if (result.status.equals("subscribed"))
						active = true;
					else if (result.status.equals("unsubscribed"))
						active = false;
					else if (result.status != "")
						Toast.makeText(context, result.status, Toast.LENGTH_SHORT).show();
					else
						Toast.makeText(context, "Unknown error", Toast.LENGTH_LONG).show();
					checking = false;
					notifyDataSetChanged();
				}
				@Override
				public void failure(RetrofitError error) {
					checking = false;
					notifyDataSetChanged();
					new AlertDialog.Builder(context).setTitle(R.string.res_rfcall_failed).setMessage(
						Commons.retrofitErrorText(error)).setIcon(android.R.drawable.ic_dialog_alert).create().show();
				}
			};
			final String feedId = target.isList() ? fid : target.id;
			final String listId = target.isList() ? target.id : fid;
			if (value && !active)
				FFAPI.client_write(session).subscribe(feedId, listId, callback);
			else if (!value && active)
				FFAPI.client_write(session).unsubscribe(feedId, listId, callback);
			else {
				checking = false;
				notifyDataSetChanged();
			}
		}
	}
	
	static class ViewHolder {
		Switch sw;
		ImageView img;
	}
}