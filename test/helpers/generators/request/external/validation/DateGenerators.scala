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

package helpers.generators.request.external.validation

import models.request.external.validation.Date
import org.scalacheck.Gen

import java.time.{LocalDate, ZoneId}
import scala.util.Try

trait DateGenerators {

  private val MIN_YEAR = 2000
  private val MAX_YEAR = 3000

  protected val genLocalDates: Gen[LocalDate] = for {
    calendar <- Gen.calendar
    date = calendar.toInstant.atZone(ZoneId.systemDefault()).toLocalDate
    year <- Gen.chooseNum(MIN_YEAR, MAX_YEAR)
  } yield date.withYear(year)

  protected val genIso8601DateStrings: Gen[String] = for {
    date <- genLocalDates
  } yield date.toString

  protected val genDates: Gen[Date] = for {
    date <- genLocalDates
  } yield Date(date)

  protected val genInvalidIso8601DateStrings: Gen[String] =
    Gen.asciiPrintableStr.filter(string => Try(LocalDate.parse(string)).isFailure)

  protected val genNonStringDates: Gen[Long] = Gen.long

}
