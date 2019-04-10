/*
 * Copyright (c) 2002-2019 "Neo4j,"
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
package org.neo4j.test;

import java.io.File;
import java.util.function.Function;

import org.neo4j.configuration.Config;
import org.neo4j.configuration.GraphDatabaseSettings;
import org.neo4j.configuration.Settings;
import org.neo4j.graphdb.facade.ExternalDependencies;
import org.neo4j.graphdb.facade.GraphDatabaseFacadeFactory;
import org.neo4j.graphdb.factory.module.GlobalModule;
import org.neo4j.graphdb.factory.module.edition.AbstractEditionModule;
import org.neo4j.graphdb.factory.module.edition.CommunityEditionModule;
import org.neo4j.graphdb.mockfs.EphemeralFileSystemAbstraction;
import org.neo4j.io.fs.FileSystemAbstraction;
import org.neo4j.kernel.impl.factory.DatabaseInfo;
import org.neo4j.kernel.internal.locker.StoreLocker;
import org.neo4j.logging.LogProvider;
import org.neo4j.logging.NullLogProvider;
import org.neo4j.logging.internal.LogService;
import org.neo4j.logging.internal.SimpleLogService;
import org.neo4j.time.SystemNanoClock;

public class TestGraphDatabaseFacadeFactory extends GraphDatabaseFacadeFactory
{
    private final TestGraphDatabaseFactoryState state;
    private final boolean impermanent;

    protected TestGraphDatabaseFacadeFactory( TestGraphDatabaseFactoryState state, boolean impermanent )
    {
        this( state, impermanent, DatabaseInfo.COMMUNITY, CommunityEditionModule::new );
    }

    public TestGraphDatabaseFacadeFactory( TestGraphDatabaseFactoryState state, boolean impermanent,
            DatabaseInfo databaseInfo, Function<GlobalModule,AbstractEditionModule> editionFactory )
    {
        super( databaseInfo, editionFactory );
        this.state = state;
        this.impermanent = impermanent;
    }

    TestGraphDatabaseFacadeFactory( TestGraphDatabaseFactoryState state )
    {
        this( state, false );
    }

    @Override
    protected GlobalModule createGlobalModule( File storeDir, Config config, ExternalDependencies dependencies )
    {
        if ( !config.isConfigured( GraphDatabaseSettings.shutdown_transaction_end_timeout ) )
        {
            config.augment( GraphDatabaseSettings.shutdown_transaction_end_timeout, "0s" );
        }
        config.augment( GraphDatabaseSettings.ephemeral, impermanent ? Settings.TRUE : Settings.FALSE );
        if ( impermanent )
        {
            return new ImpermanentTestDatabaseGlobalModule( storeDir, config, dependencies, this.databaseInfo );
        }
        else
        {
            return new TestDatabaseGlobalModule( storeDir, config, dependencies, this.databaseInfo );
        }
    }

    class TestDatabaseGlobalModule extends GlobalModule
    {

        TestDatabaseGlobalModule( File storeDir, Config config, ExternalDependencies dependencies, DatabaseInfo databaseInfo )
        {
            super( storeDir, config, databaseInfo, dependencies );
        }

        @Override
        protected FileSystemAbstraction createFileSystemAbstraction()
        {
            FileSystemAbstraction fs = state.getFileSystem();
            if ( fs != null )
            {
                return fs;
            }
            else
            {
                return createNewFileSystem();
            }
        }

        protected FileSystemAbstraction createNewFileSystem()
        {
            return super.createFileSystemAbstraction();
        }

        @Override
        protected LogService createLogService( LogProvider userLogProvider )
        {
            LogProvider internalLogProvider = state.getInternalLogProvider();
            if ( internalLogProvider == null )
            {
                if ( !impermanent )
                {
                    return super.createLogService( userLogProvider );
                }
                internalLogProvider = NullLogProvider.getInstance();
            }
            return new SimpleLogService( userLogProvider, internalLogProvider );
        }

        @Override
        protected SystemNanoClock createClock()
        {
            SystemNanoClock clock = state.clock();
            return clock != null ? clock : super.createClock();
        }
    }

    private class ImpermanentTestDatabaseGlobalModule extends TestDatabaseGlobalModule
    {

        ImpermanentTestDatabaseGlobalModule( File storeDir, Config config, ExternalDependencies dependencies, DatabaseInfo databaseInfo )
        {
            super( storeDir, config, dependencies, databaseInfo );
        }

        @Override
        protected FileSystemAbstraction createNewFileSystem()
        {
            return new EphemeralFileSystemAbstraction();
        }

        @Override
        protected StoreLocker createStoreLocker()
        {
            return new StoreLocker( getFileSystem(), getStoreLayout() );
        }
    }
}
