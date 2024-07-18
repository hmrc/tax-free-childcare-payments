package models.response

import base.BaseSpec
import models.response.TfcErrorResponse2.{ETFC1, ETFC2}
import org.apache.pekko.actor.ActorSystem
import org.scalatest.prop.TableDrivenPropertyChecks
import play.api.http.Status
import play.api.mvc.AnyContentAsEmpty
import play.api.test.FakeRequest

class TfcErrorResponse2Spec extends BaseSpec with TableDrivenPropertyChecks with Status {
  private implicit val as: ActorSystem = ActorSystem()

  "method toResult" should {
    "convey data within model" in
      forAll(errorScenarios) {
        checkErrorResult
      }
  }

  private lazy val errorScenarios = Table(
    ("Error Response", "Expected Status", "Expected Error Code", "Expected Error Description"),
    (ETFC1.toResult, BAD_REQUEST, "ETFC1", EXPECTED_400_ERROR_DESCRIPTION),
    (ETFC2.toResult, INTERNAL_SERVER_ERROR, "ETFC2", EXPECTED_500_ERROR_DESCRIPTION)
  )

  private lazy implicit val req: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
}
