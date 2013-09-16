package uk.co.grahamcox.dozy.configuration

/**
 * Representation of a factory of beans that are configured by a provided Configuration class
 * @param configuration The configuration of the beans that we can build
 * @param parent A parent bean factory if there is one
 */
class ConfigurationBeanFactory(configuration: Configuration, parent: Option[BeanFactory] = None) extends BeanFactory(parent,
configuration.beanDefinitions) {
}

