package net.ggelardi.flucso.data;

import java.util.ArrayList;

import net.ggelardi.flucso.R;
import net.ggelardi.flucso.serv.Commons;
import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

public class PostFileAdapter extends BaseAdapter {
	
	private final ArrayList<ImageRef> imglist;
	private final OnClickListener listener;
	private final LayoutInflater inflater;
	
	public PostFileAdapter(Context context) {
		super();
		
		imglist = new ArrayList<ImageRef>();
		listener = (OnClickListener) context;
		inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}
	
	@Override
	public int getCount() {
		return imglist.size();
	}
	
	@Override
	public ImageRef getItem(int position) {
		return imglist.get(position);
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
			view = inflater.inflate(R.layout.item_post_file, parent, false);
			vh = new ViewHolder();
			vh.img = (ImageView) view.findViewById(R.id.img_post_file_src);
			vh.del = (ImageView) view.findViewById(R.id.img_post_file_del);
			vh.del.setOnClickListener(listener);
			view.setTag(vh);
		} else {
			vh = (ViewHolder) view.getTag();
		}
		Commons.picasso(view.getContext().getApplicationContext()).load(imglist.get(position).uri).placeholder(
			R.drawable.ic_action_picture).into(vh.img);
		vh.del.setTag(Integer.valueOf(position));
		return view;
	}
	
	public void clear() {
		imglist.clear();
		notifyDataSetChanged();
	}
	
	public boolean append(ImageRef item) {
		for (ImageRef u : imglist)
			if (item.path.equals(u.path))
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
	
	public static class ImageRef {
		public Uri uri;
		public String mime;
		public String path;
		
		public String getMimeExt() {
			return mime != null && mime.indexOf("/") > 0 ? mime.split("/")[1] : "";
		}
		
		@Override
		public String toString() {
			StringBuilder result = new StringBuilder();
			result.append(this.getClass().getName() + " Object { ");
			result.append(" mime: \"" + mime + "\"");
			result.append(" path: \"" + path + "\"");
			result.append(" uri: " + uri.toString());
			result.append(" }");
			return result.toString();
		}
	}
	
	static class ViewHolder {
		ImageView img;
		ImageView del;
	}
}