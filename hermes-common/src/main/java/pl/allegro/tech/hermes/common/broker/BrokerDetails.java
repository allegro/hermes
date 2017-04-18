package pl.allegro.tech.hermes.common.broker;

public class BrokerDetails {

    private String host;
    private int port;

    private int unusedStuff;
    private int zero = 0;

    public BrokerDetails(String host, int port) {
        this.host = host;
        this.port = port;
    }

    private void unusedMessedUpMethod() {
        label:
        while (true) {
            try {
                if (zero == 1337) {
                    System.out.println("look ma, zero == 1337");
                    throw new Exception("throwing exception");
                } else {
                    zero += 1;
                    break label;
                }
            } catch (Throwable e) {
                System.err.println("catching them all like Ash Ketchum");
            }
        }
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}
