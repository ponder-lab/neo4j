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
package org.neo4j.test.format;

import static org.neo4j.configuration.GraphDatabaseInternalSettings.include_versions_under_development;
import static org.neo4j.configuration.GraphDatabaseSettings.db_format;
import static org.neo4j.configuration.SettingValueParsers.TRUE;

import java.util.Map;
import org.neo4j.annotations.service.ServiceProvider;
import org.neo4j.configuration.SettingMigrator;
import org.neo4j.logging.InternalLog;

@ServiceProvider
public class FormatOverrideMigrator implements SettingMigrator {
    private static final String OVERRIDE_STORE_FORMAT_KEY = "NEO4J_OVERRIDE_STORE_FORMAT";

    @Override
    public void migrate(Map<String, String> values, Map<String, String> defaultValues, InternalLog log) {
        String overrideStoreFormat = System.getProperty(OVERRIDE_STORE_FORMAT_KEY);
        if (overrideStoreFormat != null && !values.containsKey(db_format.name())) {
            values.put(db_format.name(), overrideStoreFormat);
            values.put(include_versions_under_development.name(), TRUE);
        }
    }
}