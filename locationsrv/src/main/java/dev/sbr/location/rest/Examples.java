package dev.sbr.location.rest;

final class Examples {
    private Examples() {

    }

    static final String VALID_EXAMPLE_LOCATION = """
    {
      "id": 1,
      "name": "location 1"
    }
    """;

    public static final String VALID_EXAMPLE_LOCATION_TO_CREATE =
    """
        {
          "name": "location 1"
        }
        """;


    static final String VALID_EXAMPLE_LOCATION_LIST = "[" + VALID_EXAMPLE_LOCATION + "]";
}
