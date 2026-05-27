Feature: Notes API Backend Validation

  @API @TS-API-01
  Scenario Outline: Verify POST login API generates an auth token
    When the user sends a POST login request with email "<email>" and password "<password>"
    Then the response status should be 200
    And the response should contain a valid auth token

    Examples:
      | email                    | password |
      | Sonwanevinay11@gmail.com | @Vinay   |

  @API @TS-API-02 @FR-08
  Scenario Outline: Verify GET notes API retrieves data successfully within SLA
    Given the user is authenticated via the API
    When the user requests to fetch all notes with seed title "<seed_title>"
    Then the API response should contain the note "<seed_title>"

    Examples:
      | seed_title          |
      | Pure API Validation |

  @API @TS-API-03
  Scenario Outline: Verify POST notes API creates a note successfully
    Given the user is authenticated via the API
    When the user creates a note via API with title "<title>" description "<description>" and category "<category>"
    Then the API should return status 200 and the note should exist

    Examples:
      | title                        | description                        | category |
      | The Great Gatsby - 1001      | A novel about the American dream   | Work     |
      | To Kill a Mockingbird - 1002 | A story about justice and morality | Personal |

  @API @TS-API-04
  Scenario Outline: Verify PUT notes API updates an entire note
    Given the user is authenticated via the API
    And a note is created via the API with title "<original_title>" description "<original_desc>" category "<category>"
    When the user sends a PUT request to update with title "<updated_title>" and description "<updated_desc>"
    Then the API should return 200 and reflect the updated values

    Examples:
      | original_title          | original_desc         | category | updated_title            | updated_desc           |
      | Original Note - 2001    | Original description  | Personal | Updated Note - 2001      | Updated description    |
      | Another Note - 2002     | Second description    | Home     | Another Updated - 2002   | Second updated desc    |

  @API @TS-API-05
  Scenario Outline: Verify PATCH notes API updates note category only
    Given the user is authenticated via the API
    And a note is created via the API with title "<title>" description "<description>" category "<original_category>"
    When the user sends a PATCH request to update the category to "<new_category>"
    Then the API should return 200 and only the category should be changed to "<new_category>"

    Examples:
      | title                  | description           | original_category | new_category |
      | Patch Test Note - 3001 | Description for patch | Personal          | Home         |
      | Patch Test Note - 3002 | Another patch note    | Work              | Personal     |

  @API @TS-API-06
  Scenario Outline: Verify GET notes by ID returns the correct note
    Given the user is authenticated via the API
    And a note is created via the API with title "<title>" description "<description>" category "<category>"
    When the user sends a GET request for that specific note ID
    Then the API should return 200 and the note data should match

    Examples:
      | title                  | description             | category |
      | Get By ID Note - 4001  | Description for get     | Work     |
      | Get By ID Note - 4002  | Another get by id note  | Home     |

  @API @TS-NEG-04 @FR-09
  Scenario Outline: Verify POST notes API rejects missing required fields
    Given the user is authenticated via the API
    When the user sends a POST notes request with missing title but description "<description>" and category "<category>"
    Then the API should return a 400 Bad Request status

    Examples:
      | description              | category |
      | Description without title| Work     |
      | Another missing title    | Home     |
