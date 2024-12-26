package com.github.rssh.appcontext.doc

import com.github.rssh.appcontext.{AppContextProvider, AppContextProviders}

import scala.annotation.experimental
import scala.collection.mutable.ArrayBuffer
import scala.quoted.*


@experimental
object DependencyGraph {

  /**
   *
   * @param value
   * @param children choldren,which reflect dependecies of the value.
   */
  case class Node(value: String, children: Seq[Node] = Nil)


  type Nodes = Seq[Node]


  inline def apply[Xs <: NonEmptyTuple]: Seq[Node] =
    ???

  def buildNodesFor[T:Type](using Quotes)(typeParams: Map[Symbol,quotes.reflect.TypeRepr],
                                          usedTypes:Map[quotes.reflect.Symbol,String]): Expr[Seq[Node]] = {
    import quotes.reflect.*
    val tpe = TypeRepr.of[T]
    tpe.classSymbol match
      case None => report.throwError(s"Cannot find class symbol for ${tpe.show}")
      case Some(cs) =>
        val primaryConstructor = cs.primaryConstructor
        // extract all  AppContextProvider for the type
        var paramTypeArs: ArrayBuffer[Symbol] = ArrayBuffer.empty
        var childrenTypes: ArrayBuffer[TypeRepr] = ArrayBuffer.empty
        for { params <- primaryConstructor.paramSymss
              param <- params
              paramType = param.info
            } {
          if (param.isType) {
            paramTypeArs += param
            childrenTypes += paramType
          } else if (param.isTerm) {
            val args =
              if (paramType <:< TypeRepr.of[AppContextProvider[?]]) {
                val arg = paramType.typeArgs.head
                List(arg)
              } else if (paramType <:< TypeRepr.of[AppContextProviders[?]]) {
                ???
                //val args = extractTupleTypes(paramType)
              } else {
                List.empty
              }
            for(arg <- args) {
               ???
            }
          }
        }

    /*
    val childrenNodes = childrenTypes.map { child =>
      val childNodes = buildNodesFor(child)
      '{ Node(${Expr(child.show)}, ${childNodes}) }
    }
    '{ Seq(${childrenNodes}: _*) }

     */
        ???
  }

  given FromExpr[Node] with
    def unapply(expr: Expr[Node])(using quotes: Quotes): Option[Node] =
      import quotes.reflect.*
      expr match
        case '{ Node(${ Expr(value) }, ${ Expr(children) }) } =>
          Some(Node(value, children))
        case _ => None

  given ToExpr[Node] with
    def apply(node: Node)(using Quotes): Expr[Node] =
      '{ Node(${Expr(node.value)}, ${Expr(node.children)})  }



}
