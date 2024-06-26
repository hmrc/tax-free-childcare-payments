package base

import play.api.libs.json.{JsObject, Json}

import java.time.LocalDate

trait JsonGenerators extends Generators {
  import org.scalacheck.Gen

  protected val validLinkAccountsRequestPayloads: Gen[JsObject] =
    for {
      eppAuthPayload <- validSharedPayloads
      childAgeDays   <- Gen.chooseNum(0, 18 * 365)
    } yield eppAuthPayload ++ Json.obj(
      "child_date_of_birth" -> (LocalDate.now() minusDays childAgeDays)
    )

  private lazy val validSharedPayloads =
    for {
      account_ref <- nonEmptyAlphaNumStrings
      epp_urn     <- nonEmptyAlphaNumStrings
      epp_account <- nonEmptyAlphaNumStrings
    } yield Json.obj(
      "outbound_child_payment_ref" -> account_ref,
      "epp_registration_reference" -> epp_urn,
      "epp_unique_customer_id"     -> epp_account
    )
}
