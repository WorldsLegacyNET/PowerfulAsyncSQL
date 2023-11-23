package net.cytocloud.pasyncsql.lib.worker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * @param hostname The address of the server
 * @param port The port the sql is running
 * @param database The database to connect
 * @param username The username
 * @param password The password
 * @param timeout How long in seconds does the sql timeout (when no Queries are sent?)
 * @param tries How long does the async sql tries to connect to the server before it stops
 */
public record ConnectionInformation(String hostname, int port, String database, String username, String password, int timeout, int tries) {

    public static ConnectionInformation fromConfig(@NotNull String configFile) {
        File f = new File(configFile);
        Gson gson = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().create();

        if(!f.exists())
            throw new RuntimeException(new FileNotFoundException(configFile));

        try {
            FileInputStream fin = new FileInputStream(f);
            String content = new String(IOUtils.toByteArray(fin));

            JsonObject obj = gson.fromJson(content, JsonObject.class);

            if(obj == null)
                throw new NullPointerException("No json connection information available. Check json syntax in your config (\"" + configFile + "\")");

            if(!obj.has("hostname"))
                throw new NullPointerException("No connection information for \"hostname\"");

            if(!obj.has("port"))
                throw new NullPointerException("No connection information for \"port\"");

            if(!obj.has("database"))
                throw new NullPointerException("No connection information for \"database\"");

            if(!obj.has("username"))
                throw new NullPointerException("No connection information for \"username\"");

            if(!obj.has("password"))
                throw new NullPointerException("No connection information for \"password\"");

            if(!obj.has("timeout"))
                throw new NullPointerException("No connection information for \"timeout\"");

            if(!obj.has("tries"))
                throw new NullPointerException("No connection information for \"tries\"");

            final String hostname = obj.get("hostname").getAsString();
            final int port = obj.get("port").getAsInt();
            final String database = obj.get("database").getAsString();
            final String username = obj.get("username").getAsString();
            final String password = obj.get("password").getAsString();
            final int timeout = obj.get("timeout").getAsInt();
            final int tries = obj.get("tries").getAsInt();

            return new ConnectionInformation(hostname, port, database, username, password, timeout, tries);
        }catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

}