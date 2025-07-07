package schule.ngb.carrot.protocol;

import org.ini4j.Ini;
import org.ini4j.Profile;
import schule.ngb.carrot.maildrop.FilesystemMaildrop;
import schule.ngb.carrot.maildrop.MailAddress;
import schule.ngb.carrot.maildrop.Maildrop;
import schule.ngb.carrot.maildrop.MaildropException;
import schule.ngb.carrot.util.Configuration;
import schule.ngb.carrot.util.Log;

import java.io.IOException;
import java.net.Socket;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class SMTPFactory extends GenericProtocolHandlerFactory {

	private static final Log LOG = Log.getLogger(SMTPFactory.class);


	public final class TransmissionQueue {

		private ExecutorService transmissions;

		private int count = 0;

		private String failureNotice = null;

		public TransmissionQueue() {
		}

		public int queueTransmission( MailAddress from, List<MailAddress> recipients, String content ) {
			if( this.transmissions == null ) {
				this.transmissions = Executors.newFixedThreadPool(2);
			}

			Future<?> ignored = this.transmissions.submit(new Runnable() {
				@Override
				public void run() {
					// Add random delay to simulate slow network
					int delay = config.get("smtp", "random_send_delay", int.class);
					LOG.debug("Message queued for transmission from mailbox %s (%d bytes)", from.getMailbox(), content.getBytes(StandardCharsets.UTF_8).length);
					if( delay > 0 ) {
						try {
							Thread.sleep(new Random().nextInt(delay));
						} catch( InterruptedException ignored ) {
						}
					}

					Profile.Section users = config.get("users");
					for( MailAddress addr : recipients ) {
						if( isLocalAddress(addr) && users.containsKey(addr.getMailbox()) ) {
							try {
								Maildrop maildrop = new FilesystemMaildrop(addr.getMailbox(), config.get("smtp", "maildrop"), config);
								maildrop.createMail(content);
								LOG.debug("Created new mail in mailbox %s (%d bytes)", addr.getMailbox(), content.getBytes(StandardCharsets.UTF_8).length);
							} catch( MaildropException e ) {
								LOG.error(e, "Failed to open mailbox for user %s", from.getMailbox());
							}
						} else if( config.get("smtp", "create_error_mails", boolean.class) && isLocalAddress(from) && users.containsKey(from.getMailbox()) ) {
							try {
								Maildrop maildrop = new FilesystemMaildrop(from.getMailbox(), config.get("smtp", "maildrop"), config);
								maildrop.createMail(
									String.format(getFailureNotice(),
										"4Sc6Cj3Nvxz9sT7.1700811207",
										config.get("host"),
										new Date(),
										from,
										addr,
										content
									)
								);
								LOG.debug("Created new failure notice in mailbox %s", from.getMailbox());
							} catch( MaildropException e ) {
								LOG.error(e, "Failed to open mailbox for user %s", from.getMailbox());
							}
						} else {
							LOG.debug("Transmission to recipient %s completed (NOOP).", addr.toString());
						}
					}
				}
			});

			count += 1;
			return count;
		}

		private String getFailureNotice() {
			if( failureNotice == null ) {
				URL url = type.getResource("smtp-delivery-status.txt");
				if( url != null ) {
					try {
						failureNotice = Files.readString(Paths.get(url.toURI()));
					} catch( IOException | URISyntaxException e ) {
						return "Failed to send mail!\n\n%2$s";
					}
				}
			}
			return failureNotice;
		}

		private boolean isLocalAddress( MailAddress mail ) {
			if( mail != null ) {
				String hostname = mail.getHostname();
				return hostname.equals("[127.0.0.1]")
					|| hostname.equalsIgnoreCase(config.get("carrot", "host"));
			} else {
				return false;
			}
		}

	}


	private TransmissionQueue transmissionQueue;

	public SMTPFactory( Ini config ) {
		super(config, SMTPHandler.class);
		transmissionQueue = new TransmissionQueue();
	}

	@Override
	protected Ini loadConfig( Ini globalConfig ) {
		Ini config = Configuration.from(globalConfig)
			.loadLeft(type.getResourceAsStream(this.getConfigFile()))
			.build();

		if( globalConfig.get("smtp", "maildrop") == null ) {
			if( globalConfig.get("pop3", "maildrop") != null ) {
				config.put("smtp", "maildrop", config.get("pop3", "maildrop"));
			}
		}

		return config;
	}

	@Override
	public ProtocolHandler create( Socket clientSocket ) {
		return new SMTPHandler(clientSocket, config, transmissionQueue);
	}

	@Override
	public int getPort() {
		return 25;
	}

}
