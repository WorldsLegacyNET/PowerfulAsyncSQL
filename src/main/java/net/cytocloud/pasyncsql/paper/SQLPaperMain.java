package net.cytocloud.pasyncsql.paper;

import net.cytocloud.pasyncsql.lib.worker.SQLWorker;
import org.bukkit.plugin.java.JavaPlugin;

public class SQLPaperMain extends JavaPlugin {

    @Override
    public void onEnable() {
        SQLWorker.startThread();
    }

    @Override
    public void onDisable() {
        SQLWorker.stopThread();
    }

}
