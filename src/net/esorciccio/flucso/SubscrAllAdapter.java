package net.esorciccio.flucso;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import net.esorciccio.flucso.FFAPI.BaseFeed;
import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

public class SubscrAllAdapter extends BaseAdapter implements Filterable {
	
	public enum Scope {
	    ALL(0),
	    SUBSCRIPTIONS(1),
	    SUBSCRIBERS(2);

	    private int _value;

	    Scope(int Value) {
	        this._value = Value;
	    }

	    public int getValue() {
	            return _value;
	    }
	    
	    public static Scope fromValue(int v) {
	    	Scope[] Scopes = Scope.values();
            for (Scope s : Scopes)
                if (s.getValue() == v)
                    return s;
            return null;
	    }
	}
	
	private final FFSession session;
	private final LayoutInflater inflater;
	private ArrayList<BaseFeed> feedlist = new ArrayList<BaseFeed>();
	private Scope scope = Scope.ALL;
	
	public SubscrAllAdapter(Context context) {
		super();

		session = FFSession.getInstance(context);
		inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}
	
	@Override
	public int getCount() {
		return feedlist.size();
	}
	
	@Override
	public BaseFeed getItem(int position) {
		return feedlist.get(position);
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
			view = inflater.inflate(R.layout.item_post_dsrc, parent, false);
			vh = new ViewHolder();
			vh.img = (ImageView) view.findViewById(R.id.img_post_dsrc);
			vh.txt = (TextView) view.findViewById(R.id.txt_post_dsrc);
			view.setTag(vh);
		} else {
			vh = (ViewHolder) view.getTag();
		}
		Commons.picasso(view.getContext().getApplicationContext()).load(feedlist.get(position).getAvatarUrl()).placeholder(
			R.drawable.nomugshot).into(vh.img);
		vh.txt.setText(feedlist.get(position).getName());
		return view;
	}
	
	@Override
	public Filter getFilter() {
		return nameFilter;
	}
	
	public SubscrAllAdapter setScope(Scope value) {
		scope = value;
		return this;
	}
	
	private static boolean checkFeed(BaseFeed feed, String filter) {
		return TextUtils.isEmpty(filter) || feed.name.toLowerCase(Locale.getDefault()).contains(filter);
	}
	
	private ArrayList<BaseFeed> getFeedList(String filter) {
		ArrayList<BaseFeed> result = new ArrayList<BaseFeed>();
		List<String> check = new ArrayList<String>();
		if (scope.equals(Scope.ALL) || scope.equals(Scope.SUBSCRIPTIONS))
			for (BaseFeed feed : session.profile.subscriptions)
				if (checkFeed(feed, filter)) {
					result.add(feed);
					check.add(feed.id);
				}
		if (scope.equals(Scope.ALL) || scope.equals(Scope.SUBSCRIBERS))
			for (BaseFeed feed : session.profile.subscribers) {
				if (checkFeed(feed, filter) && !check.contains(feed.id))
					result.add(feed);
			}
		Collections.sort(result, new Comparator<BaseFeed>() {
			@Override
			public int compare(BaseFeed lhs, BaseFeed rhs) {
				return lhs.name.compareToIgnoreCase(rhs.name);
			}
		});
		return result;
	}
	
	Filter nameFilter = new Filter() {
		@Override
		public CharSequence convertResultToString(Object resultValue) {
			return ((BaseFeed) resultValue).name;
		}
		@Override
		protected FilterResults performFiltering(CharSequence constraint) {
			String sortValue = constraint == null ? "" : constraint.toString().toLowerCase(Locale.getDefault()).trim();
			FilterResults filterResults = new FilterResults();
			//if (!TextUtils.isEmpty(sortValue)) {
				ArrayList<BaseFeed> list = getFeedList(sortValue);
				filterResults.values = list;
				filterResults.count = list.size();
			//}
			return filterResults;
		}
		@SuppressWarnings("unchecked")
		@Override
		protected void publishResults(CharSequence constraint, FilterResults results) {
			if (results != null && results.count > 0) {
				feedlist = (ArrayList<BaseFeed>) results.values;
				notifyDataSetChanged();
			} else
				notifyDataSetInvalidated();
		}
	};
	
	static class ViewHolder {
		TextView txt;
		ImageView img;
	}
}