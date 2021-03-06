/*
 * Copyright (c) 2002-2017 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.driver.internal.async;

import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;

import org.neo4j.driver.internal.async.inbound.ChunkDecoder;
import org.neo4j.driver.internal.async.inbound.InboundMessageHandler;
import org.neo4j.driver.internal.async.inbound.MessageDecoder;
import org.neo4j.driver.internal.async.outbound.OutboundMessageHandler;
import org.neo4j.driver.internal.messaging.MessageFormat;
import org.neo4j.driver.v1.Logging;

public final class ChannelPipelineBuilder
{
    private ChannelPipelineBuilder()
    {
    }

    public static void buildPipeline( Channel channel, MessageFormat messageFormat, Logging logging )
    {
        ChannelPipeline pipeline = channel.pipeline();

        // inbound handlers
        pipeline.addLast( new ChunkDecoder() );
        pipeline.addLast( new MessageDecoder() );
        pipeline.addLast( new InboundMessageHandler( messageFormat, logging ) );

        // outbound handlers
        pipeline.addLast( OutboundMessageHandler.NAME, new OutboundMessageHandler( messageFormat, logging ) );

        // last one - error handler
        pipeline.addLast( new ChannelErrorHandler( logging ) );
    }
}
