package net.esorciccio.flucso;

import net.esorciccio.flucso.FFAPI.Entry;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View.OnClickListener;
import android.widget.BaseAdapter;

public abstract class EntryBaseAdapter extends BaseAdapter {
	
	protected FFSession session;
	protected Entry entry;
	protected Context context;
	protected OnClickListener listener;
	protected LayoutInflater inflater;
	
	public EntryBaseAdapter(Context context, OnClickListener clickListener) {
		super();
		
		session = FFSession.getInstance(context);
		
		this.context = context;
		this.listener = clickListener;
		this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}
	
	public int setEntry(Entry value) {
		int res = getCount();
		entry = value;
		notifyDataSetChanged();
		return res - getCount();
	}
	
	abstract public int getIcon();
}