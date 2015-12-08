import testsHelper.GlobalApplicationForModels

import scala.language.postfixOps


class TestGlobal extends GlobalApplicationForModels {

  "Global" must {

    "return the number of hours separating us from 4 A.M." in {
      globalMethods.returnNumberOfHoursBetween4AMAndNow(4) mustBe 0
      globalMethods.returnNumberOfHoursBetween4AMAndNow(1) mustBe 3
      globalMethods.returnNumberOfHoursBetween4AMAndNow(21) mustBe 7
      globalMethods.returnNumberOfHoursBetween4AMAndNow(5) mustBe 23
    }
  }
}
