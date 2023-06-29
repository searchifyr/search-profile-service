package com.github.searchprofileservice.api.routes;

/**
 * Globally defined routes for controller endpoints
 */
public interface Routes {

  /**
   * Construct a concrete from a route template (containing {variable}s)
   * @param route the route template
   * @param params a list of `parameterName`, `parameterValue` pairs
   * @return
   *   a string where every occurrence of `{parameterName}` in route was replaced by `parameterValue`
   *
   * <p>
   * <h3>Examples</h3>
   * <pre>
   * {@code
   * Routes.withParams(
   *   Routes.Api.V1.Users.GetOne.route,
   *   Routes.Api.V1.Users.GetOne.PathParams.userId, "1")
   * }
   * </pre>
   */
  static String withParams(String route, String... params) {
    assert params.length % 2 == 0;

    String finalRoute = route;

    for (int i = 0; i < params.length; i += 2) {
      finalRoute = finalRoute.replace("{" + params[i] + "}", params[i + 1]);
    }

    return finalRoute;
  }

  /**
   * All REST API routes
   */
  public interface Api {
    final String path = "/api";

    /**
     * All routes to v1 of REST API
     */
    public interface V1 {
      final String path = Api.path + "/v1";

      /**
       * All routes belonging to 'applications' API endpoint
       */
      public interface Applications {
        public final String path = V1.path + "/applications";

        public final String GetAll = path;

        public final String Post = path;

        public interface GetOne {
          public final String route = Applications.path + "/{" + PathParams.applicationId + "}";

          public final String GetSearchProfiles = route + "/searchprofiles";

          public final String GetMapping = route + "/mapping";

          public final String PostDocument = route + "/documents";

          public final String PostBulkUpload = route + "/documents" + "/bulk-upload";

          public final String PostApiKey = route + "/apikeys";

          public interface PutDocument {
            public final String route = GetOne.route + "/documents" + "/{" + PathParams.documentId + "}";

            public interface PathParams {
              public final String documentId = "documentId";
            }
          }

          public interface PathParams {
            public final String applicationId = "applicationId";
          }
        }

        public interface Put {
          public final String route = Applications.path + "/{" + PathParams.applicationId + "}";

          public interface PathParams {
            public final String applicationId = "applicationId";
          }
        }

        public interface Delete {
          public final String route = Applications.path + "/{" + PathParams.applicationId + "}";


          public final String DeleteApiKey = route + "/apikeys" + "/{" + PathParams.apiKeyId + "}";

          public interface PathParams {
            public final String applicationId = "applicationId";
            public final String apiKeyId = "apiKeyId";
          }
        }

      }

      /**
       * All routes belonging to 'searchprofiles' API endpoint
       */
      public interface SearchProfiles {
        public final String path = V1.path + "/searchprofiles";

        public final String GetAll = path;

        public interface GetOne {

          public final String route = SearchProfiles.path + "/{" + PathParams.profileId + "}";

          public interface PathParams {
            public final String profileId = "profileId";
          }
        }

        public final String Post = path;

        public interface Put {
          public final String route = SearchProfiles.path + "/{" + PathParams.profileId + "}";

          public interface PathParams {
            public final String profileId = "profileId";
          }
        }

        public interface Delete {
          public final String route = SearchProfiles.path + "/{" + PathParams.profileId + "}";

          public interface PathParams {
            public final String profileId = "profileId";
          }
        }
      }

      /**
       * All routes belonging to 'users' API endpoint
       */
      public interface Users {
        public final String path = V1.path + "/users";

        public final String GetAll = path;

        public final String Post = path;

        public final String Create = path + "/elasticsearch";

        public interface GetOne {
          public final String route = Users.path + "/{" + PathParams.userId + "}";

          public interface PathParams {
            public final String userId = "userId";
          }
          public final String activate = route + "/activate";

        }

        public interface Put {
          public final String route = Users.path + "/{" + PathParams.userId + "}";

          public interface PathParams {
            public final String userId = "userId";
          }
        }

        public interface Delete {
          public final String route = Users.path + "/{" + PathParams.userId + "}";

          public interface PathParams {
            public final String userId = "userId";
          }
        }

        public interface Login {
          public final String path = Users.path + "/login";

          public final String Status = path + "/status";
        }
      }
      /**
       * All routes belonging to 'search' API endpoint
       */
      public interface search {
        public final String path = V1.path + "/search";
        public interface searchresults {
          public final String get = search.path + "/{" + SearchProfiles.Put.PathParams.profileId + "}";
        }
        public interface query {
          public final String get1 = search.path + "/{" + SearchProfiles.Put.PathParams.profileId + "}";
          public final String get2 = query.get1 + "/query";
        }
        public interface test {
          public final String path = search.path + "/test";
          public final String Post = path;
        }
      }

      /**
       * All routes belonging to 'externalServices' API endpoint
       */
      public interface externalServices {
        public final String path = V1.path + "/externalServices";
        public interface query {
          public final String get = externalServices.path + "/query";
          public final String getQueryResult = externalServices.path + "/queryResult";
        }
        public interface Applications {
          public final String path = externalServices.path + "/applications";
          public interface GetOne {
            public final String route = Applications.path + "/{" + PathParams.applicationId + "}";

            public final String PostDocument = route + "/documents";

            public final String PostBulkUpload = route + "/documents" + "/bulk-upload";

            public interface PutDocument {
              public final String route = GetOne.route + "/documents" + "/{" + PathParams.documentId + "}";

              public interface PathParams {
                public final String documentId = "documentId";
              }
            }
            public interface PathParams {
              public final String applicationId = "applicationId";
            }
          }
        }
      }
    }
  }
}
