package org.incogn1.autoshutdown.config;

public class Config {
    private Delays delays;
    private Logging logging;

    public Delays getDelays() {
        return delays;
    }

    public Logging getLogging() {
        return logging;
    }

    public static class Delays {
        private final int initial;
        private final int polling;
        private final int shutdown;

        public Delays(int initial, int polling, int shutdown) {
            this.initial = initial;
            this.polling = polling;
            this.shutdown = shutdown;
        }

        public int getInitial() {
            return initial;
        }

        public int getPolling() {
            return polling;
        }

        public int getShutdown() {
            return shutdown;
        }
    }

    public static class Logging {
        private final boolean enabled;

        public Logging(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean getEnabled() {
            return enabled;
        }
    }
}
