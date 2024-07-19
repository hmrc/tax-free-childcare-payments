package utils

import play.api.libs.json.{JsPath, JsValue, Json, JsonValidationError}

object ErrorResponseJsonFactory {
  def getJson(errors: collection.Seq[(JsPath, collection.Seq[JsonValidationError])]): JsValue = Json.obj()
}
