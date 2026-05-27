Feature: Notes UI Functional Validation

  @UI @TS-UI-01
  Scenario Outline: Verify user login behavior
    Given the user is on the login page
    When the user attempts to login with email "<email>" and password "<password>"
    Then the login should be "<status>"

    Examples:
      | email                    | password | status     |
      | Sonwanevinay11@gmail.com | @Vinay   | successful |

  @UI @TS-UI-02
  Scenario Outline: Verify new user registration
    Given the user is on the registration page
    When the user registers with name "<name>" email "<email>" and password "<password>"
    Then the account should be created successfully

    Examples:
      | name           | email                              | password    |
      | Alice Johnson  | test_alice_01@mailinator.com       | Test@Alice1 |
      | Bob Martinez   | test_bob_02@mailinator.com         | Test@Bob202 |

  @UI @TS-UI-04
  Scenario Outline: Verify user can edit an existing note
    Given the user is logged into the Notes UI with valid credentials
    And a note exists with title "<original_title>" description "<original_desc>" category "<category>"
    When the user edits the note with new title "<updated_title>" and description "<updated_desc>"
    Then the updated note should be visible on the dashboard

    Examples:
      | original_title         | original_desc              | category | updated_title              | updated_desc                |
      | My First Note          | This is the original text  | Work     | My First Note - EDITED     | Updated description text    |
      | Shopping Reminder      | Buy groceries this weekend | Home     | Shopping Reminder - EDITED | Groceries updated list      |

  @UI @TS-UI-05
  Scenario Outline: Verify user can delete a note via the UI
    Given the user is logged into the Notes UI with valid credentials
    And a note exists with title "<title>" description "<description>" category "<category>"
    When the user deletes the note via the UI
    Then the note should no longer appear on the dashboard

    Examples:
      | title              | description                  | category |
      | Note To Delete 01  | This note will be deleted    | Work     |
      | Note To Delete 02  | Another note for deletion    | Home     |

  @UI @TS-UI-06
  Scenario Outline: Verify category filter shows only matching notes
    Given the user is logged into the Notes UI with valid credentials
    And a note exists with title "<title>" description "<description>" category "<category>"
    When the user filters the dashboard by category "<category>"
    Then only notes from category "<category>" should be visible

    Examples:
      | title                | description                   | category |
      | Home Filter Note     | Note for home filter test     | Home     |
      | Work Filter Note     | Note for work filter test     | Work     |
      | Personal Filter Note | Note for personal filter test | Personal |

  @UI @TS-UI-07
  Scenario: Verify secure session logout
    Given the user is logged into the Notes UI with valid credentials
    When the user clicks logout
    Then the user should be redirected to the login page
    And accessing the dashboard directly should redirect back to login
