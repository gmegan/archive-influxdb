/*******************************************************************************
 * Copyright (c) 2011 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.archive.config.influxdb;

import java.util.HashMap;
import java.util.Map;
import org.csstudio.archive.config.ChannelConfig;
import org.csstudio.archive.config.GroupConfig;

/** InfluxDB implementation of {@link GroupConfig}
 *  @author Kay Kasemir
 */
@SuppressWarnings("nls")
public class InfluxDBGroupConfig extends GroupConfig
{
    /**
     * Unique global id for this group
     */
    final private int group_id;
    /**
     * Unique global engine id of the engine to which this group belongs
     */
    final private int engine_id;

    /**
     * Mapping of channel names to (global) channel ids
     */
    final private Map<String, Integer> channel_name2id = new HashMap<String, Integer>();
    /**
     * Mapping of (global) channel ids to channel configuration objects
     */
    final private Map<Integer, ChannelConfig> channel_id2obj = new HashMap<Integer, ChannelConfig>();

    /** Initialize
     *  @param group_id
     *  @param name
     *  @param enabling_channel
     */
    public InfluxDBGroupConfig(final int group_id, final String name, final String enabling_channel, final int engine_id)
    {
        super(name, enabling_channel);
        this.group_id = group_id;
        this.engine_id = engine_id;
    }

    /** @return InfluxDB ID of channel group */
    public int getGroupId()
    {
        return group_id;
    }

    /** @return InfluxDB ID of channel engine */
    public int getEngineId()
    {
        return engine_id;
    }

    /**
     * @return list of configured channels in this group
     */
    public ChannelConfig[] getChannelArray()
    {
        return channel_id2obj.values().toArray(new ChannelConfig[channel_id2obj.size()]);
    }

    public boolean containsChannel(final String channel_name)
    {
        return channel_name2id.containsKey(channel_name);
    }

    /** @param channel Channel that enables this group */
    void setEnablingChannel(final InfluxDBChannelConfig channel) throws Exception
    {
        if (channel.getGroupId() != group_id)
            throw new Exception("Tried to set enabling channel to config with group id " + channel.getGroupId() + " != " + group_id);
        enabling_channel = channel.getName();
    }

    public InfluxDBChannelConfig addChannel(final int channel_id, final String channel_name, final InfluxDBSampleMode mode) throws Exception
    {
        if (channel_name2id.containsKey(channel_name))
            throw new Exception("Cannot re-add extant channel " + channel_name + " to engine " + getName());

        channel_name2id.put(channel_name, channel_id);
        InfluxDBChannelConfig channel = new InfluxDBChannelConfig(channel_id, channel_name, mode, null, group_id);
        channel_id2obj.put(channel_id, channel);
        return channel;
    }

    /** @return Debug representation */
    @Override
    public String toString()
    {
        return super.toString() + " (" + group_id + ")";
    }
}
