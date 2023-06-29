package com.github.searchprofileservice;

import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RestController;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.conditions.ArchConditions.*;

/**
 * General architecture tests
 * 
 * Tests that specific types of classes have correct names, annotations and packages.
 */
@AnalyzeClasses(
  packages = "com.github.searchprofileservice",
  importOptions = ImportOption.DoNotIncludeTests.class)
public class ArchitectureTest {

  /**************************************************************************/
  /* SERVICE CLASSES                                                        */
  /**************************************************************************/

  @ArchTest
  public ArchRule allServicesAreInCorrectPackage = 
    classes().that()
      .areInterfaces()
      .and()
      .haveSimpleNameEndingWith("Service")
      .should()
      .resideInAPackage("com.github.searchprofileservice.service");

  @ArchTest
  public ArchRule onlyServicesAreInServicePackage = 
    classes().that()
      .resideInAPackage("com.github.searchprofileservice.service")
      .should(beInterfaces())
      .andShould(haveSimpleNameEndingWith("Service"));

  @ArchTest
  public ArchRule allServiceImplsAreInCorrectPackage = 
    classes().that()
      .areAnnotatedWith(Service.class)
      .and()
      .haveSimpleNameEndingWith("ServiceImpl")
      .should()
      .resideInAPackage("com.github.searchprofileservice.service.impl");

  @ArchTest
  public ArchRule onlyServiceImplsAreInServiceImplPackage = 
    classes().that()
      .resideInAPackage("com.github.searchprofileservice.service.impl")
      .should(beAnnotatedWith(Service.class))
      .andShould(haveSimpleNameEndingWith("ServiceImpl"));


  /**************************************************************************/
  /* CLIENT CLASSES                                                         */
  /**************************************************************************/

  @ArchTest
  public ArchRule allClientsAreInCorrectPackage = 
    classes().that()
      .areInterfaces()
      .and()
      .haveSimpleNameEndingWith("Client")
      .should()
      .resideInAPackage("com.github.searchprofileservice.client");

  @ArchTest
  public ArchRule onlyClientsAreInClientPackage = 
    classes().that()
      .resideInAPackage("com.github.searchprofileservice.client")
      .should(beInterfaces())
      .andShould(haveSimpleNameEndingWith("Client"));

  @ArchTest
  public ArchRule allClientImplsAreInCorrectPackage = 
    classes().that()
      .areAnnotatedWith(Component.class)
      .and()
      .haveSimpleNameEndingWith("ClientImpl")
      .should()
      .resideInAPackage("com.github.searchprofileservice.client.impl");

  @ArchTest
  public ArchRule onlyClientImplsAreInClientImplPackage = 
    classes().that()
      .resideInAPackage("com.github.searchprofileservice.client.impl")
      .should(beAnnotatedWith(Component.class))
      .andShould(haveSimpleNameEndingWith("ClientImpl"));


  /**************************************************************************/
  /* CONTROLLER CLASSES                                                     */
  /**************************************************************************/

  /**
   * A predicate which checks that a class is a controller
   */
  public static DescribedPredicate<JavaClass> areControllers =
    new DescribedPredicate<>("are controllers") {
      @Override
      public boolean test(JavaClass t) {
        return t.isAnnotatedWith(Controller.class) || t.isAnnotatedWith(RestController.class);
      }
    };

  /**
   * A condition which enforces that a class is a controller
   */
  public static ArchCondition<JavaClass> beControllers = new ArchCondition<>("be controllers") {
    @Override
    public void check(JavaClass item, ConditionEvents cond) {
      if (
        !item.isAnnotatedWith(Controller.class) &&
        !item.isAnnotatedWith(RestController.class)
      ) {
        String message =
          String.format(
            "Class %s is neither annotated with @Controller nor with @RestController",
            item.getName());
        cond.add(SimpleConditionEvent.violated(item, message));
      }
    }
  }; 

  @ArchTest
  public ArchRule allControllersAreNamedCorrectly = 
    classes().that(areControllers)
      .should()
      .haveSimpleNameEndingWith("Controller");

  @ArchTest
  public ArchRule allControllersAreInCorrectPackage = 
    classes().that(areControllers)
      .should()
      .resideInAPackage("com.github.searchprofileservice.api");

  @ArchTest
  public ArchRule onlyControllersAreInControllerPackage = 
    classes().that()
      .areTopLevelClasses()
      .and()
      .resideInAPackage("com.github.searchprofileservice.api")
      .should(beControllers)
      .andShould(haveSimpleNameEndingWith("Controller"));


  /**************************************************************************/
  /* CONTROLLER ADVICE CLASSES                                              */
  /**************************************************************************/

  @ArchTest
  public ArchRule allControllerAdvicesAreNamedCorrectlyAndInCorrectPackage = 
    classes().that()
      .areAnnotatedWith(ControllerAdvice.class)
      .should(haveSimpleNameEndingWith("ControllerAdvice"))
      .andShould(resideInAPackage("com.github.searchprofileservice.api.advice"));

  @ArchTest
  public ArchRule onlyControllerAdvicesAreInControllerAdvicePackage = 
    classes().that()
      .resideInAPackage("com.github.searchprofileservice.api.advice")
      .should(haveSimpleNameEndingWith("ControllerAdvice"))
      .andShould(beAnnotatedWith(ControllerAdvice.class));


  /**************************************************************************/
  /* MONGO DOCUMENT CLASSES                                                 */
  /**************************************************************************/

  @ArchTest
  public ArchRule allMongoDocumentsAreInCorrectPackage = 
    classes().that()
      .areTopLevelClasses()
      .and()
      .areAnnotatedWith(Document.class)
      .should(resideInAPackage("com.github.searchprofileservice.persistence.mongo.model"));

  @ArchTest
  public ArchRule onlyMongoDocumentsAreInMongoDocumentPackage = 
    classes().that()
      .areTopLevelClasses()
      .and()
      .resideInAPackage("com.github.searchprofileservice.persistence.mongo.model")
      .should(beAnnotatedWith(Document.class));


  /**************************************************************************/
  /* MONGO REPOSITORY CLASSES                                               */
  /**************************************************************************/

  @ArchTest
  public ArchRule allMongoRepositoriesAreInCorrectPackage = 
    classes().that()
      .areAssignableTo(MongoRepository.class)
      .should(haveSimpleNameEndingWith("Repository"))
      .andShould(resideInAPackage("com.github.searchprofileservice.persistence.mongo.repository"));

  @ArchTest
  public ArchRule onlyMongoRepositoriesAreInMongoRepositoryPackage = 
    classes().that()
      .resideInAPackage("com.github.searchprofileservice.persistence.mongo.repository")
      .should(beAssignableTo(MongoRepository.class))
      .andShould(haveSimpleNameEndingWith("Repository"));
}
