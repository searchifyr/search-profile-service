package com.github.searchprofileservice.service;

import com.github.searchprofileservice.api.model.ApplicationDto;
import com.github.searchprofileservice.persistence.mongo.model.Application;

public interface ApplicationConverterService {

    /**
     *
     * @param application The application which is going to be converted
     * @return the applicationDto from the original Application
     */
    ApplicationDto convertToApplicationDto(Application application);

    /**
     * converts the application dto application document.
     *
     * @return application document
     */
    Application convertToApplication(ApplicationDto applicationDto);
}
