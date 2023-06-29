package com.github.searchprofileservice.api.advice;

import com.mongodb.MongoWriteException;
import com.github.searchprofileservice.exception.ElasticSearchUnavailableException;
import com.github.searchprofileservice.exception.ErrorDTO;
import com.github.searchprofileservice.exception.IndexNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@ControllerAdvice
public class GeneralControllerAdvice {

  /**
   * Error status 502 bad gateway, if elasticsearch is not reachable.
   */
  @ExceptionHandler(value = {ElasticSearchUnavailableException.class})
  public ResponseEntity<String> elasticSearchNotAvailable(ElasticSearchUnavailableException e) {
    return new ResponseEntity<>(
        "Elasticsearch is currently unavailable", HttpStatus.BAD_GATEWAY);
  }

  /**
   * Handles uncaught MongoWriteExceptions
   * @param e the exception to handle
   * @return Error 400 on index violation, 500 else
   */
  @ExceptionHandler(value = {MongoWriteException.class})
  public ResponseEntity<String> mongoWriteException(MongoWriteException e) {
    if (e.getCode() == 11000 /* E11000 duplicate key error collection */) {
      return new ResponseEntity<>("Database index violated", HttpStatus.BAD_REQUEST);
    } else {
      return new ResponseEntity<>("Database write failed", HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }



  /**
   * Error status 404 not found, if an index was not found in elasticsearch.
   */
  @ExceptionHandler(value = {IndexNotFoundException.class})
  public ResponseEntity<String> indexNotFoundException(IndexNotFoundException e) {
    return new ResponseEntity<>(e.getMessage(), HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler({ResponseStatusException.class})
  public ResponseEntity<ErrorDTO> handleException(ResponseStatusException exception) {
    log.error(exception.getMessage());
    ErrorDTO errorDTO = new ErrorDTO(exception.getReason());

    return ResponseEntity.status(exception.getStatus()).body(errorDTO);
  }

}
