package models.response

import base.BaseSpec
import play.api.libs.json.Json

class NsiErrorResponseSpec extends BaseSpec {
  "JSON reader" should {
    "not throw a NullPointerException" when {
      "given an unknown errorCode" in {
        val json = Json.obj("errorCode" -> "UNKNOWN", "errorDescription" -> "--missing-error-description--")

        /** This statement is required to reproduce the [[NullPointerException]]. */
        val _ = NsiErrorResponse.E0000

        assert(json.validate[NsiErrorResponse].isSuccess)
      }
    }
  }
}
