package org.example.springintegration;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import lombok.Getter;
import org.testcontainers.containers.GenericContainer;

@Getter
public class BaseSftpTestContainer extends GenericContainer {
    private final GenericContainer container;

    public BaseSftpTestContainer() {
        super("atmoz/sftp:latest");
        this.container = new GenericContainer<>("atmoz/sftp:latest")
                .withExposedPorts(22)
                .withCreateContainerCmdModifier(cmd -> cmd.withHostConfig(
                        new HostConfig().withPortBindings(new PortBinding(Ports.Binding.bindPort(22), new ExposedPort(22)))
                ))
                .withCommand("user:pass:::home");
    }

    public void start() {
        this.container.start();
    }

    public void stop() {
        this.container.stop();
    }

}
