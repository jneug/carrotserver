package schule.ngb.carrot.maildrop;

import schule.ngb.carrot.util.Configuration;
import schule.ngb.carrot.util.Log;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class FilesystemMaildrop implements Maildrop {

	public static final class Factory implements MaildropFactory {

		private final Configuration config;

		public Factory( Configuration config ) {
			this.config = config;
		}

		@Override
		public Maildrop create( String username ) throws MaildropException {
			return new FilesystemMaildrop(username, this.config);
		}

	}


	public static final String LOCKFILE = ".lock";


	private static final Log LOG = Log.getLogger(FilesystemMaildrop.class);


	private Configuration config;

	private Path root;

	private HashMap<Path, Mail> mails;

	public FilesystemMaildrop( String username, Configuration config ) throws MaildropException {
		this.config = config;

		root = Paths.get(
			this.config.getString("DATA"),
			this.config.getString("MAILDROP", "maildrop"),
			username
		);
		if( !Files.exists(root) ) {
			try {
				Files.createDirectories(root);
			} catch( IOException e ) {
				throw new MaildropException(e);
			}
		}

		mails = new HashMap<>();

		updateFileList();
	}

	public void updateFileList() throws MaildropException {
		List<Path> mailList = null;
		try {
			mailList = Files.list(root)
				.filter(
					( m ) -> m.getFileName().toString().endsWith(".eml")
				)
				.sorted().collect(Collectors.toList());
		} catch( IOException e ) {
			throw new MaildropException(e);
		}
		for( Path p : mailList ) {
			if( Files.isReadable(p) && !Files.isDirectory(p) && !mails.containsKey(p) ) {
				mails.put(p,
					new Mail(mails.size() + 1, p)
				);
			}
		}
	}

	public Stream<Mail> getMailStream() {
		return getMailStream(false);
	}

	public Stream<Mail> getMailStream( boolean includeDeleted ) {
		return mails.values().stream()
			.filter(( m ) -> !m.deleted || m.deleted == includeDeleted)
			.sorted(( m1, m2 ) -> m1.number - m2.number);
	}

	@Override
	public Mail getMail( int number ) throws MaildropException {
		return getMailStream().filter(( m ) -> m.number == number).findFirst().orElse(null);
	}

	@Override
	public List<Mail> listMails() throws MaildropException {
		updateFileList();
		return getMailStream().collect(Collectors.toList());
	}

	@Override
	public List<Mail> listAllMails() throws MaildropException {
		updateFileList();
		return getMailStream(true).collect(Collectors.toList());
	}

	@Override
	public long countAll() throws MaildropException {
		return mails.size();
	}

	@Override
	public long count() throws MaildropException {
		return getMailStream().count();
	}

	@Override
	public long size() throws MaildropException {
		return getMailStream().mapToLong(Mail::getSizeOrZero).sum();
	}

	@Override
	public long size( int number ) throws MaildropException {
		Mail mail = getMail(number);
		if( mail != null ) {
			try {
				return mail.getSize();
			} catch( IOException e ) {
				throw new MaildropException(e);
			}
		} else {
			throw new MaildropException("no file for number " + number);
		}
	}

	@Override
	public String getText( int number ) throws MaildropException {
		Mail mail = getMail(number);
		if( mail != null ) {
			try {
				return Files.readString(mail.file);
			} catch( IOException e ) {
				throw new MaildropException(e);
			}
		} else {
			throw new MaildropException("no file for number " + number);
		}
	}

	@Override
	public List<String> getLines( int number ) throws MaildropException {
		Mail mail = getMail(number);
		if( mail != null ) {
			try {
				return Files.readAllLines(mail.file);
			} catch( IOException e ) {
				throw new MaildropException(e);
			}
		} else {
			throw new MaildropException("no file for number " + number);
		}
	}

	@Override
	public void deleteFile( int number ) throws MaildropException {
		Mail mail = getMail(number);
		if( mail != null ) {
			mail.deleted = true;
		} else {
			throw new MaildropException("no file for number " + number);
		}
	}

	@Override
	public void resetDeleted() {
		getMailStream(true).map(( m ) -> m.deleted = false);
	}

	@Override
	public void executeDelete() throws MaildropException {
		Path trash = getTrash();

		for( Mail mail : listAllMails() ) {
			if( mail.deleted ) {
				ensureTrashExists();
				try {
					Files.move(mail.file, trash.resolve(mail.file.getFileName()), StandardCopyOption.REPLACE_EXISTING);
					LOG.error("Moved mail file %s/%s to trash", mail.file.getParent().getFileName(), mail.file.getFileName());
				} catch( IOException e ) {
					LOG.error(e, "Failed to move mail file %s/%s to trash", mail.file.getParent().getFileName(), mail.file.getFileName());
					throw new MaildropException(e);
				}
				mails.remove(mail);
			}
		}
	}

	@Override
	public void restoreDeleted() throws MaildropException {
		Path trash = getTrash();

		if( Files.isDirectory(trash) ) {
			try {
				for( Path mail : Files.list(trash).collect(Collectors.toList()) ) {
					if( mail.getFileName().toString().endsWith(".eml") ) {
						try {
							Files.move(mail, root.resolve(mail.getFileName()));
						} catch( FileAlreadyExistsException ignored ) {
						}
					} else {
						// Delete everything not a mail from trash
						Files.delete(mail);
					}
				}
			} catch( IOException e ) {
				throw new MaildropException(e);
			}

			mails.clear();
			updateFileList();
		}
	}

	public Path getRoot() {
		return root;
	}

	public Path getTrash() {
		return root.resolveSibling(config.getString("TRASH", "_trash")).resolve(root.getFileName());
	}

	public Path getLock() {
		return root.resolve(LOCKFILE);
	}

	private void ensureTrashExists() throws MaildropException {
		Path trash = getTrash();
		if( !Files.isDirectory(trash) ) {
			try {
				Files.createDirectories(trash);
			} catch( IOException e ) {
				throw new MaildropException(e);
			}
		}
	}

	@Override
	public void lock() throws MaildropException {
		Path lock = getLock();
		if( !Files.exists(lock) ) {
			try {
				Files.createFile(lock);
			} catch( IOException e ) {
				LOG.error(e, "Failed to lock maildrop %s", this.root.getFileName());
				throw new MaildropException(e);
			}
		}
	}

	@Override
	public void unlock() throws MaildropException {
		Path lock = getLock();
		if( Files.exists(lock) ) {
			try {
				Files.delete(lock);
			} catch( IOException e ) {
				LOG.error(e, "Failed to unlock maildrop %s", this.root.getFileName());
				throw new MaildropException(e);
			}
		}
	}

	@Override
	public boolean isLocked() {
		return Files.exists(getLock());
	}

	@Override
	public void createMail( String content ) throws MaildropException {
		String filename = new SimpleDateFormat("yyyyMMddHHmmss'.eml'").format(new Date());
		Path mailPath = root.resolve(filename);
		int i = 0;
		while( Files.exists(mailPath) ) {
			filename = String.format("%d" + filename, ++i);
		}

		try {
			Path tmpPath = Files.createTempFile("tmpmail", filename);
			Files.writeString(tmpPath, content, StandardCharsets.UTF_8);
			Files.move(tmpPath, mailPath);
			LOG.debug("Created mail file %s/%s", mailPath.getParent().getFileName(), mailPath.getFileName());
		} catch( IOException e ) {
			LOG.error(e, "Failed to create mail file %s/%s", mailPath.getParent().getFileName(), mailPath.getFileName());
			throw new MaildropException(e);
		}
	}

}
