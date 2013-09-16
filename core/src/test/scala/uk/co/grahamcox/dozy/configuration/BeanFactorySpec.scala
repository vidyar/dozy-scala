package uk.co.grahamcox.dozy.configuration

import org.scalatest._
import org.scalatest.matchers._

class BeanFactorySpec extends FunSpec with Inside with OptionValues with MustMatchers {
  describe("A Bean Factory"){
    describe("with no parent"){
      val parent: Option[BeanFactory] = None
      describe("with no bean definitions"){
        val beanDefinitions: Seq[BeanDefinition[_]] = Nil
        val beanFactory = new BeanFactory(parent, beanDefinitions)
        it("should claim it can't construct a bean"){
          beanFactory.defined("string") must be(false)
        }
        it("should be unable to construct a bean"){
          beanFactory.get[String]("string") must not be('defined)
        }
        it("should have no beans of type String"){
          beanFactory.getBeansOfType[String] must be ('empty)
        }
      }
      describe("with a simple bean definition"){
        val beanDefinitions: Seq[BeanDefinition[_]] = Seq(
          BeanDefinition[Integer]("integer", 
            manifest[Integer], 
            Map.empty[String, BeanDependency], 
            (bd: BeanData) => Some(1))
        )
        val beanFactory = new BeanFactory(parent, beanDefinitions)
        it("should claim it can't construct an unknown bean"){
          beanFactory.defined("string") must be(false)
        }
        it("should be unable to construct an unknown bean"){
          beanFactory.get[String]("string") must not be('defined)
        }
        it("should claim it can construct a known bean"){
          beanFactory.defined("integer") must be(true)
        }
        it("should be able to construct a known bean"){
          beanFactory.get[Integer]("integer").value must be(1)
        }
        it("should have no beans of type String"){
          beanFactory.getBeansOfType[String] must be ('empty)
        }
        it("should have a bean of type Integer"){
          val beanNames = beanFactory.getBeansOfType[Integer]
          beanNames must not be ('empty)
          beanNames must contain("integer")
        }
        it("should have a bean of type Number"){
          val beanNames = beanFactory.getBeansOfType[Number]
          beanNames must not be ('empty)
          beanNames must contain("integer")
        }
      }
      describe("with a dependant bean definition"){
        describe("with the dependency present") {
          val beanDefinitions: Seq[BeanDefinition[_]] = Seq(
            BeanDefinition[String]("string", 
              manifest[String], 
              Map.empty[String, BeanDependency], 
              (bd: BeanData) => Some("Hello")),
            BeanDefinition[Integer]("integer", 
              manifest[Integer], 
              Map("s" -> BeanReference("string")), 
              (bd: BeanData) => bd.get[String]("s") map { a: String => a.length })
          )
          val beanFactory = new BeanFactory(parent, beanDefinitions)
          it("should claim it can construct the bean"){
            beanFactory.defined("integer") must be(true)
          }
          it("should be able to construct the bean"){
            beanFactory.get[Integer]("integer").value must be(5)
          }
        }
        describe("with the depedency missing") {
          val beanDefinitions: Seq[BeanDefinition[_]] = Seq(
            BeanDefinition[Integer]("integer", 
              manifest[Integer], 
              Map("strings" -> BeanReference("string")), 
              (bd: BeanData) => bd.get[String]("string") map { a: String => a.length })
          )
          val beanFactory = new BeanFactory(parent, beanDefinitions)
          it("should claim it can't construct the bean"){
            beanFactory.defined("integer") must be(false)
          }
          it("should be unable to construct the bean"){
            beanFactory.get[Integer]("integer") must not be('defined)
          }
        }
      }
    }
  }
}

