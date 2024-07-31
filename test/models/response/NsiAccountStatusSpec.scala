package models.response

import models.request.BaseSpec
import play.api.libs.json.{JsString, JsValue, Json}

class NsiAccountStatusSpec extends BaseSpec {
  "writesToApi" should {
    "return expected API JSON" in
      forAll(accountStatusScenarios) {
        (nsiAccountStatus, expectedApiJson) =>
          val actualApiJson = Json.toJson(nsiAccountStatus)

          actualApiJson shouldBe expectedApiJson
      }
  }

  private lazy val accountStatusScenarios = Table[NsiAccountStatus, JsValue](
    ("NSI Account Status", "Expected API JSON"),
    (NsiAccountStatus.ACTIVE, JsString("ACTIVE")),
    (NsiAccountStatus.BLOCKED, JsString("INACTIVE"))
  )
}
