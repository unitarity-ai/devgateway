package com.unitarity.apigateway.deployment;

import com.github.dockerjava.api.model.HostConfig;
import io.quarkus.deployment.IsNormal;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.DevServicesResultBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.util.List;
import java.util.Map;

import static java.util.Map.entry;

class ApigatewayProcessor {

    private static final String FEATURE = "apigateway";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep(onlyIfNot = IsNormal.class)
    public DevServicesResultBuildItem createKongContainer() {
        DockerImageName dockerImageName = DockerImageName.parse("kong");
        var env = Map.ofEntries(
                entry("KONG_DATABASE", "off"),
                entry("KONG_DECLARATIVE_CONFIG", "/kong/kong.yml")
        );

        String readyRegex = ".*ready to accept connections.*";

        GenericContainer<?> container = new GenericContainer<>(dockerImageName)
                .withExposedPorts(8000, 8001, 8002, 8443, 8444, 8445)
                .withEnv(env)
                .withCopyFileToContainer(
                        MountableFile.forClasspathResource("kong.yml"),
                        "/kong/kong.yml"
                )
                .waitingFor(Wait.forLogMessage(readyRegex, 1))
                .withReuse(true);
        container.setPortBindings(List.of("8000:8000", "8001:8001", "8002:8002", "8443:8443", "8444:8444", "8445:8445"));

        container.start();

        String newUrl = "http://%s:%d".formatted(container.getHost(),
                container.getMappedPort(8000));
        Map<String, String> configOverrides = Map.of("kong-service.base-url", newUrl);

        return new DevServicesResultBuildItem.RunningDevService(FEATURE,
                container.getContainerId(),
                container::close,
                configOverrides).toBuildItem();
    }

    @BuildStep(onlyIfNot = IsNormal.class)
    public DevServicesResultBuildItem createNginxContainer() {
        DockerImageName dockerImageName = DockerImageName.parse("nginx");

        String readyRegex = ".*start worker process.*";

        GenericContainer<?> container = new GenericContainer<>(dockerImageName)
                .withExposedPorts(80)
                .withCopyFileToContainer(
                        MountableFile.forClasspathResource("nginx.conf"),
                        "/etc/nginx/conf.d/default.conf"
                )
                .withExtraHost("host.docker.internal", "host-gateway")
                .waitingFor(Wait.forLogMessage(readyRegex, 1))
                .withReuse(true);
        container.setPortBindings(List.of("8090:80"));

        container.start();

        String newUrl = "http://%s:%d".formatted(container.getHost(),
                container.getMappedPort(80));
        Map<String, String> configOverrides = Map.of("nginx-service.base-url", newUrl);

        return new DevServicesResultBuildItem.RunningDevService(FEATURE,
                container.getContainerId(),
                container::close,
                configOverrides).toBuildItem();
    }
}
