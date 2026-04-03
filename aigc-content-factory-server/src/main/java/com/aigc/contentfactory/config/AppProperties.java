package com.aigc.contentfactory.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Cors cors = new Cors();
    private Ai ai = new Ai();
    private Storage storage = new Storage();
    private Workflow workflow = new Workflow();
    private Publish publish = new Publish();

    @Data
    public static class Cors {
        private String allowedOrigin;
    }

    @Data
    public static class Ai {
        private boolean enabled;
        private String baseUrl;
        private String apiKey;
        private String model;
    }

    @Data
    public static class Storage {
        private String baseUrl;
        private String rootDir;
    }

    @Data
    public static class Workflow {
        private boolean autoCollectHotspotsOnCreate;
        private boolean manualReviewRequired;
    }

    @Data
    public static class Publish {
        private Platform xiaohongshu = new Platform();
        private Platform douyin = new Platform();
        private Platform bilibili = new Platform();
    }

    @Data
    public static class Platform {
        private String uploadUrl;
        private boolean autoOpen = true;
        private String browserProfileDir;
        private String helperLogDir;
        private Integer qrLoginTimeoutSec = 180;
    }
}
