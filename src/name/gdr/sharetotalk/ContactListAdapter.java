/*
    Share to Talk, Copyright (C) 2012 GDR! <gdr@go2.pl>

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see http://www.gnu.org/licenses/.
 */

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
