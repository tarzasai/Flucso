package net.ggelardi.flucso.data;

import static android.widget.ImageView.ScaleType.CENTER_CROP;

import java.util.ArrayList;

import net.ggelardi.flucso.R;
import net.ggelardi.flucso.R.drawable;
import net.ggelardi.flucso.comp.SquaredImageView;
import net.ggelardi.flucso.serv.Commons;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

public class PostThmbAdapter extends BaseAdapter {
	
	private final ArrayList<String> imglist = new ArrayList<String>();
	private final Context context;
	
	public PostThmbAdapter(Context context) {
		super();
		
		this.context = context;
	}
	
	@Override
	public int getCount() {
		return imglist.size();
	}
	
	@Override
	public String getItem(int position) {
		return imglist.get(position);
	}
	
	@Override
	public long getItemId(int position) {
		return position;
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		SquaredImageView view = (SquaredImageView) convertView;
		if (view == null) {
			view = new SquaredImageView(context);
			view.setScaleType(CENTER_CROP);
		}
		Commons.picasso(context).load(getItem(position)).placeholder(R.drawable.ic_action_picture).error(
			R.drawable.ic_action_picture).fit().into(view);
		return view;
	}
	
	public void clear() {
		imglist.clear();
		notifyDataSetChanged();
	}
	
	public boolean append(String item) {
		if (imglist.contains(item))
			return false;
		imglist.add(item);
		notifyDataSetChanged();
		return true;
	}
	
	public void remove(int position) {
		if (position >= 0 && position < imglist.size()) {
			imglist.remove(position);
			notifyDataSetChanged();
		}
	}
}