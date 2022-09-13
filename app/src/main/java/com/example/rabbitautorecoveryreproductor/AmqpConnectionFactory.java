package com.example.rabbitautorecoveryreproductor;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.google.common.base.Splitter;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Address;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.ShutdownSignalException;
import com.rabbitmq.client.impl.recovery.RecordedEntity;
import com.rabbitmq.client.impl.recovery.RetryHandler;
import com.rabbitmq.client.impl.recovery.TopologyRecoveryRetryHandlerBuilder;
import com.rabbitmq.client.impl.recovery.TopologyRecoveryRetryLogic;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AmqpConnectionFactory {

    private static final Logger log = LogManager.getLogger();
    protected static final int TOPOLOGY_RECOVER_RETRY_INITIAL_DELAY_MS = 5000;
    protected static final int TOPOLOGY_RECOVER_RETRY_ATTEMPTS = 5;

    /**
     * The original predicate from TopologyRecoveryRetryLogic.CHANNEL_CLOSED_NOT_FOUND is working ok for version 5.15.0
     * but on older versions (5.7.3 or 4.12.0) it returns false, because exception intercepted by the handler
     * is not wrapped in additional IOException.
     *
     * This predicate accepts this alternative scenario.
     */
    private static final BiPredicate<RecordedEntity, Exception> CHANNEL_CLOSED_NOT_FOUND_WITHOUT_IOEXCEPTION = (entity, ex) -> {
        if (ex instanceof ShutdownSignalException) {
            ShutdownSignalException cause = (ShutdownSignalException) ex;
            if (cause.getReason() instanceof AMQP.Channel.Close) {
                return ((AMQP.Channel.Close) cause.getReason()).getReplyCode() == 404;
            }
        }
        return false;
    };

    private final String username;
    private final String password;
    private final List<Address> hosts;

    public AmqpConnectionFactory(String username, String password, String hosts) {
        this.username = username;
        this.password = password;
        this.hosts = StreamSupport.stream(Splitter.on(",").split(hosts).spliterator(), false).map(hostPort -> {
            int sep = hostPort.indexOf(":");
            final String host = hostPort.substring(0, sep);
            final int port = Integer.parseInt(hostPort.substring(sep + 1, hostPort.length()));
            log.debug("adding address host={}, port={}", host, port);
            return new Address(host, port);
        }).collect(Collectors.toList());
    }

    public Connection createNewConnection() throws RabbitMQConnectionException {
        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setTopologyRecoveryRetryHandler(buildTopologyRecoveryRetryHandler(TOPOLOGY_RECOVER_RETRY_INITIAL_DELAY_MS, TOPOLOGY_RECOVER_RETRY_ATTEMPTS));
            factory.setThreadFactory(new ThreadFactory() {
                @Override
                public Thread newThread(Runnable r) {
                    Thread t = Executors.defaultThreadFactory().newThread(r);
                    t.setDaemon(true);
                    t.setName("rabbit-" + t.getName());
                    return t;
                }
            });
            factory.setUsername(username);
            factory.setPassword(password);
            return factory.newConnection(hosts);
        } catch (Exception e) {
            throw new RabbitMQConnectionException("Failed to start AMQP connection", e);
        }
    }

    private RetryHandler buildTopologyRecoveryRetryHandler(long initialDelayMs, int retryAttempts) {
        final TopologyRecoveryRetryHandlerBuilder builder = TopologyRecoveryRetryLogic.RETRY_ON_QUEUE_NOT_FOUND_RETRY_HANDLER;
        builder.bindingRecoveryRetryCondition(TopologyRecoveryRetryLogic.CHANNEL_CLOSED_NOT_FOUND.or(CHANNEL_CLOSED_NOT_FOUND_WITHOUT_IOEXCEPTION));
        builder.consumerRecoveryRetryCondition(TopologyRecoveryRetryLogic.CHANNEL_CLOSED_NOT_FOUND.or(CHANNEL_CLOSED_NOT_FOUND_WITHOUT_IOEXCEPTION));
        builder.backoffPolicy(new ExpBackoffPolicy(initialDelayMs, 2));
        builder.retryAttempts(retryAttempts);
        return builder.build();
    }

}
