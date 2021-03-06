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
package org.neo4j.driver.v1.util;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;

import org.neo4j.driver.v1.Config;

import static java.lang.Thread.sleep;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

public class StubServer
{
    private static final int SOCKET_CONNECT_ATTEMPTS = 20;

    public static final Config INSECURE_CONFIG = Config.build()
            .withoutEncryption().toConfig();

    // This may be thrown if the driver has not been closed properly
    public static class ForceKilled extends Exception {}

    private static final String BOLT_STUB_COMMAND = "boltstub";

    private Process process = null;

    private StubServer( String script, int port ) throws IOException, InterruptedException
    {
        List<String> command = new ArrayList<>();
        command.addAll( singletonList( BOLT_STUB_COMMAND ) );
        command.addAll( asList( Integer.toString( port ), script ) );
        ProcessBuilder server = new ProcessBuilder().inheritIO().command( command );
        process = server.start();
        waitForSocket( port );
    }

    public static StubServer start( String resource, int port ) throws IOException, InterruptedException
    {
        assumeTrue( boltKitAvailable() );
        return new StubServer( resource(resource), port );
    }

    public int exitStatus() throws InterruptedException, ForceKilled
    {
        sleep( 500 );  // wait for a moment to allow disconnection to occur
        try
        {
            return process.exitValue();
        }
        catch ( IllegalThreadStateException ex )
        {
            // not exited yet
            exit();
            throw new ForceKilled();
        }
    }

    private void exit() throws InterruptedException
    {
        process.destroy();
        process.waitFor();
    }

    private static String resource( String fileName )
    {
        File resource = new File( TestNeo4j.TEST_RESOURCE_FOLDER_PATH, fileName );
        if ( !resource.exists() )
        {
            fail( fileName + " does not exists" );
        }
        return resource.getAbsolutePath();
    }

    private static boolean boltKitAvailable()
    {
        try
        {
            // run 'help' command to see if boltstub is available
            Process process = new ProcessBuilder( BOLT_STUB_COMMAND, "-h" ).start();
            int exitCode = process.waitFor();
            return exitCode == 0;
        }
        catch ( IOException | InterruptedException e )
        {
            // unable to run boltstub command, thus it is unavailable
            return false;
        }
    }

    private static void waitForSocket( int port ) throws InterruptedException
    {
        SocketAddress address = new InetSocketAddress( "localhost", port );
        for ( int i = 0; i < SOCKET_CONNECT_ATTEMPTS; i++ )
        {
            try
            {
                SocketChannel.open( address );
                return;
            }
            catch ( Exception e )
            {
                sleep( 300 );
            }
        }
        throw new AssertionError( "Can't connect to " + address );
    }
}
