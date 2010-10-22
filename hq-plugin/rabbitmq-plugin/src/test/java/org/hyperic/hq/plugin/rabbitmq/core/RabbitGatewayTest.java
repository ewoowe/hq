/**
 * NOTE: This copyright does *not* cover user programs that use Hyperic
 * program services by normal system calls through the application
 * program interfaces provided as part of the Hyperic Plug-in Development
 * Kit or the Hyperic Client Development Kit - this is merely considered
 * normal use of the program, and does *not* fall under the heading of
 * "derived work".
 *
 *  Copyright (C) [2010], VMware, Inc.
 *  This file is part of Hyperic.
 *
 *  Hyperic is free software; you can redistribute it and/or modify
 *  it under the terms version 2 of the GNU General Public License as
 *  published by the Free Software Foundation. This program is distributed
 *  in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 *  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 *  PARTICULAR PURPOSE. See the GNU General Public License for more
 *  details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 *  USA.
 *
 */
package org.hyperic.hq.plugin.rabbitmq.core;


import org.hyperic.hq.plugin.rabbitmq.AbstractSpringTest;
import org.hyperic.hq.plugin.rabbitmq.core.HypericChannel;
import org.springframework.amqp.core.Queue;
import org.hyperic.hq.plugin.rabbitmq.core.AMQPStatus;
import org.hyperic.hq.plugin.rabbitmq.core.HypericConnection;
import org.hyperic.hq.product.PluginException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
 
import org.springframework.amqp.rabbit.admin.QueueInfo;
import org.springframework.amqp.rabbit.admin.RabbitStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map; 

/**
 * RabbitGatewayTest
 * @author Helena Edelson
 */
@Ignore("Need to mock the connection for automation")
public class RabbitGatewayTest extends AbstractSpringTest {

    @Before 
    public void before() { 
        RabbitStatus s = rabbitBrokerAdmin.getStatus();
        assertNotNull(s);
    }


    @Test
    public void getQueues() throws PluginException { 
        List<QueueInfo> queues = rabbitGateway.getQueues("/");
        assertNotNull(queues);
    }

    @Test
    public void getConnections() throws Exception {
        com.rabbitmq.client.Connection conn = singleConnectionFactory.createConnection();
        List<String> vHosts = rabbitGateway.getVirtualHosts();
        for (String vhost : vHosts) {
            List<HypericConnection> cons = rabbitGateway.getConnections(vhost);
            assertTrue(cons.size() > 0);
        }

        conn.close();
    }

    @Test
    public void getChannels() throws Exception {
        com.rabbitmq.client.Connection conn = singleConnectionFactory.createConnection();
        conn.createChannel();
        conn.createChannel();
        
        List<String> vHosts = rabbitGateway.getVirtualHosts();
        for (String vhost : vHosts) {
            List<HypericChannel> channels = rabbitGateway.getChannels(vhost);
            assertTrue(channels.size() > 0);
        }
        conn.close();
    }

    @Test
    public void getVirtualHosts() throws Exception {
        List<String> virtualHosts = rabbitGateway.getVirtualHosts();
        assertNotNull(virtualHosts);
        assertTrue(virtualHosts.size() >= 0);
    }

    @Test
    @Ignore("this is hanging..have to fix")
    public void purgeQueue() {
        List<QueueInfo> queues = rabbitGateway.getQueues("/");
        assertNotNull(queues);
    }

    @Test
    public void getHosts() throws PluginException {
        String host = rabbitTemplate.getConnectionFactory().getHost();
        String virtualHost = rabbitTemplate.getConnectionFactory().getVirtualHost();
        assertNotNull(host);
        assertNotNull(virtualHost);
    }

    /*@Test
    public void declareDeleteExchange() {
        rabbitGateway.createExchange("34566", ExchangeTypes.FANOUT);
        rabbitGateway.deleteExchange("34566");
    }*/

    @Test
    public void listCreateDeletePurgeQueue() {
        AMQPStatus status = rabbitManager.createQueue(new Queue("test"), "/");
        assertTrue(status.compareTo(AMQPStatus.RESOURCE_CREATED) == 0);
        assertTrue(status.name().equalsIgnoreCase(AMQPStatus.RESOURCE_CREATED.name()));

        List<QueueInfo> queues = rabbitGateway.getQueues("/");
        assertNotNull(queues);

        Map<String, QueueInfo> map = new HashMap<String, QueueInfo>();
        for (QueueInfo qi : queues) {
            map.put(qi.getName(), qi);
        }
        assertTrue(map.containsKey("test"));
        /** hangs forever */
        //rabbitGateway.purgeQueue(queueName);

        //rabbitGateway.deleteQueue(queueName);
    }

    @Test
    public void stopStartBrokerApplication() {
        RabbitStatus status = rabbitGateway.getRabbitStatus();
        assertBrokerAppRunning(status);

        rabbitManager.stopBrokerApplication();
        status = rabbitGateway.getRabbitStatus();
        assertEquals(0, status.getRunningNodes().size());

        rabbitManager.startBrokerApplication();
        status = rabbitGateway.getRabbitStatus();
        assertBrokerAppRunning(status);
    }

    @Test
    public void listCreateDeleteChangePwdUser() {
        List<String> users = rabbitGateway.getUsers("/");
        if (users.contains("foo")) {
            rabbitManager.deleteUser("foo", "/");
        }
        rabbitManager.createUser("foo", "bar", "/");
        rabbitManager.updateUserPassword("foo", "12345", "/");
        users = rabbitGateway.getUsers("/");
        if (users.contains("foo")) {
            rabbitManager.deleteUser("foo", "/");
        }
    }

    @Test
    @Ignore("NEEDS RABBITMQ_HOME to be set and needs additional node running handling/timing")
    public void startStopRabbitNode() {
        //rabbitGateway.stopRabbitNode();
        rabbitManager.startRabbitNode();
    }

    private boolean isBrokerAppRunning(RabbitStatus status) {
        return status.getRunningNodes().size() == 1;
    }

    private void assertBrokerAppRunning(RabbitStatus status) {
        assertEquals(1, status.getRunningNodes().size());
        assertTrue(status.getRunningNodes().get(0).getName().contains("rabbit"));
    }


    /*@Test
    public void testPublish()  {
        ConnectionFactoryStub factory = new ConnectionFactoryStub();
        Connection connection = createMock(Connection.class);
        factory.setConnection(connection);
        Channel channel = createMock(Channel.class);
        expect(connection.createChannel()).andReturn(channel);
        // record some more expected calls
        replay(connection, channel);
        // create your object under test and inject the ConnectionFactoryStub
        // invoke the method that you are testing
        verify(connection, channel);
        // make some assertions
    }

    private static class ConnectionFactoryStub extends ConnectionFactory {
        Connection connection;
        public void setConnection(Connection connection) {
            this.connection = connection;
        }
        @Override
        public Connection newConnection() {
            return this.connection;
        }
    }*/
}