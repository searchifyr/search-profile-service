package com.github.searchprofileservice.util;

import co.elastic.clients.elasticsearch._types.mapping.Property;
import com.github.searchprofileservice.model.enums.ElasticSearchMappingType;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ElasticSearchMappingFlattenerUtil {

  /**
   *
   * This util will flatten a provided raw elasticsearch mapping to a mapping structure resembling
   * the raw mapping, but as a dot-notation.
   * You will get the identifier (flattened) as a key and the type value as a value.
   *
   * @param rawMapping    The raw mapping as it will be provided by elasticsearch.
   * @param mappingDepth  The maximum mapping depth for the raw mapping provided by elasticsearch
   *                      when flattening.
   *
   * @return  The flattened mapping structure.
   *          Example:  person.name : ElasticSearchMappingType.TEXT
   *                    person.id   : ElasticSearchMappingType.NOT_SUPPORTED
   */
  public static Map<String, ElasticSearchMappingType> flattenElasticSearchIndexMapping(
      Map<String, Property> rawMapping, int mappingDepth) {

    Map<String, ElasticSearchMappingType> result = new HashMap<>();

    for (Entry<String, Property> field : rawMapping.entrySet()) {

      String fieldName = field.getKey();
      Property fieldValue = field.getValue();

      if (fieldValue.isObject())
        recurseObjectResolving(fieldName, fieldValue, 1, mappingDepth, result);
      else
        result.put(fieldName, getType(fieldValue));
    }

    return result;
  }

  private static void recurseObjectResolving(
      String buildString,
      Property object,
      int currentMappingDepth,
      int maxMappingDepth,
      Map<String, ElasticSearchMappingType> flatMapToFill
  ) {

    currentMappingDepth++;

    if (currentMappingDepth > maxMappingDepth) {
      flatMapToFill.put(buildString, ElasticSearchMappingType.NOT_SUPPORTED);
      return;
    }

    Map<String, Property> mapping = object.object().properties();

    for (Entry<String, Property> field : mapping.entrySet()) {

      String fieldName = concatToFieldName(buildString, field.getKey());
      Property fieldValue = field.getValue();

      if (fieldValue.isObject()) {
        recurseObjectResolving(
            fieldName,
            fieldValue,
            currentMappingDepth,
            maxMappingDepth,
            flatMapToFill
        );
      } else flatMapToFill.put(fieldName, getType(fieldValue));

    }
  }

  private static String concatToFieldName(String currentFieldName, String newFieldValue) {
    return currentFieldName + '.' + newFieldValue;
  }

  private static ElasticSearchMappingType getType(Property field) {
    if (field.isText()) return ElasticSearchMappingType.TEXT;
    else return ElasticSearchMappingType.NOT_SUPPORTED;
  }

}
