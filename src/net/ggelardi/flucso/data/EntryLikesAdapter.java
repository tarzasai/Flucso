package net.ggelardi.flucso.data;

import net.ggelardi.flucso.R;
import net.ggelardi.flucso.serv.Commons;
import net.ggelardi.flucso.serv.FFAPI.Like;
import android.content.Context;
import android.text.Html;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

public class EntryLikesAdapter extends EntryBaseAdapter {
	
	public EntryLikesAdapter(Context context, OnClickListener clickListener) {
		super(context, clickListener);
	}
	
	@Override
	public int getCount() {
		return entry != null ? entry.likes.size() : 0;
	}
	
	@Override
	public Object getItem(int position) {
		return (position < 0 || position >= getCount()) ? null : entry.likes.get(position);
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
			view = inflater.inflate(R.layout.item_entry_like, parent, false);
			holder = new ViewHolder();
			holder.imgFrom = (ImageView) view.findViewById(R.id.img_like_from);
			holder.txtFrom = (TextView) view.findViewById(R.id.txt_like_from);
			holder.txtTime = (TextView) view.findViewById(R.id.txt_like_time);
			holder.txtBody = (TextView) view.findViewById(R.id.txt_like_body);
			view.setTag(holder);
		} else {
			holder = (ViewHolder) view.getTag();
		}
		Like like = (Like) getItem(position);
		if (like.placeholder) {
			holder.imgFrom.setVisibility(View.GONE);
			holder.txtFrom.setVisibility(View.GONE);
			holder.txtTime.setVisibility(View.GONE);
			holder.txtBody.setText(Html.fromHtml(like.body));
			holder.txtBody.setVisibility(View.VISIBLE);
		} else {
			holder.imgFrom.setVisibility(View.VISIBLE);
			holder.txtFrom.setVisibility(View.VISIBLE);
			holder.txtTime.setVisibility(View.VISIBLE);
			holder.txtBody.setVisibility(View.GONE);
			Commons.picasso(view.getContext().getApplicationContext()).load(like.from.getAvatarUrl()).placeholder(
				R.drawable.nomugshot).into(holder.imgFrom);
			holder.txtFrom.setCompoundDrawablesRelativeWithIntrinsicBounds(like.from.locked ? R.drawable.entry_private : 0, 0, 0, 0);
			holder.txtFrom.setText(like.from.getName());
			holder.txtTime.setText(like.getFuzzyTime());
		}
		return view;
	}
	
	public static class ViewHolder {
		public ImageView imgFrom;
		public TextView txtFrom;
		public TextView txtTime;
		public TextView txtBody;
	}

	@Override
	public int getIcon() {
		return entry != null && entry.canUnlike() ? R.drawable.entry_liked : R.drawable.entry_like;
	}
}