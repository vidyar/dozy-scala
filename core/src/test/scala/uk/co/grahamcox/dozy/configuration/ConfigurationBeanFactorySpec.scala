package uk.co.grahamcox.dozy.configuration

import org.scalatest._
import org.scalatest.matchers._

class ConfigurationBeanFactorySpec extends FunSpec with Inside with OptionValues with MustMatchers {
  describe("A Configuration driven Bean Factory") {
    describe("with no parent") {
      describe("and a simple configuration") {
        class MyConfiguration extends Configuration {
          (bean[String]("string")
            literal "Hello"
            )

          (bean[String]("other")
            constructed ((bd: BeanData) => {
              Some("World")
            }))

          (bean[Integer]("length")
            depends beanRef("string") as "s"
            depends beanRef("other") as "o"
            constructed ((bd: BeanData) => {
              val string = bd.get[String]("s")
              val other = bd.get[String]("o")
              (string, other) match {
                case (Some(s), Some(o)) => Some((s + o).length)
                case _ => None
              }
            }))
        }
        val beanFactory = new ConfigurationBeanFactory(new MyConfiguration)
        it("should be able to construct literal beans") {
          val bean = beanFactory.get[String]("string")
          bean.value must be("Hello")
        }
        it("should be able to construct non-dependant beans") {
          val bean = beanFactory.get[String]("other")
          bean.value must be("World")
        }
        it("should be able to construct dependant beans") {
          val bean = beanFactory.get[Integer]("length")
          bean.value must be(10)
        }
        it("should be able to list bean names") {
          val names = beanFactory.getBeansOfType[Integer]
          names must not be('empty)
          names must contain("length")
        }
      }
    }
  }
}

