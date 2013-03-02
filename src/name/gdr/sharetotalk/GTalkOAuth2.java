package name.gdr.sharetotalk;

import java.io.IOException;
import java.net.URLEncoder;

import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.sasl.SASLMechanism;

import android.util.Log;

public class GTalkOAuth2 extends SASLMechanism {
public static final String NAME="X-GOOGLE-TOKEN";


public GTalkOAuth2(SASLAuthentication saslAuthentication) {
    super(saslAuthentication);
}

@Override
protected String getName() {
    return NAME;
}

static void enable() { }

@Override
protected void authenticate() throws IOException, XMPPException
{
    String authCode = password;
    String jidAndToken = "\0" + URLEncoder.encode( authenticationId, "utf-8" ) + "\0" + authCode;

    StringBuilder stanza = new StringBuilder();
    stanza.append( "<auth mechanism=\"" ).append( getName() );
    stanza.append( "\" xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\">" );
    stanza.append( Base64.encodeBytes( jidAndToken.getBytes( "UTF-8" ) ) );

    stanza.append( "</auth>" );

    Log.v("BlueTalk", "Authentication text is "+stanza);
    // Send the authentication to the server
    getSASLAuthentication().send( new Auth2Mechanism(stanza.toString()) );
}

public class Auth2Mechanism extends Packet {
    String stanza;
    public Auth2Mechanism(String txt) {
        stanza = txt;
    }
    public String toXML() {
        return stanza;
    }
}

/**
 * Initiating SASL authentication by select a mechanism.
 */
public class AuthMechanism extends Packet {
    final private String name;
    final private String authenticationText;

    public AuthMechanism(String name, String authenticationText) {
        if (name == null) {
            throw new NullPointerException("SASL mechanism name shouldn't be null.");
        }
        this.name = name;
        this.authenticationText = authenticationText;
    }

    public String toXML() {
        StringBuilder stanza = new StringBuilder();
        stanza.append("<auth mechanism=\"").append(name);
        stanza.append("\" xmlns=\"urn:ietf:params:xml:ns:xmpp-sasl\">");
        if (authenticationText != null &&
                authenticationText.trim().length() > 0) {
            stanza.append(authenticationText);
        }
        stanza.append("</auth>");
        return stanza.toString();
    }
    }
}