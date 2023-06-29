package com.github.searchprofileservice.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder.HttpClientConfigCallback;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.RestHighLevelClientBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class ElasticSearchClientConfig {

  private final ElasticSearchProperties elasticSearchProperties;

  @Bean
  public CredentialsProvider elasticSearchCredentials() {

    CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
    credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(
        elasticSearchProperties.getUsername(), elasticSearchProperties.getPassword()
    ));

    return credentialsProvider;
  }

  @Bean
  public RestClient elasticSearchLowLevelRestClient(CredentialsProvider elasticSearchCredentials) {
    RestClient restClient = RestClient.builder(new HttpHost(
        elasticSearchProperties.getHost(), elasticSearchProperties.getPort()
    )).setHttpClientConfigCallback(new HttpClientConfigCallback() {
      @Override
      public HttpAsyncClientBuilder customizeHttpClient(
          HttpAsyncClientBuilder httpAsyncClientBuilder) {
        return httpAsyncClientBuilder.setDefaultCredentialsProvider(elasticSearchCredentials);
      }
    }).build();

    return restClient;
  }

  @Bean
  public ElasticsearchClient elasticsearchClient(RestClient elasticSearchLowLevelRestClient) {

    ElasticsearchTransport transport
        = new RestClientTransport(elasticSearchLowLevelRestClient, new JacksonJsonpMapper());

    return new ElasticsearchClient(transport);
  }

@Bean
  public RestHighLevelClient highLevelClient(RestClient elasticSearchLowLevelRestClient){
   return new RestHighLevelClientBuilder(elasticSearchLowLevelRestClient)
            .setApiCompatibilityMode(true)
            .build();
  }

}
