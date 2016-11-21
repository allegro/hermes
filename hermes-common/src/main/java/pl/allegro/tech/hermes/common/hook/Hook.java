package pl.allegro.tech.hermes.common.hook;

public interface Hook {
    int LOWER_PRIORITY = 0;
    int NORMAL_PRIORITY = 1;
    int HIGHER_PRIORITY = 2;

    void apply();

    /*
        Hooks with higher priority are executed in the first place
     */
    default int getPriority() {
        return NORMAL_PRIORITY;
    }

}
