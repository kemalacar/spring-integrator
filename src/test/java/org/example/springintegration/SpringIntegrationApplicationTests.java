package org.example.springintegration;

import lombok.SneakyThrows;
import org.apache.sshd.client.SshClient;
import org.apache.sshd.client.keyverifier.AcceptAllServerKeyVerifier;
import org.apache.sshd.client.keyverifier.ServerKeyVerifier;
import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.client.impl.DefaultSftpClientFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

@SpringBootTest
public class SpringIntegrationApplicationTests {
	static final String REMOTE_DIRECTORY = "home";
	static final String USERNAME = "user";
	static final String PASSWORD = "pass";
	static final String HOST = "localhost";
	static final int PORT = 22;
	static BaseSftpTestContainer sftp;
	static SshClient client;
	static ClientSession session;

	@TempDir
	static Path tempDir;
	static Path tempFile;

	private SshServer sshd;


	@SneakyThrows
	@BeforeAll
	public static void start() {
		sftp = new BaseSftpTestContainer();
		sftp.start();
		client = SshClient.setUpDefaultClient();
		client.start();
		ServerKeyVerifier serverKeyVerifier = AcceptAllServerKeyVerifier.INSTANCE;
		client.setServerKeyVerifier(serverKeyVerifier);
		session = client.connect(USERNAME, HOST, PORT)
				.verify(Duration.ofSeconds(10))
				.getSession();
		session.addPasswordIdentity(PASSWORD);
		session.auth().verify(Duration.ofSeconds(30));
	}

	@SneakyThrows
	@Test
	void applicationContextIsUpAndRunning() {
		uploadVaryingSizeFiles();
	}

	private void uploadVaryingSizeFiles() throws Exception {
		Path localFilePathSmall = createTestFile(1024 * 1024);  // 1 MB
		Path localFilePathMedium = createTestFile(1024 * 1024 * 10);
		Path localFilePathLarge = createTestFile(1024 * 1024 * 100);
		DefaultSftpClientFactory defaultSftpClientFactory = new DefaultSftpClientFactory();
		Thread uploadThread = new Thread(() -> {
			try (SftpClient sftpClient = defaultSftpClientFactory.createSftpClient(session)) {
				uploadFile(sftpClient, localFilePathSmall, REMOTE_DIRECTORY);
				uploadFile(sftpClient, localFilePathMedium, REMOTE_DIRECTORY);
				uploadFile(sftpClient, localFilePathLarge, REMOTE_DIRECTORY);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		});

		uploadThread.start();
	}

	private Path createTestFile(int sizeInBytes) throws IOException {
		byte[] data = new byte[sizeInBytes];

		return Files.write(Paths.get("testfile_" + sizeInBytes + ".bin"), data);
	}

	private void uploadFile(SftpClient sftpClient, Path localFile, String remotePath) throws IOException {
		try (SftpClient.CloseableHandle handle = sftpClient.open(remotePath, SftpClient.OpenMode.Write, SftpClient.OpenMode.Create)) {
			sftpClient.write(handle, 0, Files.readAllBytes(localFile));
		}
	}

}
