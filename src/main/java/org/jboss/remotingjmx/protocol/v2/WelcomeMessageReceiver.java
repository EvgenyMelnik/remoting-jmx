/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.remotingjmx.protocol.v2;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.Arrays;

import org.jboss.logging.Logger;
import org.jboss.remoting3.Channel;
import org.jboss.remoting3.MessageInputStream;
import org.xnio.IoFuture;
import org.xnio.IoUtils;

/**
 * The WelcomeMessageReceiver Receiver used by the client.
 *
 * This is a special receiver as unlike the other messages blocking on the result is desired to ensure the connection is
 * established before any client interactions occur.
 *
 * Unlike version one of the protocol this version does not receive any data from the server, just the welcome message to
 * confirm the server is ready to handle requests.
 *
 * @author <a href="mailto:darran.lofthouse@jboss.com">Darran Lofthouse</a>
 */
class WelcomeMessageReceiver implements Channel.Receiver {

    private static final Logger log = Logger.getLogger(WelcomeMessageReceiver.class);

    private final VersionedIoFuture<Void> future;

    private WelcomeMessageReceiver(VersionedIoFuture<Void> future) {
        this.future = future;
    }

    public static IoFuture<Void> awaitWelcomeMessage(final Channel channel) {
        VersionedIoFuture<Void> future = new VersionedIoFuture<Void>();

        channel.receiveMessage(new WelcomeMessageReceiver(future));
        return future;
    }

    public void handleMessage(Channel channel, MessageInputStream messageInputStream) {
        DataInputStream dis = new DataInputStream(messageInputStream);
        try {
            log.tracef("Bytes Available %d", dis.available());
            byte[] firstThree = new byte[3];
            dis.read(firstThree);
            log.tracef("First Three %s", new String(firstThree));
            if (Arrays.equals(firstThree, "JMX".getBytes()) == false) {
                throw new IOException("Invalid leading bytes in header.");
            }

            future.setResult(null);
        } catch (IOException e) {
            future.setException(e);
        } finally {
            IoUtils.safeClose(dis);
        }

    }

    public void handleError(Channel channel, IOException e) {
        future.setException(e);
    }

    public void handleEnd(Channel channel) {
        future.setException(new IOException("Channel ended"));
    }

}