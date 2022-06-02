/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.bolt.protocol.common.handler.messages;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.neo4j.logging.LogAssertions.assertThat;

import io.netty.channel.embedded.EmbeddedChannel;
import org.junit.jupiter.api.Test;
import org.neo4j.bolt.protocol.common.connection.BoltConnection;
import org.neo4j.bolt.protocol.v40.messaging.request.CommitMessage;
import org.neo4j.bolt.protocol.v40.messaging.request.ResetMessage;
import org.neo4j.logging.AssertableLogProvider;
import org.neo4j.logging.AssertableLogProvider.Level;

class ResetMessageHandlerTest {

    @Test
    void shouldInterruptStateMachine() {
        var connection = mock(BoltConnection.class);
        var log = new AssertableLogProvider();

        var channel = new EmbeddedChannel(new ResetMessageHandler(connection, log.getLog(ResetMessageHandler.class)));

        channel.writeInbound(ResetMessage.INSTANCE);

        verify(connection).interrupt();
        verifyNoMoreInteractions(connection);

        assertThat(log).forLevel(Level.DEBUG).containsMessages("Interrupted state machine");
    }

    @Test
    void shouldIgnoreUnrelatedMessages() {
        var connection = mock(BoltConnection.class);
        var log = new AssertableLogProvider();

        var channel = new EmbeddedChannel(new ResetMessageHandler(connection, log.getLog(ResetMessageHandler.class)));

        channel.writeInbound(CommitMessage.INSTANCE);

        verifyNoInteractions(connection);

        assertThat(log).doesNotHaveAnyLogs();
    }
}