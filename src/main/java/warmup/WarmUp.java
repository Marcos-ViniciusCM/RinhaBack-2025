package warmup;

import com.rinhaQuarkus.cache.CacheController;
import com.rinhaQuarkus.jdbc.api.DataService;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.sql.SQLException;

//@Singleton
//public class WarmUp {
//
//    @Inject
//    DataService service;
//
//    @Inject
//    CacheController cacheController;
//
//    @PostConstruct
//    void onStart() throws SQLException {
//        service.warmUp();
//       // cacheController.updateCache();
//    }
//}
