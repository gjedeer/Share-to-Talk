package name.gdr.sharetotalk;

import java.util.ArrayList;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class ContactListAdapter extends ArrayAdapter<ContactItem> {
    private ArrayList<ContactItem> items;
    private Context context;

    public ContactListAdapter(Context context, int textViewResourceId, ArrayList<ContactItem> items) {
        super(context, textViewResourceId, items);
        this.context = context;
        this.items = items;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.contact_row, null);
        }

        ContactItem item = items.get(position);
        if (item!= null) {
            // My layout has only one TextView
            TextView itemView = (TextView) view.findViewById(R.id.toptext);
            if (itemView != null) {
                // do whatever you want with your string and long
                itemView.setText(item.getName());
            }
            TextView itemView2 = (TextView) view.findViewById(R.id.bottomtext);
            if (itemView != null) {
                // do whatever you want with your string and long
                itemView2.setText(item.jid);
            }

         }

        return view;
    }
}
