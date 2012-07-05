/**
 * Copyright 2012 Lennart Koopmann <lennart@socketfeed.com>
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package org.graylog2.inputs.syslog;

import java.net.InetSocketAddress;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.log4j.Logger;
import org.graylog2.Configuration;
import org.graylog2.GraylogServer;
import org.graylog2.inputs.MessageInput;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelException;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class SyslogTCPInput implements MessageInput {
    
    private static final Logger LOG = Logger.getLogger(SyslogTCPInput.class);

    private static final String NAME = "Syslog TCP";

    private GraylogServer graylogServer;
    private InetSocketAddress socketAddress;
    
    @Override
    public void initialize(Configuration configuration, GraylogServer graylogServer) {
        this.graylogServer = graylogServer;
        this.socketAddress = new InetSocketAddress(
                configuration.getSyslogListenAddress(),
                configuration.getSyslogListenPort()
        );

        spinUp();
    }
    
    private void spinUp() {
        final ExecutorService bossThreadPool = Executors.newCachedThreadPool();
        final ExecutorService workerThreadPool = Executors.newCachedThreadPool();

        ServerBootstrap tcpBootstrap = new ServerBootstrap(
            new NioServerSocketChannelFactory(bossThreadPool, workerThreadPool)
        );

        tcpBootstrap.setPipelineFactory(new SyslogTCPPipelineFactory(this.graylogServer));

        try {
            tcpBootstrap.bind(socketAddress);
            LOG.info("Started TCP syslog server on " + socketAddress);
        } catch (ChannelException e) {
            LOG.fatal("Could not bind TCP syslog server to address " + socketAddress, e);
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

}
