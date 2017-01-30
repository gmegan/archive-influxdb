/*******************************************************************************
 * Copyright (c) 2011 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.archive.config.influxdb;

import org.csstudio.archive.config.EngineConfig;

/** InfluxDB implementation of EngineConfig
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class InfluxDBEngineConfig extends EngineConfig
{
    final private int id;

    /** Initialize
     *  @param id
     *  @param name
     *  @param description
     *  @param url
     *  @throws Exception if url is not a valid URL
     */
    public InfluxDBEngineConfig(final int id, final String name, final String description, final String url) throws Exception
    {
        super(name, description, url);
        this.id = id;
    }

    /** @return InfluxDB ID of engine */
    public int getId()
    {
        return id;
    }

    /** @return Debug representation */
    @Override
    public String toString()
    {
        return super.toString() + " [" + id + "]";
    }
}
