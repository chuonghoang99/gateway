package com.apus.gateway.configuration.caching;

import com.apus.gateway.configuration.GatewayProperties;
import com.hazelcast.config.*;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.spring.cache.HazelcastCacheManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.KeyGenerator;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.client.serviceregistry.Registration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import javax.annotation.PreDestroy;

@Slf4j
@Configuration
@EnableCaching
public class HazelcastConfiguration {
  private Registration registration;
  private Environment env;
  private ServerProperties serverProperties;
  private DiscoveryClient discoveryClient;

  @Autowired
  public void setEnv(Environment env) {
    this.env = env;
  }

  @Autowired
  public void setServerProperties(ServerProperties serverProperties) {
    this.serverProperties = serverProperties;
  }

  @Autowired
  public void setDiscoveryClient(DiscoveryClient discoveryClient) {
    this.discoveryClient = discoveryClient;
  }

  @Autowired(required = false)
  public void setRegistration(@Qualifier("eurekaRegistration") Registration registration) {
    this.registration = registration;
  }

  @PreDestroy
  public void destroy() {
    log.info("Closing Cache Manager!");
    Hazelcast.shutdownAll();
  }

  @Bean(name = "myHazelcast")
  public HazelcastInstance hazelcastInstance(GatewayProperties gatewayProperties) {
    log.info("Configuring Hazelcast!");
    String appName = env.getProperty("spring.application.name");
    HazelcastInstance hazelCastInstance = Hazelcast.getHazelcastInstanceByName(appName);
    if (hazelCastInstance != null) {
      log.info("Hazelcast already initialized!");
      return hazelCastInstance;
    }

    int port = gatewayProperties.getCache().getHazelcast().getPort();
    Config config = initConfig(appName, gatewayProperties);

    if (this.registration == null) {
      log.warn("No discovery service is set up, Hazelcast cannot create a cluster!");
    } else {
      // The serviceId is by default the application's name,
      // see the "spring.application.name" standard Spring property
      String serviceId = registration.getServiceId();
      log.info("Configuring Hazelcast clustering for instanceId: {}", serviceId);
      // In development, everything goes through 127.0.0.1, with a different port
      if (env.acceptsProfiles(Profiles.of("dev"))) {
        log.info(
            "Application is running with the \"dev\" profile, Hazelcast "
                + "cluster will only work with localhost instances."
        );

        config.getNetworkConfig().setPort(serverProperties.getPort() + port);
        config.getNetworkConfig().getJoin().getTcpIpConfig()
            .setEnabled(gatewayProperties.getCache().getHazelcast().isTcpIp());
        for (ServiceInstance instance : discoveryClient.getInstances(serviceId)) {
          String clusterMember = "127.0.0.1:" + (instance.getPort() + port);
          log.info("Adding Hazelcast (dev) cluster member {}", clusterMember);
          config.getNetworkConfig().getJoin().getTcpIpConfig().addMember(clusterMember);
        }
      } else { // Production configuration, one host per instance all using port 5701 by default
        config.getNetworkConfig().setPort(port);
        config.getNetworkConfig().getJoin().getTcpIpConfig()
            .setEnabled(gatewayProperties.getCache().getHazelcast().isTcpIp());
        config.setClusterName("prod");
        for (ServiceInstance instance : discoveryClient.getInstances(serviceId)) {
          String clusterMember = instance.getHost() + ":" + port;
          log.info("Adding Hazelcast (prod) cluster member {}", clusterMember);
          config.getNetworkConfig().getJoin().getTcpIpConfig().addMember(clusterMember);
        }
      }
    }
    return Hazelcast.newHazelcastInstance(config);
  }

  public Config initConfig(String appName, GatewayProperties gatewayProperties) {
    Config config = new Config();
    config.setInstanceName(appName);
    // Disable multicast-mode for adding member to cluster by service discovery
    config.getNetworkConfig().getJoin().getMulticastConfig()
        .setEnabled(gatewayProperties.getCache().getHazelcast().isMulticast());
    // CP subsystem with at least 3 members, turn off by set member-count = 0
    config.getCPSubsystemConfig().setCPMemberCount(gatewayProperties.getCache().getHazelcast().getCpCountMember());
    // Management Center
    config.setManagementCenterConfig(new ManagementCenterConfig());
    // Init MapConfig
    config.addMapConfig(initDefaultMapConfig(gatewayProperties));
    return config;
  }

  private MapConfig initDefaultMapConfig(GatewayProperties gatewayProperties) {
    MapConfig mapConfig = new MapConfig("default");

    /*
    Number of backups. If 1 is set as the backup-count for example,
    then all entries of the map will be copied to another JVM for
    fail-safety. Valid numbers are 0 (no backup), 1, 2, 3.
    */
    mapConfig.setBackupCount(gatewayProperties.getCache().getHazelcast().getBackupCount());

    /*
    Valid values are:
    NONE (no eviction),
    LRU (Least Recently Used),
    LFU (Least Frequently Used).
    NONE is the default.
    */
    mapConfig.getEvictionConfig().setEvictionPolicy(EvictionPolicy.LRU);

    /*
    Maximum size of the map. When max size is reached,
    map is evicted based on the policy defined.
    Any integer between 0 and Integer.MAX_VALUE. 0 means
    Integer.MAX_VALUE. Default is 0.
    */
    mapConfig.getEvictionConfig().setMaxSizePolicy(MaxSizePolicy.USED_HEAP_SIZE);

    /*
    Maximum time in seconds for each entry to stay in the map (TTL).
    Set value = 0 for infinite.
     */
    mapConfig.setTimeToLiveSeconds(gatewayProperties.getCache().getHazelcast().getTimeToLiveSeconds());

    /*
    Maximum time in seconds for each entry to stay idle in the map.
    Set value = 0 for infinite.
     */
    mapConfig.setMaxIdleSeconds(gatewayProperties.getCache().getHazelcast().getMaxIdleSeconds());

    return mapConfig;
  }

  @Bean
  public KeyGenerator keyGenerator() {
    return new SimpleKeyGenerator();
  }

  @Bean
  @Primary
  public CacheManager cacheManager(@Qualifier("myHazelcast") HazelcastInstance hazelcastInstance) {
    log.debug("Starting HazelcastCacheManager!");
    return new HazelcastCacheManager(hazelcastInstance);
  }
}
