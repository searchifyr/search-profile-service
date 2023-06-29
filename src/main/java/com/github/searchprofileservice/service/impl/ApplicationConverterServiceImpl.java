package com.github.searchprofileservice.service.impl;

import com.github.searchprofileservice.api.model.ApiKeyDto;
import com.github.searchprofileservice.api.model.ApplicationDto;
import com.github.searchprofileservice.persistence.mongo.model.Application;
import com.github.searchprofileservice.service.ApplicationConverterService;
import com.github.searchprofileservice.service.ElasticSearchClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationConverterServiceImpl implements ApplicationConverterService {

    private final ElasticSearchClientService elasticSearchService;

    @Override
    public ApplicationDto convertToApplicationDto(Application application) {
        ApplicationDto applicationDto = fromApplication(application);
        var applicationId = application.getId();
        if(applicationId == null){
            return applicationDto;
        }
        long documentCount = elasticSearchService.getDocumentCountForIndex(applicationId.toString());
        applicationDto.setNumberOfDocuments(documentCount);
        return applicationDto;
    }

    /**
     * converts the application dto application document.
     *
     * @return application document
     */
    @Override
    public Application convertToApplication(ApplicationDto applicationDto) {
        return new Application(applicationDto.getId(),
                applicationDto.getCreatedDate(),
                applicationDto.getName(),
                applicationDto.isActive(),
                applicationDto.getAllowedUserIds());
    }

    /**
     * converts an application object to data transfer object.
     *
     * @param application application document, which will be converted
     * @return data transfer object
     */
    private ApplicationDto fromApplication(Application application) {
        ArrayList<ApiKeyDto> apiKeyDtos = new ArrayList<ApiKeyDto>(
                application.getApiKeys().stream()
                        .map(ApiKeyDto::fromApiKey).toList());

        return new ApplicationDto(
                application.getId(),
                application.getCreatedDate(),
                application.getApplicationName(),
                application.getCreatorId(),
                application.isActive(),
                application.getAllowedUserIds(),
                apiKeyDtos);
    }
}
