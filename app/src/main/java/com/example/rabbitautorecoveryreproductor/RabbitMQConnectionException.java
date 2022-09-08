package com.example.rabbitautorecoveryreproductor;

public class RabbitMQConnectionException extends Throwable {
    public RabbitMQConnectionException(String message, Exception e) {
        super(message,e);
    }
}
