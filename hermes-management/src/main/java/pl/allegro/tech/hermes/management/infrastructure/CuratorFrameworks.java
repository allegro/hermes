package pl.allegro.tech.hermes.management.infrastructure;

import org.apache.curator.framework.CuratorFramework;

import java.util.List;

public class CuratorFrameworks {
    private List<CuratorFramework> frameworks;

    public CuratorFrameworks(List<CuratorFramework> frameworks) {
        this.frameworks = frameworks;
    }

    public List<CuratorFramework> getFrameworks() {
        return frameworks;
    }
}
