package utils

import base.BaseSpec
import models.requests.LinkRequest
import org.scalatest.EitherValues

class ErrorResponseJsonFactorySpec extends BaseSpec with models.request.Generators with EitherValues {
  "method getJson" should {
    "return expected errorCode and errorDescription" when {
      "LinkRequest JSON is invalid" in
        forAll(linkRequestJsonErrorScenarios) {
          (invalidPayloads, expectedErrorCode) =>
            forAll(invalidPayloads) { payload =>
              val jsErrors = payload.validate[LinkRequest].asEither.left.value

              val errorResponseJson = ErrorResponseJsonFactory getJson jsErrors

              checkErrorJson(errorResponseJson, expectedErrorCode, EXPECTED_400_ERROR_DESCRIPTION)
            }
        }
    }
  }

  private lazy val linkRequestJsonErrorScenarios = Table(
    ("Invalid Payloads", "Expected Error Code"),
    (randomLinkRequestJsonWithMissingChildDob, "E0006"),
    (randomLinkRequestJsonWithInvalidChildDob, "E0023")
  )
}
