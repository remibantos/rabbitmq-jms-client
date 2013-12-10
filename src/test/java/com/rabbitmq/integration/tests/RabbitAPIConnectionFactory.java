/* Copyright (c) 2013 Pivotal Software, Inc. All rights reserved. */
package com.rabbitmq.integration.tests;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;

import com.rabbitmq.jms.admin.RMQConnectionFactory;

/**
 * Connection factory for use in integration tests.
 */
public class RabbitAPIConnectionFactory extends AbstractTestConnectionFactory {

    private static final int RABBIT_PORT = 5672; // 5672 default; 5673 Tracer.
    private final boolean testssl;

    public RabbitAPIConnectionFactory() { this(false); }

    public RabbitAPIConnectionFactory(boolean testssl) { this.testssl = testssl; }
    @Override
    public ConnectionFactory getConnectionFactory() {
        return new RMQConnectionFactory() {

            private static final long serialVersionUID = 1L;

            @Override
            public Connection createConnection(String userName, String password) throws JMSException {
                super.setSsl(testssl);
                if (!testssl) this.setPort(RABBIT_PORT);
                return super.createConnection(userName, password);
            }

        };
    }

}
