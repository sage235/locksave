// config/PaymentConfig.java

package com.LockSaveApplication.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.payment")
public class PaymentConfig {

    private MtnMomo  mtnMomo  = new MtnMomo();
    private Airtel   airtel   = new Airtel();
    private Orange   orange   = new Orange();
    private Webhook  webhook  = new Webhook();

    @Getter @Setter
    public static class MtnMomo {
        private String baseUrl;
        private String subscriptionKey;
        private String apiUser;
        private String apiKey;
        private String targetEnvironment;
        private String callbackUrl;
    }

    @Getter @Setter
    public static class Airtel {
        private String baseUrl;
        private String clientId;
        private String clientSecret;
        private String callbackUrl;
    }

    @Getter @Setter
    public static class Orange {
        private String baseUrl;
        private String clientId;
        private String clientSecret;
        private String callbackUrl;
    }

    @Getter @Setter
    public static class Webhook {
        // Secret used to verify webhook signatures from providers
        private String mtnSecret;
        private String airtelSecret;
        private String orangeSecret;
    }
}