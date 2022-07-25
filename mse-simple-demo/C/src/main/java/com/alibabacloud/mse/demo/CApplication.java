
package com.alibabacloud.mse.demo;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.commons.util.InetUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

@SpringBootApplication
public class CApplication {

    public static void main(String[] args) {
        SpringApplication.run(CApplication.class, args);
    }

    @Bean
    @LoadBalanced
    RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @RestController
    class AController {

        @Autowired
        RestTemplate restTemplate;

        @Autowired
        InetUtils inetUtils;

        @Value("${throwException:false}")
        boolean throwException;

        private String currentZone;

        @PostConstruct
        private void init() {
            try {
                HttpClient client = HttpClientBuilder.create().build();
                RequestConfig requestConfig = RequestConfig.custom()
                        .setConnectionRequestTimeout(1000)
                        .setConnectTimeout(1000)
                        .setSocketTimeout(1000)
                        .build();
                HttpGet req = new HttpGet("http://100.100.100.200/latest/meta-data/zone-id");
                req.setConfig(requestConfig);
                HttpResponse response = client.execute(req);
                currentZone = EntityUtils.toString(response.getEntity());
            } catch (Exception e) {
                currentZone = e.getMessage();
            }
        }

        @GetMapping("/c")
        public String c(HttpServletRequest request) {
            if (throwException) {
                throw new RuntimeException();
            }
            return "C" + SERVICE_TAG + "[" + inetUtils.findFirstNonLoopbackAddress().getHostAddress() + "]";
        }

        @GetMapping("/c-zone")
        public String cZone(HttpServletRequest request) {
            if (throwException) {
                throw new RuntimeException();
            }
            return "C" + SERVICE_TAG + "[" + currentZone + "]";
        }
    }

    public static String SERVICE_TAG = "";

    static {

        try {
            File file = new File("/etc/podinfo/annotations");
            if (file.exists()) {

                Properties properties = new Properties();
                FileReader fr = null;
                try {
                    fr = new FileReader(file);
                    properties.load(fr);
                } catch (IOException e) {
                } finally {
                    if (fr != null) {
                        try {
                            fr.close();
                        } catch (Throwable ignore) {
                        }
                    }
                }
                SERVICE_TAG = properties.getProperty("alicloud.service.tag").replace("\"", "");
            } else {
                SERVICE_TAG = System.getProperty("alicloud.service.tag");;
            }
        } catch (Throwable ignore) {
        }
        if ("null".equalsIgnoreCase(SERVICE_TAG) || null == SERVICE_TAG) {
            SERVICE_TAG = "";
        }
    }
}
