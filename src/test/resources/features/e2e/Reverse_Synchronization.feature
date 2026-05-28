
Feature: Reverse E2E Synchronization
  As a full stack application
  I want the UI dashboard to dynamically reflect API database deletions
  So that users always see accurate data

  @Hybrid @TS-DEF-02 @FR-07
  Scenario Outline: Verify note deleted via API disappears from UI
    Given the user is logged into the Notes UI with valid credentials
    And a note is created via the API with title "<title>" description "<description>" and category "<category>"
    When the user deletes that specific note via the API
    And the user refreshes the UI dashboard
    Then the deleted note should no longer be visible on the screen

    Examples:
      | title                      | description                     | category |
      | Reverse Sync Note - 8001   | Note to be deleted via API      | Personal |
      | Reverse Sync Note - 8002   | Another note deleted via API    | Work     |
