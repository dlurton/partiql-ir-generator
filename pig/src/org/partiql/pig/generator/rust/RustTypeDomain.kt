/*
 * Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package org.partiql.pig.generator.rust

import org.partiql.pig.domain.model.*
import org.partiql.pig.util.snakeToCamelCase
import org.partiql.pig.util.snakeToPascalCase

//
// Types in this file are exposed to FreeMarker templates at run-time.
//

data class Name(val snakeCaseName: String) {
    val snake: String get() = this.snakeCaseName.mangleKeyword()
    val pascal: String get() = this.snakeCaseName.snakeToPascalCase() // pascal case can never conflict with a rust keyword
    val camel: String get() = this.snakeCaseName.snakeToCamelCase().mangleKeyword()
}

fun String.asName() = Name(this)

private val KEYWORDS = setOf("let", "match", "return", "fn")

fun String.mangleKeyword() = if(KEYWORDS.contains(this)) { "_$this" } else this

data class RustTypeDomain(
    val name: Name,
    val tuples: List<RustTuple>,
    val sums: List<RustSum>
)

data class RustElement(
    val identifier: Name,
    val tag: Name,
    val builderParameterTypeName: String,
    val structTypeName: String,
    val isVariadic: Boolean,
    val isOptional: Boolean
)

data class RustTuple(
    val tag: Name,
    val memberOfType: Name?,
    val elements: List<RustElement>,
    val arity: IntRange,
    val tupleType: TupleType
) {
    /** All of the elements excluding the variadic element. */
    @Suppress("unused")
    val monadicElements = elements.filter { !it.isVariadic }

    /** There may be only one variadic element and if it's present, it's here. */
    @Suppress("MemberVisibilityCanBePrivate")
    val variadicElement = elements.singleOrNull { it.isVariadic }

    /** True when there's a variadic element. */
    @Suppress("unused")
    val hasVariadicElement = variadicElement != null
}

data class RustSum(
    val name: Name,
    val variants: List<RustTuple>
)

fun TypeDomain.toRustTypeDomain(): RustTypeDomain {
    val gTuples = mutableListOf<RustTuple>()
    val gSums = mutableListOf<RustSum>()

    this.userTypes.forEach {
        when(it) {
            is DataType.UserType.Tuple -> gTuples.add(it.toCTuple(memberOfType = null))
            is DataType.UserType.Sum -> gSums.add(it.toCTuple())
        }
    }

    return RustTypeDomain(
        name = this.tag.asName(),
        tuples = gTuples,
        sums = gSums
    )
}

private fun DataType.UserType.Tuple.toCTuple(memberOfType: String?) =
    RustTuple(
        tag = this.tag.asName(),
        memberOfType = memberOfType?.asName(),
        elements = this.namedElements.map { it.toRustElement() },
        arity = this.computeArity(),
        tupleType = this.tupleType
    )

private fun NamedElement.toRustElement(): RustElement {
    val builderParameterTypeName = when(val tn = this.typeReference.typeName) {
        "ion" -> "IonPrimitive"
        "int" -> "IntPrimitive"
        "bool" -> "BoolPrimitive"
        "symbol" -> "SymbolPrimitive"
        else -> tn.snakeToPascalCase()
    }
    val typeName = "Box<$builderParameterTypeName>"
    val (isOptional, isVariadic, structTypeName) = when(this.typeReference.arity) {
        Arity.Required -> Triple(false, false, typeName)
        Arity.Optional -> Triple(true, false, "Option<$typeName>")
        is Arity.Variadic -> Triple(false, true, "Vec<$typeName>")
    }

    return RustElement(
        identifier = this.identifier.asName(),
        tag = this.tag.asName(),
        builderParameterTypeName = builderParameterTypeName,
        structTypeName = structTypeName,
        isOptional = isOptional,
        isVariadic = isVariadic
    )
}


private fun DataType.UserType.Sum.toCTuple() =
    RustSum(
        name = this.tag.asName(),
        variants = this.variants.map { it.toCTuple(this.tag) }
    )
