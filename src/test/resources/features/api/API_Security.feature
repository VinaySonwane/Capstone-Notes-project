Feature: API Security and Error Handling
  As a backend system
  I want to enforce token authentication and handle bad requests gracefully
  So that endpoints are secure

  @API @TS-NEG-03 @FR-09
  Scenario Outline: Verify unauthorized access is blocked
    When a GET request is sent to "<endpoint>" without an auth token
    Then the API should return a 401 Unauthorized status

    Examples:
      | endpoint |
      | /notes   |

  @API @TS-NEG-05 @FR-09
  Scenario Outline: Verify deletion of invalid note ID is handled
    Given the user is authenticated via the API
    When a DELETE request is sent for an invalid note ID "<invalid_id>"
    Then the API should return a 400 or 404 error status code

    Examples:
      | invalid_id    |
      | 999999999404  |
      | 000000000000  |
