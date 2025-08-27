package eu.haruka.wrongturn;

import eu.haruka.wrongturn.objects.TurnConfig;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.Properties;

public class Main implements TurnLogBack {

    public static void main(String[] args) throws Exception {

        String config_path = "config.properties";

        if (args.length > 0) {
            config_path = args[0];
        }

        TurnConfig config = new TurnConfig();
        parseConfig(config, config_path);
        TurnServer server = new TurnServer(config);
        server.setLogFunctions(new Main());
        server.start();
    }

    private static void parseConfig(TurnConfig config, String configPath) throws Exception {
        Properties p = new Properties();
        p.load(new FileReader(configPath));
        config.bind_addr = p.getProperty("bind_addr");
        config.bind_to_all = Boolean.parseBoolean(p.getProperty("bind_to_all"));
        config.port = Integer.parseInt(p.getProperty("port"));

        ArrayList<TurnConfig.Server> servers = new ArrayList<>();
        for (int i = 0; p.getProperty("servers." + i + ".ip") != null; i++) {
            servers.add(new TurnConfig.Server(p.getProperty("servers." + i + ".ip"), Integer.parseInt(p.getProperty("servers." + i + ".port"))));
        }
        config.servers = new TurnConfig.Server[servers.size()];

        config.debug_logging = Boolean.parseBoolean(p.getProperty("debug_logging"));
        config.log_very_verbose_relay_traffic = Boolean.parseBoolean(p.getProperty("log_very_verbose_relay_traffic"));
        config.require_message_integrity = Boolean.parseBoolean(p.getProperty("require_message_integrity"));
        config.default_lifetime = Integer.parseInt(p.getProperty("default_lifetime"));
        config.max_lifetime = Integer.parseInt(p.getProperty("max_lifetime"));
        config.maximum_allocations = Integer.parseInt(p.getProperty("maximum_allocations"));
        config.max_channels = Integer.parseInt(p.getProperty("max_channels"));
        config.allocation_min_port = Integer.parseInt(p.getProperty("allocation_min_port"));
        config.allocation_max_port = Integer.parseInt(p.getProperty("allocation_max_port"));
        config.allow_anonymous = Boolean.parseBoolean(p.getProperty("allow_anonymous"));
        config.default_realm = p.getProperty("default_realm");

        ArrayList<TurnConfig.Login> logins = new ArrayList<>();
        for (int i = 0; p.getProperty("static_logins." + i + ".username") != null; i++) {
            logins.add(new TurnConfig.Login(
                    p.getProperty("static_logins." + i + ".username"),
                    p.getProperty("static_logins." + i + ".realm"),
                    p.getProperty("static_logins." + i + ".password")
            ));
        }
        config.static_logins = logins.toArray(new TurnConfig.Login[logins.size()]);

        config.hacks.allow_reallocation = Boolean.parseBoolean(p.getProperty("hacks.allow_reallocation"));
        config.hacks.delete_previous_allocation_on_reallocation = Boolean.parseBoolean(p.getProperty("hacks.delete_previous_allocation_on_reallocation"));
        config.hacks.delete_on_invalid_allocation_returns_ok = Boolean.parseBoolean(p.getProperty("hacks.delete_on_invalid_allocation_returns_ok"));
        config.hacks.delete_retains_allocation_active = Boolean.parseBoolean(p.getProperty("hacks.delete_retains_allocation_active"));
        config.hacks.ignore_change_ip_port_flags_in_binding = Boolean.parseBoolean(p.getProperty("hacks.ignore_change_ip_port_flags_in_binding"));
        config.hacks.no_response_to_change_ip_port = Boolean.parseBoolean(p.getProperty("hacks.no_response_to_change_ip_port"));
        config.hacks.min_lifetime = Integer.parseInt(p.getProperty("hacks.min_lifetime"));
    }

    @Override
    public void debug(String str) {
        System.out.println("[DEBUG] " + str);
    }

    @Override
    public void info(String str) {
        System.out.println("[INFO] " + str);
    }

    @Override
    public void warning(String str) {
        System.out.println("[WARNING] " + str);
    }

    @Override
    public void error(String str) {
        System.out.println("[ERROR] " + str);
    }

    @Override
    public void exception(Throwable tr, String str) {
        System.out.println("[ERROR] " + str);
        tr.printStackTrace(System.out);
    }
}
