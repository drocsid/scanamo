package com.gu.scanamo.error

import cats.{Semigroup, Show}
import cats.data.NonEmptyList
import cats.std.list._
import com.amazonaws.services.dynamodbv2.model.AttributeValue

sealed abstract class DynamoReadError
final case class NoPropertyOfType(propertyType: String, actual: AttributeValue) extends DynamoReadError
final case class TypeCoercionError(t: Throwable) extends DynamoReadError
final case object MissingProperty extends DynamoReadError

final case class PropertyReadError(name: String, problem: DynamoReadError)
final case class InvalidPropertiesError(errors: NonEmptyList[PropertyReadError]) extends DynamoReadError
object InvalidPropertiesError {
  import cats.syntax.semigroup._
  implicit object SemigroupInstance extends Semigroup[InvalidPropertiesError] {
    override def combine(x: InvalidPropertiesError, y: InvalidPropertiesError): InvalidPropertiesError =
      InvalidPropertiesError(x.errors |+| y.errors)
  }
}

object DynamoReadError {
  implicit object ShowInstance extends Show[DynamoReadError] {
    def show(e: DynamoReadError): String = describe(e)
  }

  def describe(d: DynamoReadError): String =  d match {
    case InvalidPropertiesError(problems) => problems.unwrap.map(p => s"'${p.name}': ${describe(p.problem)}").mkString(", ")
    case NoPropertyOfType(propertyType, actual) => s"not of type: '$propertyType' was '$actual'"
    case TypeCoercionError(e) => s"could not be converted to desired type: $e"
    case MissingProperty => "missing"
  }
}