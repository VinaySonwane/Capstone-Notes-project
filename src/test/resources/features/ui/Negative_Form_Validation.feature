Feature: UI Negative Form Validation
  As a user interface
  I want to prevent the submission of empty or invalid forms
  So that the database doesn't get corrupted with bad data

  @UI @TS-15 @TS-18 @FR-09
  Scenario: Verify note creation fails with missing required fields
    Given the user is logged into the Notes UI with valid credentials
    When the user tries to create a note with an empty title
    Then a validation error should appear on the screen preventing submission