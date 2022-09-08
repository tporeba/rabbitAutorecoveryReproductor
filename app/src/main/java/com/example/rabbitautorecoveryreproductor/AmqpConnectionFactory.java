package com.example.rabbitautorecoveryreproductor;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.google.common.base.Splitter;
import com.rabbitmq.client.Address;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class AmqpConnectionFactory {

    private static final Logger log = LogManager.getLogger();
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
}
