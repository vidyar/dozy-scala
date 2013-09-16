package uk.co.grahamcox.dozy.configuration

import grizzled.slf4j.Logger
import java.util.UUID

class BeanDepEntry[T](builder: BeanDefinitionBuilder[T], dep: BeanDependency) {
  /**
   * Register the bean dependency with the builder under the given name
   * @param name The name to register the dependency as
   * @return The builder to use
   */
  def as(name: String): BeanDefinitionBuilder[T] = {
    builder.addDependency(name, dep)
    builder
  }
}

/**
 * Builder to use to define a bean in the configuration
 * @param beanName The name of the bean
 */
class BeanDefinitionBuilder[T: Manifest](val beanName: String) {
  /** The logger to use */
  private val logger = Logger[this.type]
  /** The type of the bean */
  val beanType = manifest[T]

  /** The constructor function to use */
  var constructor: BeanData => Option[T] = (bd: BeanData) => None
  /** The dependencies of the bean */
  var dependencies: Map[String, BeanDependency] = Map.empty[String, BeanDependency]

  logger.debug("Creating bean builder for bean name " + beanName + " of type " + beanType)

  /**
   * Define a new dependency from this bean on another one
   * @param dep The dependency definition
   * @return this, for chaining
   */
  def depends(dep: BeanDependency): BeanDepEntry[T] = {
    logger.debug("Bean " + beanName + " depends on " + dep)
    new BeanDepEntry(this, dep)
  }

  /**
   * Actually add the depedency to the bean definition
   * @param name The name of the dependency
   * @param dep The dependency
   */
  def addDependency(name: String, dep: BeanDependency) {
    dependencies ++= Map(name -> dep)
  }
  /**
   * Define the actual mechanism to construct this bean
   * @param f The function that will construct this bean
   */
  def constructed(f: BeanData => Option[T]): this.type = {
    logger.debug("Bean " + beanName + " constructed by " + f)
    this.constructor = f
    this
  }

  /**
   * Define the bean as having a literal value instead of a constructor
   * @param value The literal value
   */
  def literal(value: T): this.type = {
    logger.debug("Bean " + beanName + " has literal value " + value)
    this.constructor = (bd: BeanData) => Some(value)
    this
  }

  /**
   * Build the bean definition that we have been describing
   * @return the bean definition
   */
  def build: BeanDefinition[T] = BeanDefinition[T](beanName, beanType, dependencies, constructor)
}

/**
 * Trait that is used for writing Configuration classes
 */
trait Configuration {
  /** The set of bean definitions that exist */
  private var beanDefs: Seq[BeanDefinitionBuilder[_]] = Nil

  /**
   * Get all of the bean definitions
   * @return the bean definitions
   */
  def beanDefinitions: Seq[BeanDefinition[_]] = beanDefs.map {
    builder => builder.build
  }
  /**
   * Provide the definition of a bean in the configuration
   * @param beanName the name of the bean
   */
  def bean[T: Manifest](beanName: String): BeanDefinitionBuilder[T] = {
    val builder = new BeanDefinitionBuilder[T](beanName)
    beanDefs ++= Seq(builder)
    builder
  }

  /**
   * Provide the definition of a bean in the configuration
   */
  def bean[T: Manifest](): BeanDefinitionBuilder[T] = bean[T](UUID.randomUUID().toString())

  /**
   * Produce a dependency that is a reference to another bean by name
   * @param id The ID of the bean to depend on
   * @return the dependency definition
   */
  def beanRef(id: String): BeanDependency = BeanReference(id)
}
