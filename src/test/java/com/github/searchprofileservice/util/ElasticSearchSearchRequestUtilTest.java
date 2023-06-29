package com.github.searchprofileservice.util;

import co.elastic.clients.elasticsearch.core.SearchRequest;
import com.github.searchprofileservice.model.Analyser;
import com.github.searchprofileservice.model.SearchField;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;


public class ElasticSearchSearchRequestUtilTest {

  private final String index = UUID.randomUUID().toString();
  private final String searchValue = "searchText";

  private final ElasticSearchSearchRequestUtil elasticSearchSearchRequestUtil
      = new ElasticSearchSearchRequestUtil(index, searchValue);

  @Test
  public void createRequest() {
    List<SearchField> searchFields = getSearchFields();
    SearchRequest request = elasticSearchSearchRequestUtil.createRequest(searchFields,
        new Analyser());
    String requestAsString = request.toString();
    assertThat(requestAsString, containsString("/" + index + "/_search"));
    assertThat(requestAsString, containsString("bool"));
    assertThat(requestAsString, containsString("should"));
    assertThat(requestAsString, containsString("text^1.0"));
    assertThat(requestAsString, containsString("title^2.0"));
    assertThat(requestAsString, containsString("id^0.5"));
  }

  @Test
  public void createRequest_with_min_score() {
    List<SearchField> searchFields = getSearchFields();
    final Double minScore = 3.14159;
    SearchRequest request =
        elasticSearchSearchRequestUtil.createRequest(searchFields, minScore, new Analyser());
    String requestAsString = request.toString();
    assertThat(requestAsString, containsString("\"min_score\":"));
    assertThat(requestAsString, containsString(minScore.toString()));
    assertThat(requestAsString, containsString("/" + index + "/_search"));
    assertThat(requestAsString, containsString("bool"));
    assertThat(requestAsString, containsString("should"));
    assertThat(requestAsString, containsString("text^1.0"));
    assertThat(requestAsString, containsString("title^2.0"));
    assertThat(requestAsString, containsString("id^0.5"));
  }

  @Test
  public void createRequest_withFaultTolerance() {
    List<SearchField> searchFields = getSearchFields();
    Analyser analyser = new Analyser();
    analyser.setFaultTolerant(true);
    SearchRequest request = elasticSearchSearchRequestUtil.createRequest(searchFields, analyser);

    String requestAsString = request.toString();
    assertThat(requestAsString, containsString("/" + index + "/_search"));
    testQueryOnBoostFields(requestAsString);
    assertThat(requestAsString, containsString("\"fuzzy_transpositions\":true"));
    assertThat(requestAsString, containsString("\"fuzziness\":\"AUTO\""));
  }

  private void testQueryOnBoostFields(String requestAsString) {
    assertThat(requestAsString, containsString("bool"));
    assertThat(requestAsString, containsString("should"));
    assertThat(requestAsString, containsString("text^1.0"));
    assertThat(requestAsString, containsString("title^2.0"));
    assertThat(requestAsString, containsString("id^0.5"));
  }

  private List<SearchField> getSearchFields() {
    return List.of(
        new SearchField("text", true, 1.0),
        new SearchField("title", true, 2.0),
        new SearchField("id", true, 0.5)
    );
  }
}
