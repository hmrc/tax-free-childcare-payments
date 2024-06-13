package connectors

import base.BaseISpec

class NsiConnectorISpec extends BaseISpec {
  private val connector = app.injector.instanceOf[NsiConnector]

  "method linkAccounts" should {
    s"respond $OK with a defined LinkResponse" when {
      s"NSI responds $CREATED with expected JSON format" in {

      }
    }
  }

  "method checkBalance" should {
    s"respond $OK with a defined BalanceResponse" when {
      s"NSI responds $OK with expected JSON format" in {

      }
    }
  }

  "method makePayment" should {
    s"respond $OK with a defined PaymentResponse" when {
      s"NSI responds $CREATED with expected JSON format" in {

      }
    }
  }
}
