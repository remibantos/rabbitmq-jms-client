/* Copyright (c) 2014 Pivotal Software, Inc. All rights reserved. */
package com.rabbitmq.jms.client;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.jms.JMSException;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.GetResponse;
import com.rabbitmq.jms.admin.RMQDestination;
import com.rabbitmq.jms.parse.sql.SqlEvaluator;

class BrowsingConsumer extends DefaultConsumer {

    private final CountDownLatch latch = new CountDownLatch(1);

    private int messagesExpected;
    private final java.util.Queue<RMQMessage> msgQueue;
    private final SqlEvaluator evaluator;
    private final RMQSession session;
    private final RMQDestination dest;

    public BrowsingConsumer(Channel channel, RMQSession session, RMQDestination dest, int messagesExpected, java.util.Queue<RMQMessage> msgQueue, SqlEvaluator evaluator) {
        super(channel);
        this.messagesExpected = messagesExpected;
        this.msgQueue = msgQueue;
        this.evaluator = evaluator;
        this.session = session;
        this.dest = dest;
    }

    public boolean finishesInTime(int browsingConsumerTimeout) {
        try {
            return this.latch.await(browsingConsumerTimeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // reset interrupted status
            return false;
        }
    }

    @Override
    public void handleCancelOk(String consumerTag) {
        this.latch.countDown();
    }

    @Override
    public void handleCancel(String consumerTag) {
        this.latch.countDown();
    }

    @Override
    public void handleDelivery(String consumerTag,
                               Envelope envelope,
                               AMQP.BasicProperties properties,
                               byte[] body)
    throws IOException {
        if (this.messagesExpected==0) return;
        try {
            RMQMessage msg = RMQMessage.convertMessage(this.session, this.dest, new GetResponse(envelope, properties, body, --this.messagesExpected));
            if (evaluator==null || evaluator.evaluate(msg.toHeaders()))
                this.msgQueue.add(msg);
        } catch (JMSException e) {
            throw new IOException("Failure to convert message to JMS Message type.", e);
        }
        if (this.messagesExpected == 0) {
            this.getChannel().basicCancel(consumerTag);
        }
    }
}