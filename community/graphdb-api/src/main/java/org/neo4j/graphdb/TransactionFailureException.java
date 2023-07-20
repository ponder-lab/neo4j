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
package org.neo4j.graphdb;

import org.neo4j.annotations.api.PublicApi;
import org.neo4j.kernel.api.exceptions.Status;

/**
 * Signals that a transaction failed and has been rolled back.
 */
@PublicApi
public class TransactionFailureException extends RuntimeException implements Status.HasStatus {

    public final Status status;

    @Deprecated
    public TransactionFailureException(String msg) {
        super(msg);
        this.status = Status.Database.Unknown;
    }

    @Deprecated
    public TransactionFailureException(String msg, Throwable cause) {
        super(msg, cause);
        this.status = (cause instanceof Status.HasStatus se) ? se.status() : Status.Database.Unknown;
    }

    public TransactionFailureException(String msg, Status status) {
        super(msg);
        this.status = status;
    }

    public TransactionFailureException(String msg, Throwable cause, Status status) {
        super(msg, cause);
        this.status = status;
    }

    @Override
    public Status status() {
        return status;
    }
}
