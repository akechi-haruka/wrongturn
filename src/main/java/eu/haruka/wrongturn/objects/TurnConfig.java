package eu.haruka.wrongturn.objects;

public class TurnConfig {
    public int port = 5555;
    public boolean require_message_integrity;
    public Server[] servers = new Server[]{new Server("127.0.0.1", 5555)};
    public int default_lifetime = 600;
    public int max_lifetime = 3600;
    public Login[] static_logins = new Login[0];
    public int maximum_allocations = 999;
    public int max_channels = 5;
    public String bind_addr = "0.0.0.0";
    public boolean debug_logging = false;
    public Hacks hacks;
    public boolean bind_to_all = true;
    public int allocation_min_port = 60000;
    public int allocation_max_port = 60999;
    public boolean allow_anonymous = false;
    public boolean log_very_verbose_relay_traffic = false;

    public class Server {
        public String ip;
        public int port;

        public Server() {
        }

        public Server(String ip, int port) {
            this.ip = ip;
            this.port = port;
        }
    }

    public class Login {
        public String username;
        public String realm;
        public String password;
    }

    public class Hacks {
        public boolean allow_reallocation;
        public boolean delete_previous_allocation_on_reallocation;
        public boolean delete_on_invalid_allocation_returns_ok;
        public boolean delete_retains_allocation_active;
        public boolean ignore_change_ip_port_flags_in_binding;
        public int min_lifetime = 600;
        public boolean no_response_to_change_ip_port = true;
    }
}
