package com.github.searchprofileservice.service;

import com.github.searchprofileservice.api.model.ElasticSearchUser;
import com.github.searchprofileservice.model.enums.ElasticSearchMappingType;
import com.github.searchprofileservice.persistence.mongo.model.Application;
import org.elasticsearch.client.security.user.privileges.IndicesPrivileges;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ElasticSearchClientService {

    /**
     *
     * @param application Application from which the Index should be created
     * @return Whether the index was successfully created.
     */
    boolean createIndex(Application application);

    /**
     * Removes an index from elastic search
     * @param application the application whose index to remove
     * @return true, if deletion was successful, false otherwise
     * @throws IOException on error while removal
     */
    default void deleteIndex(Application application) throws IOException {
        deleteIndex(application.getId());
    }

    /**
     * Removes an index from elastic search
     * @param id the id of the application whose index to remove
     * @throws IOException on error while removal
     */
    void deleteIndex(UUID id) throws IOException;


    /**
     * Uploads Raw json to specific Application
     * @param applicationId the id of the application whose es-index to insert the data into
     * @param rawJson Document which is going to be uploaded to the Application
     * @return The id of the created document
     */
    String uploadRawJsonToApplication(UUID applicationId, String rawJson) throws IOException;

    /**
     * Uploads Raw jsons to specific Application
     * @param rawJsons Documents which are going to be uploaded to the Application
     * @param applicationId the id of the application whose es-index to insert the data into
     * @return The id of the created document
     */
    List<String> bulkUploadRawJsonToApplication(UUID applicationId, List<String> rawJsons) throws IOException;

    /**
     * Updates the content of an existing document
     * @param applicationId the id of the application whose es-index the document belongs to
     * @param documentId the id of document to update
     * @param rawJson Document which is going to be uploaded to the Application
     */
    void updateDocument(UUID applicationId, String documentId, String rawJson) throws IOException;

    /**
     *
     * @param application The application of which the index mapping should be retrieved.
     * @return The index mapping of the specified application.
     */
    Map<String, ElasticSearchMappingType> getIndexMapping(Application application);

    /**
     *
     * @param indexName The name of the index of which the mapping should be retrieved
     * @return The index mapping of the specified application.
     */
    Map<String, ElasticSearchMappingType> getIndexMapping(String indexName);

    /**
     *
     * @param indexPrivilege COLLECTION<String, String> which describe the Privileges on ES indicies
     * @return multiple IndicesPrivileges with diffrent settings.
     */
    List<IndicesPrivileges> createIndicesPrivileges(Map<String, String> indexPrivilege);

    /**
     *
     * @param newUser new User to create
     * @return Whether the User was successfully created or not.
     */
    boolean createElasticSearchUser(ElasticSearchUser newUser);

    /**
     *
     * @param index Name of the Index
     * @return the number of uploaded Documents for a specific Elasticsearch Index
     */
    long getDocumentCountForIndex(String index);

}
