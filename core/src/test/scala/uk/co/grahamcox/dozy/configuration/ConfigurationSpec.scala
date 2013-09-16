package uk.co.grahamcox.dozy.configuration

import org.scalatest._
import org.scalatest.matchers._

class ConfigurationSpec extends FlatSpec with Inside with MustMatchers {
  "A Configuration" should "be able to define beans" in {
    class MyConfiguration extends Configuration {
      (bean[String]("string")
        literal "Hello"
        )

      (bean[String]("other")
        depends beanRef("string") as "s"
        constructed ((bd: BeanData) => {
          bd.get[String]("string") map { _ + " World" }
        }))

      (bean[Integer]()
        depends beanRef("other") as "o"
        constructed ((bd: BeanData) => {
          bd.get[String]("other") map { _.length }
        }))
    }

    val c = new MyConfiguration
    val beanDefs = c.beanDefinitions
    beanDefs must have size(3)

    inside(beanDefs) {
      case Seq(string, other, integer) => 
        inside (string) { case BeanDefinition(beanName, beanType, dependencies, constructor) => 
          beanName must be("string")
          beanType must be(manifest[String])
          dependencies must have size(0)
        }
        inside(other) { case BeanDefinition(beanName, beanType, dependencies, constructor) =>
          beanName must be("other")
          beanType must be(manifest[String])
          dependencies must have size(1)
          inside(dependencies.get("s")) { case Some(stringDependency) =>
            inside(stringDependency) { case BeanReference(stringId) =>
              stringId must be("string")
            }
          }
        }
        inside(integer) { case BeanDefinition(beanName, beanType, dependencies, constructor) =>
          beanType must be(manifest[Integer])
          dependencies must have size(1)
          inside(dependencies.get("o")) { case Some(stringDependency) =>
            inside(stringDependency) { case BeanReference(stringId) =>
              stringId must be("other")
            }
          }
        }
    }
  }
}
