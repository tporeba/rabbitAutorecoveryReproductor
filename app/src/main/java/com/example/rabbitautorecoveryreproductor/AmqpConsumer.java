package com.example.rabbitautorecoveryreproductor;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

public class AmqpConsumer extends DefaultConsumer {

    private static final Logger log = LogManager.getLogger();

    private final Channel channel;

    public AmqpConsumer(Channel channel) {
        super(channel);
        this.channel = channel;
    }

    @Override
    public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
        Long deliveryTag = null;
        boolean messageAcknowledged = false;
        try {
            deliveryTag = envelope.getDeliveryTag();
            log.debug("Message received: consumerTag=" + consumerTag);
            log.debug(" message headers: " + properties.getHeaders());
            channel.basicAck(deliveryTag, false);
            messageAcknowledged = true;
        } catch (Exception e) {
            log.error("Unrecoverable processing error caught", e);
        } finally {
            if (!messageAcknowledged && channel != null && channel.isOpen() && deliveryTag != null) {
                log.warn("Message 'deliveryTag=" + deliveryTag + "' will be permanently rejected. Probably processing error occurred.");
                try {
                    channel.basicReject(deliveryTag, false);
                } catch (IOException rejectExc) {
                    log.error("Error rejecting message 'deliveryTag=" + deliveryTag + "'", rejectExc);
                }
            }
        }
    }
}
