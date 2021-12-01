package models;

/**
 *
 * @author Allan Capistrano
 * @author João Erick Barbosa
 */
public class Company {
    private int amountRequests;
    private ServerAddress server;

    public Company(int amountRequests, ServerAddress server) {
        this.amountRequests = amountRequests;
        this.server = server;
    }

    public int getAmountRequests() {
        return amountRequests;
    }

    public ServerAddress getServer() {
        return server;
    }
}
