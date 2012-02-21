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
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Context context = getApplicationContext();
        String login = getLogin();
        
        if(login == null) {
        	Intent prefsIntent = new Intent(getApplicationContext(), SharetoTalkActivity.class);
    		Toast.makeText(context, "You need to enter login and password before sharing for the first time", Toast.LENGTH_LONG).show();
        	startActivityForResult(prefsIntent, 0);
        	return;
        }
    }
    
    @Override
    public void onResume() {
    	super.onResume();
		final Context context = getApplicationContext();
    	
    	String login = getLogin();
    	if(login == null) {
    		Toast.makeText(context, "No login and password entered\nUnable to share", Toast.LENGTH_LONG).show();
    		return;
    	}
    	fillContactList();
    }

	private void fillContactList() {
		final Context context = getApplicationContext();

    	ConnectionConfiguration cc = new ConnectionConfiguration("talk.google.com", 5222, "gmail.com");
    	cc.setSecurityMode(SecurityMode.required);
    	final XMPPConnection conn = new XMPPConnection(cc);
    	try {
    		conn.connect();
    		SASLAuthentication.supportSASLMechanism("PLAIN", 0);
    		conn.login(getLogin(), getPassword(), "gtalk-share");
    	} catch(XMPPException e)
    	{
    		Toast.makeText(context, "GTalk connect failed:\n" + e.toString(), Toast.LENGTH_LONG).show();
    		Log.e("xmpp", e.toString());
    		return;
    	}

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
        
        Collections.sort(items);
    	
        setListAdapter(new ContactListAdapter(this, R.layout.contact_row, items));
        
        ListView lv = getListView();
        /* TODO: add list filtering */
//        lv.setTextFilterEnabled(true);
        
        lv.setOnItemClickListener(new OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view,
                    int position, long id) {
                  // When clicked, show a toast with the TextView text
            	  sendMessage(conn, items.get(position).jid);
/*                  Toast.makeText(getApplicationContext(), items.get(position).jid,
                      Toast.LENGTH_SHORT).show();
                      */
                }
		});
	}
    
    private void sendMessage(XMPPConnection conn, String jid) {
    	
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        String action = intent.getAction();
        
        // Check if from share menu
        if(Intent.ACTION_SEND.equals(action))
        {
        	final Context context = getApplicationContext();
        	
        	intent.getType();
        	
        	try {
        		String text = extras.getString(Intent.EXTRA_TEXT);
        		if(text == null) {
        			String mime = intent.getType();
        			if(mime == null) {
        				mime = "unknown";
        			}
            		Toast.makeText(context, "The item you shared contains no text (" + mime + ")", Toast.LENGTH_LONG).show();
        			return;
        		}
        		ChatManager mgr = conn.getChatManager();
        		Chat chat = mgr.createChat(jid, new MessageListener() {

					public void processMessage(Chat arg0, Message arg1) {
					}
        		});
        		
            	final Message msg = new Message(jid, Message.Type.chat);
            	msg.setBody(text);
            	chat.sendMessage(msg);
            	conn.disconnect();

            	boolean open_gtalk = true;
            	
            	SharedPreferences sp=PreferenceManager.
                        getDefaultSharedPreferences(getApplicationContext());
           		open_gtalk = sp.getBoolean("open_gtalk_preference", true);
           		
           		if(open_gtalk) {
	            	Uri imUri = new Uri.Builder().scheme("imto").authority("gtalk").appendPath("gdr@gdr.name").build();
	            	Intent imIntent = new Intent(Intent.ACTION_SENDTO, imUri);
	            	startActivity(imIntent);
           		}
        		
        	} catch(XMPPException e)
        	{
        		Toast.makeText(context, "Sending message failed:\n" + e.toString(), Toast.LENGTH_LONG).show();
        		Log.e("xmpp", e.toString());
        	}
        	
        }

    }
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.share_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
    	Intent prefsIntent = new Intent(getApplicationContext(), SharetoTalkActivity.class);
    	startActivityForResult(prefsIntent, 0);
		return true;
	}
    
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
    
    private String getPassword() {
    	SharedPreferences sp=PreferenceManager.
                getDefaultSharedPreferences(getApplicationContext());

    	return sp.getString("password_preference", "");
    }
}
