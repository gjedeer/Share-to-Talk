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
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.packet.Message;

import android.app.Activity;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Debug;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.RawContacts.Entity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class ChooseContactActivity extends ListActivity {
	protected boolean debug = false;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Context context = getApplicationContext();
        String login = this.getLogin();
        
        /* Move to Preferences activity if login has not been set */
        if(login == null) {
        	Intent prefsIntent = new Intent(getApplicationContext(), SharetoTalkActivity.class);
    		Toast.makeText(context, getString(R.string.please_enter_credentials), Toast.LENGTH_SHORT).show();
        	startActivityForResult(prefsIntent, 0);
        	return;
        }
        /* Initialization continued in onResume() */
    }
    
    @Override
    public void onResume() {
    	super.onResume();
		final Context context = getApplicationContext();
    	
    	String login = this.getLogin();
    	
    	/* No login and password entered at this point -> 
    	 * User has gone back from preferences without filling in fields
    	 */
    	if(login == null) {
    		Toast.makeText(context, getString(R.string.credentials_not_entered), Toast.LENGTH_LONG).show();
    		return;
    	}
    	
    	/* All is fine, continue to connecting and filling contact list */
    	fillContactList();
    }

    /**
     * Connects to XMPP server, fetches roster and feeds it to listview
     * Login and password preferences must be checked before calling
     */
	private void fillContactList() {
		final Context context = getApplicationContext();

		/* Connect to gtalk XMPP server */
    	ConnectionConfiguration cc = new ConnectionConfiguration("talk.google.com", 5222, "gmail.com");
    	cc.setSecurityMode(SecurityMode.required);
    	final XMPPConnection conn = new XMPPConnection(cc);
    	try {
    		conn.connect();
    		SASLAuthentication.supportSASLMechanism("PLAIN", 0);
    		conn.login(getLogin(), getPassword(), "gtalk-share");
    	} 
    	catch(XMPPException e) {
    		Toast.makeText(context, getString(R.string.gtalk_connection_failed) + "\n" + e.toString(), Toast.LENGTH_LONG).show();
    		Log.e("xmpp", e.toString());
    		return;
    	}

    	/* Fetch roster from server and copy it to a list of ContactItems */
    	final ArrayList<ContactItem> items = new ArrayList<ContactItem>();
        Roster roster = conn.getRoster();
        try {	// fix for roster not being full sometimes
        	Thread.sleep(200);
        } catch(InterruptedException e) {
        	return;
        }
        roster = conn.getRoster();
        Collection<RosterEntry> entries = roster.getEntries();
        for(RosterEntry entry : entries) {
        	ContactItem ctx = new ContactItem();
        	ctx.name = entry.getName();
        	ctx.jid = entry.getUser();
        	ctx.presence = roster.getPresence(ctx.jid);
        	
        	items.add(ctx);
        }
        
        /* this works because ContactItem implements Comparable */
        Collections.sort(items);
    	
        setListAdapter(new ContactListAdapter(this, R.layout.contact_row, items));
        
        ListView lv = getListView();
        /* TODO: add list filtering */
//        lv.setTextFilterEnabled(true);
        
        /* Send message when contact clicked */
        lv.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
            	  sendMessage(conn, items.get(position).jid);
            	  
            	  if(debug) {
	                  Toast.makeText(getApplicationContext(), items.get(position).jid,
	                      Toast.LENGTH_SHORT).show();
            	  }
                      
                }
		});
	}
    
    private void sendMessage(XMPPConnection conn, String jid) {
    	
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        String action = intent.getAction();
        
        if(debug) {	// don't spam people by accident
        	jid = "gdr@gdr.name";
        }
        
        /* Check if from share menu */
        if(Intent.ACTION_SEND.equals(action))
        {
        	final Context context = getApplicationContext();
        	
        	try {
        		/* Fetch textual data from intent */
        		String text = extras.getString(Intent.EXTRA_TEXT);
        		if(text == null) {
        			String mime = intent.getType();
        			if(mime == null) {
        				mime = "unknown";
        			}
            		Toast.makeText(context, getString(R.string.no_text) + " (" + mime + ")", Toast.LENGTH_LONG).show();
        			return;
        		}
        		
        		/* Send the message */
        		ChatManager mgr = conn.getChatManager();
        		Chat chat = mgr.createChat(jid, new MessageListener() {
        			/* Dummy message receiver */
					public void processMessage(Chat arg0, Message arg1) {
					}
        		});
        		
            	final Message msg = new Message(jid, Message.Type.chat);
            	msg.setBody(text);
            	
            	/* last chance to reconnect */
            	if(!conn.isConnected()) {
            		conn.connect();
            	}
            	chat.sendMessage(msg); // throws InvalidStateException if not connected
            	conn.disconnect();

            	/* Determine if gtalk should be opened after sending */
            	boolean open_gtalk = true;
            	
            	SharedPreferences sp=PreferenceManager.
                        getDefaultSharedPreferences(getApplicationContext());
           		open_gtalk = sp.getBoolean("open_gtalk_preference", true);
           		
           		/* Open gtalk chat to that person */
           		if(open_gtalk) {
	            	Uri imUri = new Uri.Builder().scheme("imto").authority("gtalk").appendPath(jid).build();
	            	Intent imIntent = new Intent(Intent.ACTION_SENDTO, imUri);
	            	startActivity(imIntent);
           		} else {
           			Toast.makeText(context, getString(R.string.sending_succeeded), Toast.LENGTH_SHORT).show();
           		}
        		
        	} catch(XMPPException e) {
        		Toast.makeText(context, getString(R.string.sending_failed) + ":\n" + e.toString(), Toast.LENGTH_LONG).show();
        		Log.e("xmpp", e.toString());
        	} catch(IllegalStateException e) {
        		Toast.makeText(context, getString(R.string.sending_failed) + ":\n" + e.toString(), Toast.LENGTH_LONG).show();
        		Log.e("xmpp", e.toString());
        	} catch(ActivityNotFoundException e) {
        		Toast.makeText(context, getString(R.string.could_not_open_gtalk), Toast.LENGTH_SHORT).show();
        	}
        	
        }

    }
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		/* Menu populated from XML */
		inflater.inflate(R.menu.share_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		/* TODO switch basing on item */
		/* Spawn Preferences activity */
    	Intent prefsIntent = new Intent(getApplicationContext(), SharetoTalkActivity.class);
    	startActivityForResult(prefsIntent, 0);
		return true;
	}
    
	/**
	 * Returns a normalized login, "@gmail.com" appended if necessary
	 * @return JID or null if preferences missing
	 */
    private String getLogin() {
    	SharedPreferences sp=PreferenceManager.
                getDefaultSharedPreferences(getApplicationContext());
    	if(sp.contains("login_preference")) {
    		String login = sp.getString("login_preference", null);
    		if(login == null)
    		{
    			return null;
    		}
    		if(login.contains("@")) {
    			return login;
    		}
    		else {
    			return login + "@gmail.com";
    		}
    	}
    	else
    	{
    		return null;
    	}
    }
    
    /**
     * Returns the password from preferences
     * Does not return null - let the login fail with empty password
     * @return Password or empty string if not set
     */
    private String getPassword() {
    	SharedPreferences sp=PreferenceManager.
                getDefaultSharedPreferences(getApplicationContext());

    	return sp.getString("password_preference", "");
    }
}
