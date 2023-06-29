package com.github.searchprofileservice.util;

import co.elastic.clients.elasticsearch._types.mapping.Property;
import com.github.searchprofileservice.model.enums.ElasticSearchMappingType;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ElasticSearchMappingFlattenerUtilTest {

  private final static Property TYPE_KEYWORD = Property.of(b -> b.keyword(k -> k.boost(1.0)));

  private final static Property TYPE_TEXT = Property.of(b -> b.text(t -> t.boost(1.0)));

  private final static Property TYPE_INTEGER = Property.of(b -> b.integer(i -> i.nullValue(0)));

  private final static Property TYPE_BOOLEAN = Property.of(b -> b.boolean_(bo -> bo.boost(1.0)));

  @Test
  void flattener_returns_correct_mapping_on_only_first_level_elastic_search_mapping() {

    // arrange
    String idField = "id";
    String nameField = "name";
    String costsField = "costs";
    String descriptionField = "description";

    Map<String, Property> elasticSearchMapping = new HashMap<>();
    elasticSearchMapping.put(idField, TYPE_KEYWORD);
    elasticSearchMapping.put(nameField, TYPE_TEXT);
    elasticSearchMapping.put(costsField, TYPE_INTEGER);
    elasticSearchMapping.put(descriptionField, TYPE_TEXT);

    Map<String, ElasticSearchMappingType> expected = new HashMap<>();
    expected.put(idField, ElasticSearchMappingType.NOT_SUPPORTED);
    expected.put(nameField, ElasticSearchMappingType.TEXT);
    expected.put(costsField, ElasticSearchMappingType.NOT_SUPPORTED);
    expected.put(descriptionField, ElasticSearchMappingType.TEXT);

    // act
    Map<String, ElasticSearchMappingType> result
        = ElasticSearchMappingFlattenerUtil.flattenElasticSearchIndexMapping(
        elasticSearchMapping, 5);

    // assert
    assertEquals(expected, result);
  }

  @Test
  void flattener_returns_correct_mapping_on_objects() {

    // arrange
    Map<String, Property> certificatesField = new HashMap<>();
    certificatesField.put("microsoft", TYPE_BOOLEAN);
    certificatesField.put("oracle", TYPE_BOOLEAN);
    certificatesField.put("amazon", TYPE_BOOLEAN);

    Map<String, Property> personFields = new HashMap<>();
    personFields.put("id", TYPE_KEYWORD);
    personFields.put("name", TYPE_TEXT);
    personFields.put("age", TYPE_INTEGER);
    personFields.put("description", TYPE_TEXT);
    personFields.put("certificates", getObjectProperty(certificatesField));

    Map<String, Property> elasticSearchMapping = new HashMap<>();
    elasticSearchMapping.put("id", TYPE_KEYWORD);
    elasticSearchMapping.put("person", getObjectProperty(personFields));

    Map<String, ElasticSearchMappingType> expected = new HashMap<>();
    expected.put("id", ElasticSearchMappingType.NOT_SUPPORTED);
    expected.put("person.id", ElasticSearchMappingType.NOT_SUPPORTED);
    expected.put("person.name", ElasticSearchMappingType.TEXT);
    expected.put("person.age", ElasticSearchMappingType.NOT_SUPPORTED);
    expected.put("person.description", ElasticSearchMappingType.TEXT);
    expected.put("person.certificates.microsoft", ElasticSearchMappingType.NOT_SUPPORTED);
    expected.put("person.certificates.oracle", ElasticSearchMappingType.NOT_SUPPORTED);
    expected.put("person.certificates.amazon", ElasticSearchMappingType.NOT_SUPPORTED);

    // act
    Map<String, ElasticSearchMappingType> result
        = ElasticSearchMappingFlattenerUtil.flattenElasticSearchIndexMapping(
        elasticSearchMapping, 5);

    // assert
    assertEquals(expected, result);
  }

  @Test
  void flattener_returns_not_supported_on_exceeded_mapping_depth_and_cuts_fields() {

    // arrange
    Map<String, Property> object6 = new HashMap<>();
    object6.put("name", TYPE_TEXT);

    Map<String, Property> object5 = new HashMap<>();
    object5.put("object6", getObjectProperty(object6));

    Map<String, Property> object4 = new HashMap<>();
    object4.put("object5", getObjectProperty(object5));

    Map<String, Property> object3 = new HashMap<>();
    object3.put("object4", getObjectProperty(object4));

    Map<String, Property> object2 = new HashMap<>();
    object2.put("object3", getObjectProperty(object3));

    Map<String, Property> object1 = new HashMap<>();
    object1.put("object2", getObjectProperty(object2));

    Map<String, Property> elasticSearchMapping = new HashMap<>();
    elasticSearchMapping.put("id", TYPE_KEYWORD);
    elasticSearchMapping.put("description", TYPE_TEXT);
    elasticSearchMapping.put("object1", getObjectProperty(object1));

    Map<String, ElasticSearchMappingType> expected = new HashMap<>();
    expected.put("id", ElasticSearchMappingType.NOT_SUPPORTED);
    expected.put("description", ElasticSearchMappingType.TEXT);
    expected.put("object1.object2.object3.object4.object5", ElasticSearchMappingType.NOT_SUPPORTED);

    // act
    Map<String, ElasticSearchMappingType> result
        = ElasticSearchMappingFlattenerUtil.flattenElasticSearchIndexMapping(
        elasticSearchMapping, 5);

    // assert
    assertEquals(expected, result);
  }

  private Property getObjectProperty(Map<String, Property> object) {
    return Property.of(b -> b.object(o -> o.properties(object)));
  }

}