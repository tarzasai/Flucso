package net.esorciccio.flucso;

import java.util.ArrayList;

import net.esorciccio.flucso.FFAPI.BaseFeed;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class PostDSelAdapter extends BaseAdapter {
	
	private final ArrayList<BaseFeed> feedlist;
	private final OnClickListener listener;
	private final LayoutInflater inflater;
	
	public PostDSelAdapter(Context context) {
		super();
		
		feedlist = new ArrayList<BaseFeed>();
		listener = (OnClickListener) context;
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
			view = inflater.inflate(R.layout.item_post_dsel, parent, false);
			vh = new ViewHolder();
			vh.img = (ImageView) view.findViewById(R.id.img_post_dsel);
			vh.txt = (TextView) view.findViewById(R.id.txt_post_dsel);
			vh.del = (ImageView) view.findViewById(R.id.img_post_dsel_del);
			vh.del.setOnClickListener(listener);
			view.setTag(vh);
		} else {
			vh = (ViewHolder) view.getTag();
		}
		Commons.picasso(view.getContext().getApplicationContext()).load(feedlist.get(position).getAvatarUrl()).placeholder(
			R.drawable.nomugshot).into(vh.img);
		vh.txt.setText(feedlist.get(position).getName());
		vh.del.setTag(Integer.valueOf(position));
		return view;
	}
	
	public void clear() {
		feedlist.clear();
		notifyDataSetChanged();
	}
	
	public void append(BaseFeed item) {
		for (BaseFeed f : feedlist)
			if (f.isIt(item.id))
				return;
		Log.v(getClass().getSimpleName(), "append: " + item.id);
		feedlist.add(item);
		notifyDataSetChanged();
	}
	
	public void remove(int position) {
		if (position >= 0 && position < feedlist.size()) {
			Log.v(getClass().getSimpleName(), "remove: " + getItem(position).id);
			feedlist.remove(position);
			notifyDataSetChanged();
		}
	}
	
	public String[] getIDs() {
		String[] res = new String[getCount()];
		for (int i = 0; i < getCount(); i++)
			res[i] = feedlist.get(i).id;
		return res;
	}
	
	static class ViewHolder {
		ImageView img;
		TextView txt;
		ImageView del;
	}
}