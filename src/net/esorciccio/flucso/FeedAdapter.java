package net.esorciccio.flucso;

import net.esorciccio.flucso.Commons.PK;
import net.esorciccio.flucso.FFAPI.Entry;
import net.esorciccio.flucso.FFAPI.Entry.Comment;
import net.esorciccio.flucso.FFAPI.Feed;
import android.content.Context;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class FeedAdapter extends BaseAdapter {

	private Context context;
	private FFSession session;
	private OnClickListener listener;
	private LayoutInflater inflater;
	
	public Feed feed;
	
	public FeedAdapter(Context context, OnClickListener clickListener) {
		super();
		
		this.context = context;
		session = FFSession.getInstance(context);
		listener = clickListener;
		inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}
	
	@Override
	public int getCount() {
		return feed == null ? 0 : feed.entries.size();
	}
	
	@Override
	public Entry getItem(int position) {
		return feed.entries.get(position);
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
			view = inflater.inflate(R.layout.item_feed_entry, parent, false);
			vh = new ViewHolder();
			vh.lNormal = (LinearLayout) view.findViewById(R.id.l_feed_entry_visible);
			vh.lHidden = (LinearLayout) view.findViewById(R.id.l_feed_entry_hidden);
			vh.txtFromH = (TextView) view.findViewById(R.id.txt_feed_hidden_from);
			vh.txtTimeH = (TextView) view.findViewById(R.id.txt_feed_hidden_time);
			vh.imgFrom = (ImageView) view.findViewById(R.id.img_entry_from);
			vh.imgFrom.setOnClickListener(listener);
			vh.txtFrom = (TextView) view.findViewById(R.id.txt_entry_from);
			vh.txtTo = (TextView) view.findViewById(R.id.txt_entry_to);
			vh.txtTime = (TextView) view.findViewById(R.id.txt_entry_time);
			vh.txtBody = (TextView) view.findViewById(R.id.txt_feed_body);
			vh.imgThumb = (ImageView) view.findViewById(R.id.img_feed_thumb);
			vh.imgThumb.setOnClickListener(listener);
			vh.lComm = (LinearLayout) view.findViewById(R.id.l_feed_lc);
			vh.imgLC = (ImageView) view.findViewById(R.id.img_feed_lc);
			vh.txtLC = (TextView) view.findViewById(R.id.txt_feed_lc);
			vh.txtLikes = (TextView) view.findViewById(R.id.txt_feed_likes);
			vh.txtLikes.setOnClickListener(listener);
			vh.txtFiles = (TextView) view.findViewById(R.id.txt_feed_files);
			vh.txtFiles.setOnClickListener(listener);
			vh.txtFrwd = (TextView) view.findViewById(R.id.txt_feed_fwd);
			vh.txtFrwd.setOnClickListener(listener);
			vh.txtHide = (TextView) view.findViewById(R.id.txt_feed_hide);
			vh.txtHide.setOnClickListener(listener);
			vh.lFoF = (LinearLayout) view.findViewById(R.id.l_feed_fof);
			vh.imgFoF1 = (ImageView) view.findViewById(R.id.img_feed_fof1);
			vh.imgFoF2 = (ImageView) view.findViewById(R.id.img_feed_fof2);
			vh.imgIsDM = (ImageView) view.findViewById(R.id.img_entry_dm);
			vh.txtComms = (TextView) view.findViewById(R.id.txt_feed_comms);
			view.setTag(vh);
		} else {
			vh = (ViewHolder) view.getTag();
		}
		
		vh.imgFrom.setTag(Integer.valueOf(position));
		vh.imgThumb.setTag(Integer.valueOf(position));
		vh.txtLikes.setTag(Integer.valueOf(position));
		vh.txtFiles.setTag(Integer.valueOf(position));
		vh.txtFrwd.setTag(Integer.valueOf(position));
		vh.txtHide.setTag(Integer.valueOf(position));
		
		String tmp;
		final Entry entry = getItem(position);
		
		if (entry.hidden || entry.banned) {
			vh.lNormal.setVisibility(View.GONE);
			vh.lHidden.setVisibility(View.VISIBLE);
			vh.txtFromH.setCompoundDrawablesWithIntrinsicBounds(entry.hidden ? R.drawable.ic_action_desktop :
				R.drawable.ic_action_phone, 0, 0, 0);
			vh.txtFromH.setText(entry.from.getName());
			vh.txtTimeH.setText(entry.getFuzzyTime());
			return view;
		} else {
			vh.lNormal.setVisibility(View.VISIBLE);
			vh.lHidden.setVisibility(View.GONE);
		}
		
		Commons.picasso(context).load(entry.from.getAvatarUrl()).placeholder(
			R.drawable.nomugshot).into(vh.imgFrom);
		
		vh.txtFrom.setText(entry.from.getName());
		vh.txtFrom.setCompoundDrawablesWithIntrinsicBounds(entry.from.locked ? R.drawable.entry_private : 0, 0, 0, 0);
		
		vh.imgIsDM.setVisibility(entry.isDM() ? View.VISIBLE : View.GONE);
		
		tmp = entry.getToLine();
		if (tmp == null) {
			vh.txtTo.setVisibility(View.GONE);
		} else {
			vh.txtTo.setText(tmp);
			vh.txtTo.setVisibility(View.VISIBLE);
		}
		
		String tl = entry.getFuzzyTime();
		if (entry.via != null && !TextUtils.isEmpty(entry.via.name.trim()))
			tl += new StringBuilder().append(" ").append(context.getString(R.string.source_prefix)).append(" ").append(
				entry.via.name.trim()).toString();
		vh.txtTime.setText(tl);
		
		vh.txtHide.setVisibility(entry.canHide() ? View.VISIBLE : View.GONE);
		vh.txtBody.setText(Html.fromHtml(entry.body));

		if (entry.thumbnails.length <= 0)
			vh.imgThumb.setVisibility(View.GONE);
		else {
			vh.imgThumb.setVisibility(View.VISIBLE);
			Commons.picasso(context).load(entry.thumbnails[0].url).placeholder(R.drawable.ic_action_picture).into(vh.imgThumb);
		}
		
		if (entry.files.length > 0 || (entry.thumbnails.length + entry.files.length) > 1) {
			vh.txtFiles.setVisibility(View.VISIBLE);
			vh.txtFiles.setText(Integer.toString(entry.thumbnails.length + entry.files.length));
		} else
			vh.txtFiles.setVisibility(View.GONE);
		
		if (!session.getPrefs().getBoolean(PK.FEED_ELC, true) || entry.comments.size() <= 0) {
			vh.lComm.setVisibility(View.GONE);
			vh.lNormal.setBackground(view.getResources().getDrawable(R.drawable.feed_item_box_nc));
		} else {
			vh.lComm.setVisibility(View.VISIBLE);
			vh.lNormal.setBackground(view.getResources().getDrawable(R.drawable.feed_item_box_lc));
			Comment c = entry.comments.get(entry.comments.size() - 1);
			if (c.placeholder)
				c = entry.comments.get(entry.comments.size() - 2);
			Commons.picasso(context).load(c.from.getAvatarUrl()).placeholder(R.drawable.nomugshot).into(vh.imgLC);
			vh.txtLC.setText(Html.fromHtml(c.body));
		}
		
		vh.txtLikes.setCompoundDrawablesWithIntrinsicBounds(entry.canUnlike() ? R.drawable.entry_liked :
			R.drawable.entry_like, 0, 0, 0);
		
		int n = entry.getLikesCount();
		if (n <= 0) {
			vh.txtLikes.setCompoundDrawablePadding(0);
			vh.txtLikes.setText("");
		} else {
			vh.txtLikes.setCompoundDrawablePadding(14);
			vh.txtLikes.setText(Integer.toString(n));
		}
		
		n = entry.getCommentsCount();
		if (n <= 0) {
			vh.txtComms.setCompoundDrawablePadding(0);
			vh.txtComms.setText("");
		} else {
			vh.txtComms.setCompoundDrawablePadding(14);
			vh.txtComms.setText(Integer.toString(n));
		}
		
		String[] fofs = entry.getFofIDs();
		if (fofs == null) {
			vh.lFoF.setVisibility(View.GONE);
			vh.imgFoF1.setVisibility(View.GONE);
			vh.imgFoF2.setVisibility(View.GONE);
		} else {
			vh.lFoF.setVisibility(View.VISIBLE);
			vh.imgFoF1.setVisibility(View.VISIBLE);
			Commons.picasso(context).load("http://friendfeed-api.com/v2/picture/" + fofs[0] + "?size=large").placeholder(
				R.drawable.nomugshot).into(vh.imgFoF1);
			if (fofs.length == 1)
				vh.imgFoF2.setVisibility(View.GONE);
			else {
				vh.imgFoF2.setVisibility(View.VISIBLE);
				Commons.picasso(context).load("http://friendfeed-api.com/v2/picture/" + fofs[1] + "?size=large").placeholder(
					R.drawable.nomugshot).into(vh.imgFoF2);
			}
		}
		
		return view;
	}
	
	public static class ViewHolder {
		public LinearLayout lNormal;
		public LinearLayout lHidden;
		public TextView txtFromH;
		public TextView txtTimeH;
		public ImageView imgFrom;
		public TextView txtFrom;
		public TextView txtTo;
		public TextView txtTime;
		public TextView txtHide;
		public TextView txtBody;
		public ImageView imgThumb;
		public LinearLayout lComm;
		public ImageView imgLC;
		public TextView txtLC;
		public TextView txtLikes;
		public TextView txtFiles;
		public TextView txtFrwd;
		public LinearLayout lFoF;
		public ImageView imgFoF1;
		public ImageView imgFoF2;
		public ImageView imgIsDM;
		public TextView txtComms;
	}
}