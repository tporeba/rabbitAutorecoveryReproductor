/*
 * This Java source file was generated by the Gradle 'init' task.
 */
package com.example.rabbitautorecoveryreproductor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeoutException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.io.ByteSource;
import com.google.common.io.Resources;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Consumer;

public class RabbitAutorecoveryReproductorMain {

    private static final Logger log = LogManager.getLogger();
    private static final int PREFETCH_ONE_MESSAGE = 1;

    public static void main(String[] args) throws RabbitMQConnectionException, IOException {

        Properties p = loadProperties("rabbit.properties");

        final String username = p.getProperty("rabbit.username");
        final String password = p.getProperty("rabbit.password");
        final String hosts = p.getProperty("rabbit.hosts");
        final String consumerTag = p.getProperty("rabbit.consumerTag");
        final String queueName = p.getProperty("rabbit.queueName");

        log.info("Connecting as username={}, password=****** to hosts={}", username, hosts);

        AmqpConnectionFactory factory = new AmqpConnectionFactory(username, password, hosts);
        try (Connection connection = factory.createNewConnection()) {
            Channel channel = null;
            try {
                channel = connection.createChannel();
                declareQueue(queueName, channel);
                Consumer newConsumer = createConsumer(channel);
                channel.basicConsume(queueName, false, consumerTag, newConsumer);
                log.info("Started consumer on AMQP {} ", queueName);

                System.out.print("Hit enter to finish the consumer.");
                new BufferedReader(new InputStreamReader(System.in)).readLine();

                log.info("Closing");

            } catch (IOException e) {
                if (channel != null) {
                    try {
                        channel.close();
                    } catch (IOException | TimeoutException ex) {
                        log.warn("Failed to close AMQP channel");
                    }
                }
            }
        }

    }

    private static Consumer createConsumer(Channel channel) {
        return new AmqpConsumer(channel);
    }

    private static void declareQueue(String queueName, Channel channel) throws IOException {
        channel.basicQos(PREFETCH_ONE_MESSAGE);
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("x-message-ttl", 600000);
        parameters.put("x-max-length", 500000);
        parameters.put("x-expires", 259200000);
        channel.queueDeclare(queueName, false, false, false, parameters);
    }

    public static Properties loadProperties(String filename) {
        URL url = Resources.getResource(filename);
        final Properties properties = new Properties();

        final ByteSource byteSource = Resources.asByteSource(url);
        try (InputStream inputStream = byteSource.openBufferedStream()) {
            properties.load(inputStream);
            return properties;
        } catch (IOException e) {
            log.error("openBufferedStream failed!", e);
        }
        return properties;
    }
}
