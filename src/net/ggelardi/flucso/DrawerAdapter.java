package net.ggelardi.flucso;

import net.ggelardi.flucso.FFAPI.FeedList.Section;
import net.ggelardi.flucso.FFAPI.FeedList.SectionItem;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class DrawerAdapter extends BaseAdapter {
	private static final int TYPE_HEADER = 0;
	private static final int TYPE_ITEM = 1;
	
	private FFSession session;
	private LayoutInflater inflater;
	
	public DrawerAdapter(Context context) {
		super();
		
		session = FFSession.getInstance(context);
		inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}
	
	@Override
	public boolean areAllItemsEnabled() {
		return false;
	}
	
	@Override
	public boolean isEnabled(int position) {
		return getItem(position) instanceof SectionItem;
	}
	
	@Override
	public int getCount() {
		int total = 0;
		if (session.hasProfile() && session.getNavigation().sections != null) {
			for (Section sec : session.getNavigation().sections)
				total += sec.feeds.length + 1;
		}
		return total;
	}
	
	@Override
	public Object getItem(int position) {
		if (position >= 0 && session.hasProfile() && session.getNavigation().sections != null) {
			int offset = 0;
			int index = -1;
			for (Section sec : session.getNavigation().sections) {
				if (position <= (offset + sec.feeds.length)) {
					index = position - offset - 1;
					return index < 0 ? sec : sec.feeds[index]; // a Section or a SectionItem
				} else
					offset += sec.feeds.length + 1;
			}
		}
		return null;
	}
	
	@Override
	public long getItemId(int position) {
		return position;
	}
	
	@Override
	public int getViewTypeCount() {
		return 2;
	}
	
	@Override
	public int getItemViewType(int position) {
		return getItem(position) instanceof Section ? TYPE_HEADER : TYPE_ITEM;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder = null;
		if (convertView == null) {
			if (getItemViewType(position) == TYPE_HEADER)
				convertView = inflater.inflate(R.layout.header_drawer, parent, false);
			else
				convertView = inflater.inflate(R.layout.item_drawer, parent, false);
			holder = new ViewHolder();
			holder.textView = (TextView) convertView.findViewById(R.id.txtLabel);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		Object item = getItem(position);
		holder.textView.setText(item instanceof Section ? ((Section) item).name : ((SectionItem) item).name);
		
		return convertView;
	}
	
	public static class ViewHolder {
		public TextView textView;
	}
}