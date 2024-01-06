package schule.ngb.carrot.protocol;

import schule.ngb.carrot.maildrop.FilesystemMaildrop;
import schule.ngb.carrot.maildrop.Maildrop;
import schule.ngb.carrot.maildrop.MaildropException;
import schule.ngb.carrot.util.Configuration;
import schule.ngb.carrot.util.Log;

import java.net.Socket;

public class POP3Factory extends GenericProtocolHandlerFactory {

	private static final Log LOG = Log.getLogger(POP3Factory.class);


	public POP3Factory( Configuration config ) {
		super(config, POP3Handler.class);
		restoreMails();
	}

	@Override
	public ProtocolHandler create( Socket clientSocket ) {
		return new POP3Handler(clientSocket, this.config);
	}

	public void restoreMails() {
		boolean restore = config.getBool("RESTORE_TRASH_ON_START", false);
		for( String user : config.getConfig("USERS").keySet() ) {
			try {
				Maildrop maildrop = new FilesystemMaildrop(user, config);
				if( maildrop.isLocked() ) {
					maildrop.unlock();
				}

				if( restore ) {
					maildrop.restoreDeleted();
				}
			} catch( MaildropException ex ) {
				LOG.error(ex, "failed to restore mails for user %s", user);
			}
		}
	}

}
