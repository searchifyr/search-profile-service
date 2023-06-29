package com.github.searchprofileservice.service;

import com.github.searchprofileservice.persistence.mongo.model.Application;
import com.github.searchprofileservice.service.impl.ApplicationConverterServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ApplicationConverterServiceTest {

    private final ElasticSearchClientService elasticSearchService = mock(ElasticSearchClientService.class);

    @InjectMocks
    private final ApplicationConverterService applicationconverter
            = new ApplicationConverterServiceImpl(elasticSearchService);

    @Test
    void convertToApplicationDto(){
        Application app = new Application(
                UUID.randomUUID(),
                Calendar.getInstance().getTime(),
                new ArrayList<>(),
                "app1",
                "some dude",
                false,
                List.of("111")
        );

        long zero = 0;
        when(elasticSearchService.getDocumentCountForIndex(any(String.class))).thenReturn(zero);

        var result = applicationconverter.convertToApplicationDto(app);

        assertThat(result.getId(), equalTo(app.getId()));
        assertThat(result.getCreatorId(), equalTo(app.getCreatorId()));
        assertThat(result.getName(), equalTo(app.getApplicationName()));
        assertThat(result.isActive(), equalTo(app.isActive()));
        assertThat(result.getNumberOfDocuments(), equalTo(zero));
    }

    @Test
    void convertToApplication(){
        Application app = new Application(
                UUID.randomUUID(),
                Calendar.getInstance().getTime(),
                new ArrayList<>(),
                "app1",
                "some dude",
                false,
                List.of("111")
        );

        long zero = 0;
        when(elasticSearchService.getDocumentCountForIndex(any(String.class))).thenReturn(zero);

        var dto = applicationconverter.convertToApplicationDto(app);

        var result = applicationconverter.convertToApplication(dto);

        assertThat(result.getId(), equalTo(app.getId()));
        assertThat(result.getApplicationName(), equalTo(app.getApplicationName()));
        assertThat(result.isActive(), equalTo(app.isActive()));

    }

}
