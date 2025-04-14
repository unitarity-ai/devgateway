package com.unitarity.apigateway.deployment;

import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

@ConfigMapping(prefix = "devgateway")
@ConfigRoot
public interface DevGatewayConfig {

    /**
     * Host entry to be created in the container to access the host machine.
     * 
     */
    @WithDefault("host-gateway")
    String devHost();
}