package models.request

import base.BaseSpec
import models.requests.LinkRequest
import org.scalatest.{EitherValues, LoneElement}

class LinkRequestSpec extends BaseSpec with models.request.Generators with EitherValues with LoneElement {
  "API JSON reader" should {
    "report missing child DoB" in
      forAll(randomLinkRequestJsonWithMissingChildDob) { json =>
        val (jsPath, errors) =
          json
            .validate[LinkRequest]
            .asEither
            .left
            .value
            .loneElement

        val errorKey   = jsPath.path.loneElement.toString
        val errorValue = errors.loneElement.message

        errorKey shouldBe "/child_date_of_birth"
        errorValue shouldBe "error.path.missing"
      }

    "report invalid child DoB" in
      forAll(randomLinkRequestJsonWithInvalidChildDob) { json =>
        val (jsPath, errors) =
          json
            .validate[LinkRequest]
            .asEither
            .left
            .value
            .loneElement

        val errorKey   = jsPath.path.loneElement.toString
        val errorValue = errors.loneElement.message

        errorKey shouldBe "/child_date_of_birth"
        errorValue shouldBe "error.expected.date.isoformat"
      }
  }
}
