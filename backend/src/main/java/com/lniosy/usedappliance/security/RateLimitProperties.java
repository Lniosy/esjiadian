package com.lniosy.usedappliance.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "app.security.rate-limit")
public class RateLimitProperties {
    private boolean enabled = true;
    private long windowSeconds = 60;
    private int maxAnonymous = 120;
    private int maxAuthenticated = 300;
    private List<Group> groups = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getWindowSeconds() {
        return windowSeconds;
    }

    public void setWindowSeconds(long windowSeconds) {
        this.windowSeconds = windowSeconds;
    }

    public int getMaxAnonymous() {
        return maxAnonymous;
    }

    public void setMaxAnonymous(int maxAnonymous) {
        this.maxAnonymous = maxAnonymous;
    }

    public int getMaxAuthenticated() {
        return maxAuthenticated;
    }

    public void setMaxAuthenticated(int maxAuthenticated) {
        this.maxAuthenticated = maxAuthenticated;
    }

    public List<Group> getGroups() {
        return groups;
    }

    public void setGroups(List<Group> groups) {
        this.groups = groups == null ? new ArrayList<>() : groups;
    }

    public static class Group {
        private String name;
        private List<String> pathPrefixes = new ArrayList<>();
        private Integer maxAnonymous;
        private Integer maxAuthenticated;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<String> getPathPrefixes() {
            return pathPrefixes;
        }

        public void setPathPrefixes(List<String> pathPrefixes) {
            this.pathPrefixes = pathPrefixes == null ? new ArrayList<>() : pathPrefixes;
        }

        public Integer getMaxAnonymous() {
            return maxAnonymous;
        }

        public void setMaxAnonymous(Integer maxAnonymous) {
            this.maxAnonymous = maxAnonymous;
        }

        public Integer getMaxAuthenticated() {
            return maxAuthenticated;
        }

        public void setMaxAuthenticated(Integer maxAuthenticated) {
            this.maxAuthenticated = maxAuthenticated;
        }
    }
}
