package com.github.searchprofileservice.util;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.search.Highlight;
import co.elastic.clients.elasticsearch.core.search.HighlightField;
import com.github.searchprofileservice.model.Analyser;
import com.github.searchprofileservice.model.SearchField;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * helper class to build a request for elastic search.
 */
public class ElasticSearchSearchRequestUtil {

  private final String index;
  private final String searchValue;

  public ElasticSearchSearchRequestUtil(String index, String searchValue) {
    this.index = index;
    this.searchValue = searchValue;
  }

  /**
   * creates a request, which can be used to perform a search with elastic search.
   * 
   * @param searchFields the field (w/ boost) to search in
   * @param analyser additional search options
   *
   * @return created search request
   */
  public SearchRequest createRequest(List<SearchField> searchFields, Analyser analyser) {
    return this.createRequest(searchFields, null, analyser);
  }

  /**
   * creates a request, which can be used to perform a search with elastic search.
   * 
   * @param searchFields the field (w/ boost) to search in
   * @param minScore the min score a result must have to be returned
   * @param analyser additional search options
   *
   * @return created search request
   */
  public SearchRequest createRequest(
    List<SearchField> searchFields,
    Double minScore,
    Analyser analyser
  ) {
    return SearchRequest.of(s -> s
        .index(index)
        .minScore(minScore)
        .query(createQuery(searchFields, analyser))
        .highlight(createHighlighter(searchFields))
    );
  }

  /**
   * creates a query with the corresponding profile data.
   *
   * @return query
   */
  private Query createQuery(List<SearchField> searchFields, Analyser analyser) {
    return Query.of(q -> q
        .bool(b -> b
            .must(List.of())
            .should(getShouldSubQuery(searchFields, analyser))
        )
    );
  }

  /**
   * creates the sub query, which will be part of the main query.
   *
   * @return query
   */
  private Query getShouldSubQuery(List<SearchField> searchFields, Analyser analyser) {
    List<String> fields = searchFields
        .stream()
        .filter(SearchField::isEnabled)
        .map(searchField -> searchField.getFieldName() + "^" + searchField.getBoost().floatValue())
        .toList();

    return analyser.isFaultTolerant() ? getFaultTolerantSubQuery(fields) : getNotFaultTolerantSubQuery(fields);
  }

  /**
   * creates a query, which searches for the given search value in all fields.
   *
   * @return query
   */
  private Query getNotFaultTolerantSubQuery(List<String> fields) {
    return Query.of(q -> q
        .multiMatch(m -> m
            .query(searchValue)
            .type(TextQueryType.MostFields)
            .fields(fields)));
  }

  /**
   * creates a query, which searches for the given search value in all fields with a fault tolerance
   * of one character.
   *
   * @return query
   */
  private Query getFaultTolerantSubQuery(List<String> fields) {
    return Query.of(q -> q
        .multiMatch(m -> m
            .query(searchValue)
            .type(TextQueryType.MostFields)
            .fields(fields)
            .fuzziness("AUTO")
            .fuzzyTranspositions(true)
            .fuzzyRewrite("constant_score")
            .prefixLength(0)));
  }
  /**
   * creates Highlights for every enabled field of a given SearchProfileDTO-Object
   *
   * @return highlights
   */

  private Highlight createHighlighter(List<SearchField> searchFields) {
    Map<String, HighlightField> highlightFieldMap = new HashMap<>();

    searchFields.stream()
            .filter(SearchField::isEnabled)
            .forEach(searchField -> highlightFieldMap
                    .put(searchField.getFieldName(), new HighlightField.Builder().build())
            );
    return    Highlight.of(q -> q
            .fields(highlightFieldMap)
    );
  }
}
