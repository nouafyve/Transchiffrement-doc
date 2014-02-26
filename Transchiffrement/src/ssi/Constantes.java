package ssi;

public interface Constantes {
	public static final String PROJECT_FOLDER = System.getProperty("user.dir") + "/";
	public static final String KEYSTORE_FILE =  PROJECT_FOLDER + "keystores/keystore_ok.jks";
	public static final String KEYSTORE_PATH = PROJECT_FOLDER + "keystores/";
	public static final String KEYSTORE_PASSWORD = "000000";
//	public static final String TRUSTSTORE_FILE = "/usr/lib/jvm/java-7-openjdk-amd64/jre/lib/security/cacerts";
//	public static final String TRUSTSTORE_PASSWORD = "changeit";
	public static final int PROXY_PORT = 9999;
	public static final int BUFFER_SIZE = 4096;
}
