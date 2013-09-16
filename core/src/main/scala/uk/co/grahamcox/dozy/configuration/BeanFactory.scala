package uk.co.grahamcox.dozy.configuration

import grizzled.slf4j.Logger
/**
 * Base class defining a dependency from one bean to another
 */
abstract class BeanDependency

/**
 * Bean Dependency that is literally a dependency on another named bean
 * @param id The ID of the bean to depend on
 */
case class BeanReference(id: String) extends BeanDependency

/**
 * Definition of the data required to construct a bean
 */
class BeanData(dependencies: Map[String, _]) {
  /**
   * Get the bean with the given name, constructing it first if necessary
   * @param name The name of the bean to get
   * @return the constructed bean, or None if it can't be found
   */
  def get[T](name: String): Option[T] = dependencies.get(name) map { a => a.asInstanceOf[T] }
}

/**
 * A fully formed Bean Definition
 * @param beanName the name of the bean
 * @param beanType the type of the bean
 * @param dependencies the dependencies of the bean
 * @param constructor the function to construct the bean
 */
case class BeanDefinition[T](beanName: String, 
  beanType: Manifest[T], 
  dependencies: Map[String, BeanDependency], 
  constructor: BeanData => Option[T])

/**
 * Representation of a factory of beans
 * @param parent A parent bean factory if there is one
 */
class BeanFactory(parent: Option[BeanFactory] = None, beanDefinitions: Seq[BeanDefinition[_]] = Nil) {
  /** The logger to use */
  private val logger = Logger[this.type]

  /** The map of all the bean definitions that we actually know about */
  private val beanDefs: Map[String, BeanDefinition[_]] = Map(beanDefinitions map {
    bd => bd.beanName -> bd
  }: _*)

  /**
   * Get the bean with the given name, either from this factory or it's parent
   * @param bean The name of the bean
   * @return the bean, or None if it couldn't be built by this factory
   * @throws ClassCaseException if the bean requested is of the wrong type
   */
  def get[T](bean: String): Option[T] = {
    val beanDefinition: Option[BeanDefinition[_]] = getBeanDefinition(bean)

    (beanDefinition, parent) match {
      case (Some(definition), _) => definition match {
        case d: BeanDefinition[T] => buildBean[T](d)
        case _ => throw new ClassCastException
      }
      case (None, Some(bf)) => bf.get[T](bean)
      case (None, None) => None
    }
  }

  /**
   * Get a list of all the bean names that can fulfil the provided bean type
   * @param manifest Implicit parameter giving the type of bean to get
   * @return the sequence of bean names that can fulfil this type.
   */
  def getBeansOfType[T](implicit manifest: Manifest[T]): Iterable[String] = {
    logger.debug("Finding beans that fulfil the type %s".format(manifest))
    val filtered = beanDefs filter {
      case (key: String, definition: BeanDefinition[_]) => {
        val beanType = definition.beanType
        // Object.isAssignableFrom(Integer) == True
        // Integer.isAssignableFrom(Object) == False
        logger.debug("Bean %s has type %s".format(key, beanType.runtimeClass))
        manifest.runtimeClass.isAssignableFrom(beanType.runtimeClass)
      }
    }
    val names = filtered map {
      case (key: String, definition: BeanDefinition[_]) => key
    }
    names filter {
      name => defined(name)
    }
  }

  /**
   * Determine if a bean is defined by this bean factory hierarchy
   * @param bean The name of the bean
   * @return the bean, or None if it couldn't be built by this factory
   */
  def defined(bean: String): Boolean = {
    logger.debug("Looking if we can define bean: " + bean)
    val beanDefinition = getBeanDefinition(bean)

    val result = (beanDefinition, parent) match {
      case (Some(beanDef), _) => {
        beanDef.dependencies.foldLeft(true) {
          (b: Boolean, a: (String, BeanDependency)) => {
            b && (a._2 match {
              case BeanReference(id) => defined(id)
              case _ => false
            })
          }
        }
      }
      case (None, Some(bf)) => bf.defined(bean)
      case (None, None) => false
    }
    logger.debug("Can we define bean: " + bean + "? " + result)
    result
  }

  /**
   * Get the bean definition with the given name
   * @param bean The name of the bean
   * @return the bean definition, or None if there isn't one
   */
  protected def getBeanDefinition(bean: String): Option[BeanDefinition[_]] = beanDefs.get(bean)

  /**
   * Actually build the bean that is defined by the given Bean Definition
   * @param definition The definition of the bean to build
   * @return the built Bean, or None if it couldn't be built
   */
  protected def buildBean[T](definition: BeanDefinition[T]): Option[T] = {
    logger.debug("Attempting to build bean: " + definition.beanName)

    val dependencies: Map[String, Any] = definition.dependencies flatMap {
      case (name: String, dep: BeanDependency) => {
        val bean = dep match {
          case BeanReference(id) => get(id)
          case _ => None
        }
        logger.debug("Built bean %s as %s".format(name, bean))
        bean map { 
          a: Any => (name -> a) 
        }
      }
    }

    val beanData = new BeanData(dependencies)
    val result = definition.constructor(beanData)

    logger.debug("Built bean: " + definition.beanName + " as " + result)
    result
  }
}
