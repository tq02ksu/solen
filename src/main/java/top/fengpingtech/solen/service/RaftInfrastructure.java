package top.fengpingtech.solen.service;

import com.alipay.sofa.jraft.RouteTable;
import com.alipay.sofa.jraft.rhea.client.DefaultRheaKVStore;
import com.alipay.sofa.jraft.rhea.client.RheaKVStore;
import com.alipay.sofa.jraft.rhea.options.PlacementDriverOptions;
import com.alipay.sofa.jraft.rhea.options.RheaKVStoreOptions;
import com.alipay.sofa.jraft.rhea.options.StoreEngineOptions;
import com.alipay.sofa.jraft.rhea.options.configured.PlacementDriverOptionsConfigured;
import com.alipay.sofa.jraft.rhea.options.configured.RheaKVStoreOptionsConfigured;
import com.alipay.sofa.jraft.rhea.options.configured.RocksDBOptionsConfigured;
import com.alipay.sofa.jraft.rhea.options.configured.StoreEngineOptionsConfigured;
import com.alipay.sofa.jraft.rhea.storage.StorageType;
import com.alipay.sofa.jraft.util.Endpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Service
public class RaftInfrastructure {
    private static final Logger logger = LoggerFactory.getLogger(RaftInfrastructure.class);

    private RheaKVStore rheaKVStore;

    private final RaftProperties raftProperties;

    public RaftInfrastructure(RaftProperties raftProperties) {
        this.raftProperties = raftProperties;
    }

    @PostConstruct
    public void init() {
        PlacementDriverOptions pdOpts = PlacementDriverOptionsConfigured.newConfigured()
                .withFake(true)
                .config();
        String dbPath = raftProperties.getDataPath() + "/" + raftProperties.getClusterName();
        StoreEngineOptions storeOpts = StoreEngineOptionsConfigured.newConfigured()
                .withStorageType(StorageType.RocksDB)
                .withRocksDBOptions(
                        RocksDBOptionsConfigured.newConfigured()
                                .withDbPath(dbPath).config())
                .withRaftDataPath(raftProperties.getDataPath())
                .withServerAddress(new Endpoint("127.0.0.1", raftProperties.getPort()))
                .config();
        RheaKVStoreOptions opts = RheaKVStoreOptionsConfigured.newConfigured()
                .withClusterName(raftProperties.getClusterName())
                .withInitialServerList(raftProperties.getAllNodeAddresses())
                .withStoreEngineOptions(storeOpts)
                .withPlacementDriverOptions(pdOpts)
                .config();
        RouteTable.getInstance().updateConfiguration(raftProperties.getClusterName() + "--1", opts.getInitialServerList());

        logger.info("Initializing Raft Cluster {} with Options = {}", raftProperties.getClusterName(), opts);
        rheaKVStore = new DefaultRheaKVStore();
        rheaKVStore.init(opts);
    }

    @PreDestroy
    public void destroy() {
        rheaKVStore.shutdown();
    }

    @Bean
    public RheaKVStore getRheaKVStore() {
        return rheaKVStore;
    }
}
