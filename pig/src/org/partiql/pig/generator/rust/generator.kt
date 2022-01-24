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

import org.partiql.pig.domain.model.TypeDomain
import org.partiql.pig.generator.createDefaultFreeMarkerConfiguration
import org.partiql.pig.generator.setClassLoaderForTemplates
import java.io.PrintWriter
import java.time.OffsetDateTime

fun applyRustTemplate(
    domains: List<TypeDomain>,
    output: PrintWriter
) {
    val renderModel = createRustFreeMarkerGlobals(domains)

    val cfg = createDefaultFreeMarkerConfiguration()
    cfg.setClassLoaderForTemplates()
    val template = cfg.getTemplate("rust.ftl")!!

    template.process(renderModel, output)
}

internal fun createRustFreeMarkerGlobals(domains: List<TypeDomain>): RustFreeMarkerGlobals =
    RustFreeMarkerGlobals(
        domains = domains.map { it.toRustTypeDomain() },
        generatedDate = OffsetDateTime.now())
