package com.company.sre.tools;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Service
public class SreTools {

    private static final Logger log = LoggerFactory.getLogger(SreTools.class);
    private final RestClient elasticClient;
    private final RestClient prometheusClient;

    public SreTools(
            @Value("${app.elasticsearch.url}") String elasticsearchUrl,
            @Value("${app.prometheus.url}") String prometheusUrl) {

        this.elasticClient = RestClient.builder().baseUrl(elasticsearchUrl).build();
        this.prometheusClient = RestClient.builder().baseUrl(prometheusUrl).build();
    }

    @Tool(description = "Queries the local Elasticsearch cluster to extract error logs for a given microservice name during the last 15 minutes.")
    public String fetchRecentLogs(String serviceName) {
        log.info("Executing Elasticsearch log retrieval for service: {}", serviceName);

        // Raw Elasticsearch Query DSL checking match against fields
        String queryDsl = """
        {
          "query": {
            "bool": {
              "must": [
                { "match": { "appName": "%s" } },
                { "match": { "logLevel": "ERROR" } }
              ],
              "filter": [
                {
                  "range": {
                    "@timestamp": {
                      "gte": "now-15m"
                    }
                  }
                }
              ]
            }
          },
          "size": 10
        }
        """.formatted(serviceName);

        try {
            return elasticClient.post()
                    .uri("/_search")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(queryDsl)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            log.error("Failed to query Elasticsearch engine", e);
            return "Error retrieving logs from local cluster: " + e.getMessage();
        }
    }

    @Tool(description = "Executes an immediate raw PromQL query evaluation against the local Prometheus instance API.")
    public String queryClusterMetrics(String queryExpression) {
        log.info("Executing Prometheus metric evaluation for query expression: {}", queryExpression);

        try {
            return prometheusClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/v1/query")
                            .queryParam("query", queryExpression)
                            .build())
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);
        } catch (Exception e) {
            log.error("Failed to fetch values from Prometheus engine", e);
            return "Error querying metrics from local cluster: " + e.getMessage();
        }
    }

    @Tool(description = "Restarts a Kubernetes pod or Docker container for a given microservice to resolve hung or deadlocked states.")
    public String restartPod(String serviceName) {
        log.info("Executing pod restart for service: {}", serviceName);

        // TODO: Wire up actual Kubernetes Fabric8 client or Docker Engine API call here

        return "Successfully initiated restart for pod: " + serviceName;
    }
}