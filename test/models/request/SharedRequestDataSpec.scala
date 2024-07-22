package models.request

import base.BaseSpec
import models.requests.SharedRequestData
import org.scalatest.EitherValues

class SharedRequestDataSpec extends BaseSpec with Generators with EitherValues {
  "API JSON reader" should {
    "successfully read valid API JSON" in forAll(validSharedDataModels) { expectedSharedDataModel =>
      val validJson = getJsonFrom(expectedSharedDataModel)

      val actualSharedDataModel = validJson.validate[SharedRequestData].asEither.value

      actualSharedDataModel shouldBe expectedSharedDataModel
    }
  }
}
