package cows.memcached

import grails.util.Holders
import groovy.transform.CompileStatic
import java.lang.reflect.Field
import java.lang.reflect.Modifier

/**
 * Created by IntelliJ IDEA.
 * User: 33cows.com
 * Date: 10.04.11
 * Time: 12:19
 */

@CompileStatic
class AnnotationFinder {
//  @Deprecated
//  public <T> T find(Class<T> annotationType) {
//    def requestAttributes = RequestContextHolder.requestAttributes // todo grails3 GrailsWebRequest
//    def controllerName = requestAttributes.controllerName
//    def actionName = requestAttributes.actionName
//    return find(annotationType,controllerName,actionName)
//  }

  public static <T> T find(Class<T> annotationType, String controllerName, String actionName) {

    def controller = Holders.grailsApplication.getArtefactByLogicalPropertyName("Controller", controllerName)
    if (!controller) return null
    Class controllerClass = controller.clazz

    def method = controllerClass.declaredMethods.find{ it.name == actionName && Modifier.isPublic(it.modifiers) && it.getAnnotation(annotationType)}
    if (method) return (T) method.getAnnotation(annotationType)

    actionName = actionName ?: (controller['defaultAction'] ?: 'index')

    Field action = null
    try {
      action = controllerClass.getDeclaredField(actionName)
    } catch (all) {}
    (T) action?.getAnnotation(annotationType) ?: controllerClass.getAnnotation(annotationType)
  }

}
