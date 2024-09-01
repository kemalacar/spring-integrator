package org.example.springintegration;

import org.apache.sshd.sftp.client.SftpClient;
import org.example.springintegration.BaseSftpTestContainer;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.Pollers;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.sftp.dsl.Sftp;
import org.springframework.integration.sftp.session.DefaultSftpSessionFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.concurrent.Executors;

@SpringBootApplication
public class SpringIntegrationApplication {

    static final String USERNAME = "user";
    static final String PASSWORD = "pass";
    static final String ROOT_DIRECTORY = "home";

    private static Path createTestFile(int sizeInBytes) throws IOException {
        byte[] data = new byte[sizeInBytes];

        return Files.write(Paths.get("testfile_" + sizeInBytes + ".bin"), data);
    }


    public static void main(String[] args) {

//        try {
//            createTestFile(2000000000);
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }

        BaseSftpTestContainer sftp = new BaseSftpTestContainer();
        sftp.start();
        System.out.println("main thread : " + Thread.currentThread().threadId());
        new SpringApplicationBuilder(SpringIntegrationApplication.class)
                .web(WebApplicationType.NONE)
                .run(args);
    }

    @Bean
    public IntegrationFlow sftpInboundFlow() {

        return IntegrationFlow
                .from(Sftp.inboundAdapter(this.sftpSessionFactory())
                                .maxFetchSize(-1)
                                .deleteRemoteFiles(false)
                                .preserveTimestamp(true)
                                .remoteDirectory(ROOT_DIRECTORY)
                                .regexFilter(".*\\.pdf$")
                                .localFilenameExpression("#this.toUpperCase() + '.a'")
                                .localDirectory(new File("sftp-inbound")),
                        e -> e.id("sftpInboundAdapter")
                                .autoStartup(true)
                                .poller(
//                                        Pollers.fixedDelay(1000).maxMessagesPerPoll(10)
                                        Pollers.fixedDelay(500).maxMessagesPerPoll(10).taskExecutor(Executors.newFixedThreadPool(100))
                                ))
                .handle(this::handle)
                .get();
    }


    private void handle(Message<?> message) {
        System.out.println(LocalDateTime.now() + "\t\t" + message.getPayload() + " \t\t " + Thread.currentThread().threadId());
    }


    @Bean
    public SessionFactory<SftpClient.DirEntry> sftpSessionFactory() {
        DefaultSftpSessionFactory factory = new DefaultSftpSessionFactory(true);
        factory.setHost("localhost");
        factory.setPort(22);
        factory.setUser(USERNAME);
        factory.setPassword(PASSWORD);
        factory.setAllowUnknownKeys(true);
        return new CachingSessionFactory<>(factory);
    }
}

