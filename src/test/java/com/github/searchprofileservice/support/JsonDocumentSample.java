package com.github.searchprofileservice.support;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class JsonDocumentSample {

  public static String getElasticSearchIndexJsonDocumentSample() {
    return ("{'name': 'Johannes', "
        + "'certificate': {'name': 'Microsoft', 'available': false}, 'object1': {'object2': "
        + "{'object3': {'object4': {'object5': {'object6': 'heyho'}}}}}}")
        .replace('\'', '"');
  }

}
