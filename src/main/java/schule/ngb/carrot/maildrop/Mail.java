package schule.ngb.carrot.maildrop;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

public final class Mail {

	String id;

	int number;

	Path file;

	boolean deleted = false;

	private String hash;

	public Mail( int number, Path file ) {
		this.number = number;
		this.file = file;
		this.id = file.getFileName().toString();
	}

	public String getId() {
		return id;
	}

	public int getNumber() {
		return number;
	}

	public Path getFile() {
		return file;
	}

	public boolean fileExists() {
		return Files.exists(this.file);
	}

	public String getHash() throws IOException {
		if( hash == null ) {
			try {
				MessageDigest md5 = MessageDigest.getInstance("md5");
				// TODO: need to consider charset here?
				md5.update(StandardCharsets.UTF_8.encode(getText()));
				hash = String.format("%032x", new BigInteger(1, md5.digest()));
			} catch( NoSuchAlgorithmException e ) {
				hash = id;
			}
		}
		return hash;
	}

	public String getText() throws IOException {
		return Files.readString(this.file, StandardCharsets.UTF_8);
	}

	public List<String> getLines() throws IOException {
		return Files.readAllLines(this.file, StandardCharsets.UTF_8);
	}

	public boolean isDeleted() {
		return deleted;
	}

	public long getSize() throws IOException {
		return Files.size(file);
	}

	public long getSizeOrZero() {
		try {
			return Files.size(file);
		} catch( IOException ex ) {
			return 0;
		}
	}

}
