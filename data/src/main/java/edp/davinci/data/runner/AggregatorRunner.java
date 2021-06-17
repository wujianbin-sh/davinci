package edp.davinci.data.runner;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import edp.davinci.core.dao.entity.Source;
import edp.davinci.core.dao.entity.User;
import edp.davinci.data.aggregator.AggregatorFactory;
import edp.davinci.data.aggregator.JdbcAggregator;
import edp.davinci.data.provider.DataProviderFactory;

@Order(2)
@Component
public class AggregatorRunner implements ApplicationRunner {

    @Value("${aggregator.enable:true}")
    private boolean enable;

    private final static ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    
    @Override
    public void run(ApplicationArguments args) throws Exception {

        if (!enable) {
            return;
        }

        JdbcAggregator aggregator = (JdbcAggregator) AggregatorFactory.getAggregator("jdbc");
        Source source = aggregator.getSource();

        // create system user
        User user = new User();
        user.setId(0L);

        DataProviderFactory.getProvider(source.getType()).execute(source,
                "create table if not exists " + JdbcAggregator.DATA_TABLE_NAME
                        + " (table_name varchar(255) primary key, ttl int not null, create_time datetime not null);", user);
        
        executor.scheduleWithFixedDelay(() -> {
            aggregator.cleanData();
        }, 1, 1, TimeUnit.MINUTES);
    }
    
}