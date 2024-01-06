package schule.ngb.carrot.maildrop;

public class MailAddress {

	private final String mailbox;

	private final String hostname;

	private final MailAddress[] path;

	public static MailAddress parseString( String mailPath ) {
		int openAngle = mailPath.indexOf('<');
		if( openAngle >= 0 ) {
			mailPath = mailPath.substring(openAngle + 1);
		}
		int closeAngle = mailPath.lastIndexOf('>');
		if( closeAngle >= 0 ) {
			mailPath = mailPath.substring(0, closeAngle);
		}

		String name = mailPath;
		String host = "";
		String[] path = null;

		int indexOfColon = mailPath.lastIndexOf(':');
		if( indexOfColon >= 0 ) {
			path = mailPath.substring(0, indexOfColon).split(",");
			name = mailPath.substring(indexOfColon + 1);
		}

		int indexOfAt = name.lastIndexOf('@');
		if( indexOfAt >= 0 ) {
			host = name.substring(indexOfAt + 1);
			name = name.substring(0, indexOfAt);
		}

		return new MailAddress(name, host, path);
	}

	public MailAddress( String mailbox ) {
		this.mailbox = mailbox;
		this.hostname = null;
		this.path = null;
	}

	public MailAddress( String mailbox, String hostname ) {
		this.mailbox = mailbox;
		this.hostname = hostname;
		this.path = null;
	}

	public MailAddress( String mailbox, String hostname, MailAddress[] path ) {
		this.mailbox = mailbox;
		this.hostname = hostname;
		this.path = path;
	}

	public MailAddress( String mailbox, String hostname, String[] path ) {
		this.mailbox = mailbox;
		this.hostname = hostname;

		if( path != null ) {
			this.path = new MailAddress[path.length];
			for( int i = 0; i < path.length; i++ ) {
				this.path[i] = MailAddress.parseString(path[i]);
			}
		} else {
			this.path = null;
		}
	}

	public String getName() {
		return mailbox;
	}

	public String getMailbox() {
		return mailbox;
	}

	public String getHostname() {
		return hostname;
	}

	public MailAddress[] getPath() {
		return path;
	}

	public boolean isFullyQualified() {
		return mailbox != null && hostname != null;
	}

	public String toQualifiedString() {
		if( path == null || path.length == 0 ) {
			return '<' + toString() + '>';
		} else {
			StringBuilder sb = new StringBuilder();
			sb.append('<');

			boolean first = true;
			for( MailAddress m: path ) {
				if( first ) {
					first = false;
				} else {
					sb.append(',');
				}
				sb.append(m.toString());
			}

			sb.append(':');
			sb.append(toString());

			sb.append('>');

			return sb.toString();
		}
	}

	@Override
	public String toString() {
		if( mailbox != null && hostname != null ) {
			return mailbox + "@" + hostname;
		} else if( hostname != null ) {
			return "@" + hostname;
		} else {
			return mailbox;
		}
	}

}
