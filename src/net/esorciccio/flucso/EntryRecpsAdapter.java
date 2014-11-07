package net.esorciccio.flucso;

import net.esorciccio.flucso.FFAPI.BaseFeed;
import net.esorciccio.flucso.FFAPI.Entry;
import android.content.Context;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class EntryRecpsAdapter extends EntryBaseAdapter {
	
	public EntryRecpsAdapter(Context context, OnClickListener clickListener) {
		super(context, clickListener);
	}
	
	@Override
	public int getCount() {
		return entry != null ? entry.to.length : 0;
	}
	
	@Override
	public BaseFeed getItem(int position) {
		return entry.to[position];
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
			view = inflater.inflate(R.layout.item_entry_recp, parent, false);
			vh = new ViewHolder();
			vh.img = (ImageView) view.findViewById(R.id.img_recp_to);
			vh.txt = (TextView) view.findViewById(R.id.txt_recp_to);
			vh.flt = (ImageView) view.findViewById(R.id.img_recp_fv);
			vh.flt.setOnClickListener(listener);
			view.setTag(vh);
		} else {
			vh = (ViewHolder) view.getTag();
		}
		vh.flt.setTag(Integer.valueOf(position));
		BaseFeed rec = getItem(position);
		Commons.picasso(view.getContext().getApplicationContext()).load(rec.getAvatarUrl()).placeholder(
			R.drawable.nomugshot).into(vh.img);
		vh.txt.setCompoundDrawablesWithIntrinsicBounds(rec.locked ? R.drawable.entry_private : 0, 0, 0, 0);
		vh.txt.setText(rec.getName());
		vh.flt.setVisibility(rec.isGroup() || (rec.isUser() && !session.profile.isIt(rec.id)) ? View.VISIBLE : View.GONE);
		vh.flt.setImageResource(Entry.bFeeds.contains(rec.id) ? R.drawable.feed_hidden : R.drawable.feed_visible);
		return view;
	}
	
	public static class ViewHolder {
		public ImageView img;
		public TextView txt;
		public ImageView flt;
	}

	@Override
	public int getIcon() {
		return R.drawable.entry_target;
	}
}