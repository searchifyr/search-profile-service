package com.github.searchprofileservice.service.impl;

import co.elastic.clients.elasticsearch._types.mapping.Property;
import com.github.searchprofileservice.api.model.ElasticSearchUser;
import com.github.searchprofileservice.client.ElasticSearchStatefulClient;
import com.github.searchprofileservice.exception.ElasticSearchUnavailableException;
import com.github.searchprofileservice.exception.IndexNotFoundException;
import com.github.searchprofileservice.model.enums.ElasticSearchMappingType;
import com.github.searchprofileservice.persistence.mongo.model.Application;
import com.github.searchprofileservice.service.ElasticSearchClientService;
import com.github.searchprofileservice.util.ElasticSearchMappingFlattenerUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.client.security.PutUserResponse;
import org.elasticsearch.client.security.user.privileges.IndicesPrivileges;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ElasticSearchClientServiceImpl implements ElasticSearchClientService {

    private static final int MAX_MAPPING_DEPTH = 5;

    private final ElasticSearchStatefulClient client;

    @Override
    public boolean createIndex(Application application) {

        boolean success = false;
        try {
            UUID id = application.getId();
            if (null == id) {
                return false;
            } else {
                success = client.createIndex(id.toString());
            }
        } catch (Exception e) {
            log.error(e.getMessage());
            return false;
        }
        return success;
    }

    @Override
    public void deleteIndex(UUID id) throws IOException {
        client.deleteIndex(id.toString());
    }

    @Override
    public String uploadRawJsonToApplication(UUID applicationId, String rawJson) throws IOException {
        return client.uploadRawJsonToIndex(applicationId.toString(), rawJson);
    }

    @Override
    public List<String> bulkUploadRawJsonToApplication(UUID applicationId, List<String> rawJsons) throws IOException {
       return client.bulkUploadRawJsonToIndex(applicationId.toString(), rawJsons);
    }

    @Override
    public void updateDocument(UUID applicationId, String documentId, String rawJson) throws IOException {
        client.updateDocument(applicationId.toString(), documentId, rawJson);
    }

    @Override
    public Map<String, ElasticSearchMappingType> getIndexMapping(Application application) {
        UUID id = application.getId();
        if (null != id) {
            return getIndexMapping(id.toString());
        } else {
            return Collections.emptyMap();
        }
    }

    @Cacheable("index-mapping")
    @Override
    public Map<String, ElasticSearchMappingType> getIndexMapping(String indexName) {

        try {

            if (!client.isIndexExistent(indexName))
                throw new IndexNotFoundException(String.format("The index '%s' was not found.", indexName));

            Map<String, Property> rawResult = client.getIndexMapping(indexName);

            return ElasticSearchMappingFlattenerUtil.flattenElasticSearchIndexMapping(
                rawResult, MAX_MAPPING_DEPTH);

        } catch (IOException e) {
            log.error(e.getMessage());
            throw new ElasticSearchUnavailableException(e);
        }
    }

    @Override
    public List<IndicesPrivileges> createIndicesPrivileges(Map<String, String> indexPrivilege) {
        List<IndicesPrivileges> indicesPrivileges = new ArrayList<>();
        for (Map.Entry<String, String> entry : indexPrivilege.entrySet()) {
            indicesPrivileges.add(
                            IndicesPrivileges.builder()
                                    .indices(entry.getKey())
                                    .allowRestrictedIndices(false)
                                    .privileges(entry.getValue())
                                    .build());
        }
        return indicesPrivileges;
    }

    @Override
    public boolean createElasticSearchUser(ElasticSearchUser newUser) {
        PutUserResponse response = null;
        try {
            response = client.createElasticUser(newUser.getUserName(), newUser.getPassword());
        }catch (Exception e){
            log.warn(e.getMessage());
            return false;
        }
        return response.isCreated();
    }

    @Override
    public long getDocumentCountForIndex(String index){
        long documentCount = 0;
        try {
            documentCount = client.getIndexDocumentCount(index);

        }catch(Exception e){
            log.error(e.getMessage());
            return documentCount;
        }
        return documentCount;
    }

}
