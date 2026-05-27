Feature: UI Negative Login Validation
  As an application security protocol
  I want to ensure invalid users cannot access the dashboard
  So that user data remains secure

  @UI @TS-02 @TS-03 @FR-09
  Scenario Outline: Verify system rejects invalid login attempts
    Given the user is on the login page
    When the user attempts to login with email "<email>" and password "<password>"
    Then the login should be "unsuccessful"
    And an error message should be displayed on the screen

    Examples:
      | email                    | password |
      | invalid_user@gmail.com   | Wrong123 |
      | Sonwanevinay11@gmail.com |          |
      |                          | @Vinay   |