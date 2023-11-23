package net.cytocloud.pasyncsql.lib.api.adapter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Stores all database information as annotations
 */
public class Database {

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Connection {

        String hostname();
        int port() default 3306;
        String database();
        String username();
        String password();
        int timeout() default 300;
        int tries() default 3;

    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface ConfigConnection {

        String configFile();

    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Table {

        String id(); //the name of the table in the database

    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface Cache {

        String tableID(); //the name of the table
        int maxEntries() default 1000; // how many entries can this cache store (-1 => Infintie)
        boolean autoUpload() default true; // when the cache is full -> the value gets updated (on sql) | on false => just delete it from the cache
        boolean autoSave() default false; // Uploads all cached values and (autoEmpty) empties the cache
        boolean autoEmpty() default false; //Empties the cache after saving (needs autoSave = true)
        int saveInterval() default 30; // the interval in minutes where the whole cache gets uploaded and emptied (needs autoSave to be on)

        //TODO: can be added for future cache controls
        //boolean detectExpiration() default false; // enables that data sets can expire after a certain amount of time (


    }

}
