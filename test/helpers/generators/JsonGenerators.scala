/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package helpers.generators

import helpers.json.JsValueConversions
import org.scalacheck.Gen
import play.api.libs.json.Json.JsValueWrapper
import play.api.libs.json.{JsObject, Json}

trait JsonGenerators extends JsValueConversions {

  protected def jsObjects(properties: (String, Gen[ToJsValue])*): Gen[JsObject] = {
    val seqOfPropertyGens: Seq[Gen[(String, JsValueWrapper)]] =
      properties
        .map { case (key, gen) => gen.map(value => key -> toJsValue(value)) }

    val objectGenerator: Gen[Seq[(String, JsValueWrapper)]] = Gen.sequence(seqOfPropertyGens)

    for {
      fields <- objectGenerator
    } yield Json.obj(fields*)
  }

  extension (gen: Gen[JsObject]) {
    infix def -(key: String): Gen[JsObject] = gen.map(_ - key)

    infix def +(entryGen: (String, Gen[ToJsValue])): Gen[JsObject] = for {
      original <- gen
      key = entryGen._1
      value <- entryGen._2
    } yield original + (key -> toJsValue(value))

    infix def ++(otherObject: JsObject): Gen[JsObject] = gen.map(_ ++ otherObject)

    infix def ++(otherGen: Gen[JsObject]): Gen[JsObject] = for {
      first  <- gen
      second <- otherGen
    } yield first ++ second

  }

}
