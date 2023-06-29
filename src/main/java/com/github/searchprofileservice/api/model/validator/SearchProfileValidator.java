package com.github.searchprofileservice.api.model.validator;

import com.github.searchprofileservice.api.model.SearchProfileDto;
import com.github.searchprofileservice.api.model.validator.SearchProfileValidator.ValidationResult;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

public interface SearchProfileValidator extends Function<SearchProfileDto, ValidationResult> {

  static SearchProfileValidator isProfileIdValid() {
    return searchProfile -> StringUtils.isBlank(searchProfile.getProfileId())
        ? ValidationResult.SUCCESS : ValidationResult.NO_VALID_PROFILE_ID;
  }

  static SearchProfileValidator isApplicationIdValid() {
    return searchProfile -> (!Objects.isNull(searchProfile.getApplicationId()) &&
        !StringUtils.isEmpty(searchProfile.getApplicationId().toString())) ?
        ValidationResult.SUCCESS : ValidationResult.NO_VALID_APPLICATION_ID;
  }

  static SearchProfileValidator isAnalyserValid() {
    return searchProfile -> (!Objects.isNull(searchProfile.getAnalyser())) ?
            ValidationResult.SUCCESS : ValidationResult.NO_VALID_FAULTTOLERANCE;
  }

  static SearchProfileValidator isEditorIdValid() {
    return searchProfile -> !StringUtils.isBlank(searchProfile.getLastEditorId()) ?
        ValidationResult.SUCCESS : ValidationResult.NO_VALID_EDITOR_ID;
  }

  static SearchProfileValidator isNameValid() {
    return searchProfile -> !StringUtils.isBlank(searchProfile.getName()) ?
        ValidationResult.SUCCESS : ValidationResult.NAME_NOT_VALID;
  }

  /**
   * Returns a function to validate a SearchProfileDto's fields
   * 
   * Checks if no field's boost is below zero
   * @return SearchProfileDto validator function
   */
  static SearchProfileValidator areFieldsValid() {
    return searchProfile -> {
      final boolean fieldBoostIsNull =
        Optional.ofNullable(searchProfile.getSearchFields())
          .orElse(Collections.emptyList())
          .stream()
          .anyMatch(field -> null == field || null == field.getBoost());

      if (fieldBoostIsNull) {
        return ValidationResult.FIELD_BOOST_NULL;
      }

      final boolean fieldBoostIsNegative =
        Optional.ofNullable(searchProfile.getSearchFields())
          .orElse(Collections.emptyList())
          .stream()
          .anyMatch(field -> field.getBoost() < 0);

      if (fieldBoostIsNegative) {
        return ValidationResult.FIELD_BOOST_NEGATIVE;
      }

      return ValidationResult.SUCCESS;
    };
  }

  default SearchProfileValidator and(SearchProfileValidator otherValidator) {
    return searchProfile -> {
      ValidationResult result = this.apply(searchProfile);
      return result.equals(ValidationResult.SUCCESS) ? otherValidator.apply(searchProfile) : result;
    };
  }

  @Getter
  enum ValidationResult {
    SUCCESS(""),
    NAME_NOT_VALID("'name' must not be null or empty."),
    NO_VALID_APPLICATION_ID("'applicationID must not be null or empty.'"),
    NO_VALID_PROFILE_ID("'profileID' must not be null or empty."),
    NO_VALID_EDITOR_ID("'editorID' must not be null or empty."),
    FIELD_BOOST_NEGATIVE("'Field boost must not be negative'"),
    NO_VALID_FAULTTOLERANCE("faultTolerant must be a boolean value"),
    FIELD_BOOST_NULL("'Field boost must not be null'");

    private final String label;

    ValidationResult(String label){
      this.label = label;
    }
  }
}
