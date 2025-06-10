package dev.sbr.customer.rest;

final class Examples {
  private Examples() {

  }

	static final String VALID_EXAMPLE_CUSTOMER = """
		{
			"id": 1,
			"customerName": "Acme Corporation",
			"customerAddress": "123 Main St"
		}
		""";

	static final String VALID_EXAMPLE_CUSTOMER_TO_CREATE = """
    {
			"customerName": "Acme Corporation",
			"customerAddress": "123 Main St"
		}
		""";

	static final String VALID_EXAMPLE_CUSTOMER_LIST = "[" + VALID_EXAMPLE_CUSTOMER + "]";


}
