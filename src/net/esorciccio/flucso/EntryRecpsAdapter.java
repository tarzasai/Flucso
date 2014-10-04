package net.esorciccio.flucso;

import net.esorciccio.flucso.FFAPI.BaseFeed;
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
	public Object getItem(int position) {
		return (position < 0 || position >= getCount()) ? null : entry.to[position];
	}
	
	@Override
	public long getItemId(int position) {
		return position;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;
		View view = convertView;
		if (view == null) {
			view = inflater.inflate(R.layout.item_entry_recp, parent, false);
			holder = new ViewHolder();
			holder.img = (ImageView) view.findViewById(R.id.img_to);
			holder.txt = (TextView) view.findViewById(R.id.txt_to);
			view.setTag(holder);
		} else {
			holder = (ViewHolder) view.getTag();
		}
		BaseFeed rec = (BaseFeed) getItem(position);
		Commons.picasso(view.getContext().getApplicationContext()).load(rec.getAvatarUrl()).placeholder(
			R.drawable.nomugshot_medium).into(holder.img);
		holder.txt.setCompoundDrawablesWithIntrinsicBounds(rec.locked ? R.drawable.entry_private : 0, 0, 0, 0);
		holder.txt.setText(rec.getName());
		return view;
	}
	
	public static class ViewHolder {
		public ImageView img;
		public TextView txt;
	}

	@Override
	public int getIcon() {
		return R.drawable.entry_target;
	}
}