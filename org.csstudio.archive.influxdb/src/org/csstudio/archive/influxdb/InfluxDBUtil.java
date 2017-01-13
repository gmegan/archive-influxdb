package org.csstudio.archive.influxdb;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.time.Instant;
import java.util.List;
import java.util.logging.Level;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;

public class InfluxDBUtil
{

    public static BigInteger toNano(Instant time)
    {
        BigInteger ret = BigInteger.valueOf(time.getEpochSecond());
        ret.multiply(BigInteger.valueOf(1000000000));
        ret.add(BigInteger.valueOf(time.getNano()));
        return ret;
    }

    public static long toNanoLong(Instant time)
    {
        final BigInteger ret = toNano(time);
        try
        {
            return ret.longValueExact();
        }
        catch (Exception e)
        {
            Activator.getLogger().log(Level.WARNING, "Could not convert instant to long time stamp!", e);
        }
        return ret.longValue();
    }

    public static String getDataDBName(final String channel_name)
    {
        return InfluxDBArchivePreferences.DBNAME;
    }

    public static String getMetaDBName(final String channel_name)
    {
        return InfluxDBArchivePreferences.METADBNAME;
    }

    public static InfluxDB connect(final String url, final String user, final String password) throws Exception
    {
        Activator.getLogger().log(Level.FINE, "Connecting to {0}", url);
        InfluxDB influxdb;
        if (user == null || password == null)
        {
            influxdb = InfluxDBFactory.connect(url);
        }
        else {
            influxdb = InfluxDBFactory.connect(url, user, password);
        }

        try
        {
            // Have to do something like this because connect fails silently.
            influxdb.version();
        }
        catch (Exception e)
        {
            throw new Exception("Failed to connect to InfluxDB as user " + user + " at " + url, e);
        }
        return influxdb;
    }

    public static class ConnectionInfo
    {
        final public String version;
        final public List<String> dbs;
        final public InfluxDB influxdb;

        public ConnectionInfo(InfluxDB influxdb) throws Exception
        {
            this.influxdb = influxdb;
            try
            {
                version = influxdb.version();
                dbs = influxdb.describeDatabases();
            }
            catch (Exception e)
            {
                throw new Exception("Failed to get info for connection. Maybe disconnected?", e);
            }
        }

        @Override
        public String toString()
        {
            return "InfluxDB connection version " + version + " [" + dbs.size() + " databases]";
        }

    };

    public static byte[] toByteArray(double value) {
        byte[] bytes = new byte[8];
        ByteBuffer.wrap(bytes).putDouble(value);
        return bytes;
    }

    public static double toDouble(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getDouble();
    }

    public static String bytesToHex(byte[] bytes)
    {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

}
