/*
 * Copyright 2024 HM Revenue & Customs
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

package utils

import play.api.Logging
import play.api.mvc.RequestHeader

trait FormattedLogging extends Logging {
  import utils.FormattedLogging.{CORRELATION_ID, endpoints}

  def formattedLog(msg: String)(implicit req: RequestHeader): String = {

    val endpoint      = endpoints.getOrElse(req.uri, s"${req.method} ${req.uri}")
    val correlationId = req.headers.get(CORRELATION_ID).orNull

    s"[Error] - [$endpoint] - [$correlationId: $msg]"
  }

}

object FormattedLogging {
  val CORRELATION_ID = "Correlation-ID"

  private val endpoints = Map(
    s"/link"    -> "link",
    s"/balance" -> "balance",
    s"/"        -> "payment"
  )

}
