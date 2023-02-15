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
package org.neo4j.bolt.test.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.neo4j.bolt.test.extension.dependency.CommunityEditionStateMachineDependencyProvider;
import org.neo4j.bolt.testing.annotation.fsm.StateMachineTestExtension;
import org.neo4j.bolt.transport.Neo4jWithSocketExtension;
import org.neo4j.test.extension.testdirectory.EphemeralTestDirectoryExtension;

@Documented
@EphemeralTestDirectoryExtension
@Neo4jWithSocketExtension
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE, ElementType.TYPE, ElementType.METHOD})
@StateMachineTestExtension(dependencyProvider = CommunityEditionStateMachineDependencyProvider.class)
public @interface CommunityStateMachineTestExtension {}