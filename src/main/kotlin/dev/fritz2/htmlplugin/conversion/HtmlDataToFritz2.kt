package dev.fritz2.htmlplugin.conversion

private val reservedKeywords = listOf("object")


fun StringBuilder.indented(indent: Int = 4, build: StringBuilder.() -> Unit) = append(
    StringBuilder()
        .apply(build)
        .toString()
        .prependIndent(" ".repeat(indent))
)!!

fun StringBuilder.indentedCodeBlock(indent: Int = 4, build: StringBuilder.() -> Unit) {
    appendLine("{")
    indented(indent, build)
    append("}")
}


fun HtmlAttribute.isConstructorAttribute(): Boolean = when (attrName) {
    "class" -> true
    "id" -> true
    else -> false
}

fun Collection<HtmlAttribute>.filterConstructorAttributes(): List<HtmlAttribute> =
    filter { it.isConstructorAttribute() }

fun Collection<HtmlAttribute>.filterBodyAttributes(): List<HtmlAttribute> =
    filter { !it.isConstructorAttribute() }


data class Fritz2TagDescription(
    val name: String,
    val leadingConstructorParams: List<String>
)


fun Collection<HtmlElement>.toFritz2(): String =
    joinToString("\n") { it.toFritz2() }

fun HtmlElement.toFritz2(): String = when (this) {
    is HtmlTag -> toFritz2()
    is HtmlText -> toFritz2Text()
    is HtmlComment -> toFritz2Comment()
    else -> error("${this.javaClass.typeName} not supported")
}

fun HtmlTag.toFritz2(): String {
    val constructorAttributes = attributes.filterConstructorAttributes()
    val bodyAttributes = attributes.filterBodyAttributes()

    // TODO: Cleanup call
    val isInline = children.size == 1 && children[0] is HtmlText && bodyAttributes.isEmpty()

    val fritz2Tag = tagName.lowercase().let { lowercaseName ->
        val isCustom = lowercaseName !in fritz2Tags

        Fritz2TagDescription(
            name = when {
                lowercaseName in reservedKeywords -> "`$lowercaseName`"
                isCustom -> "custom"
                else -> lowercaseName
            },
            leadingConstructorParams = if (isCustom) listOf(lowercaseName) else emptyList()
        )
    }

    return buildString {
        indented {
            append(fritz2Tag.name)

            // TODO: Strip away 'baseClass = ' if class is the only param
            val constructorParams = constructorAttributes.map { it.toFritz2Parameter() }
            if (constructorParams.isNotEmpty()) {
                val paramString = (fritz2Tag.leadingConstructorParams + constructorParams).joinToString(separator = ", ")
                append("($paramString)")
            }


            if (isInline) {
                appendLine("{ ${children.first().toFritz2()} }")
            } else {
                indentedCodeBlock {
                    bodyAttributes.forEach { attribute ->
                        appendLine(attribute.toFritz2Attribute(this@toFritz2))
                    }
                    children.forEach { child ->
                        appendLine(child.toFritz2())
                    }
                }
            }
        }
    }
}

fun HtmlText.toFritz2Text(): String = "+\"\"\"$text\"\"\""

fun HtmlComment.toFritz2Comment(): String = "/* $text */"

fun HtmlAttribute.toFritz2Attribute(owner: HtmlTag? = null): String {
    // remap for fritz2
    val htmlAttrName = attrName

    val attrName = fritz2Attributes.find { it.equals(htmlAttrName, ignoreCase = true) }
    val attrValue = if (attributesWithoutQuotes.contains(attrName)) """$value""" else """"$value""""

    return if (attrName != null) {
        val fritz2AttrName = when (attrName) {
            "for" -> "`for`"
            "class" -> "className"
            else -> attrName
        }

        return when {
            value != null -> """$fritz2AttrName($attrValue)""".trim()
            else -> """$fritz2AttrName("true")""".trim()
        }
    } else {
        toFritz2DataAttribute(htmlAttrName, attrValue)
    }

}

fun HtmlAttribute.toFritz2Parameter(owner: HtmlTag? = null): String {
    // remap for fritz2
    val attrNameLowerCase = attrName.toLowerCase()
    val attrValue = "$value"
    val attrName = when (attrNameLowerCase) {
        "class" -> "baseClass"
        else -> attrNameLowerCase
    }

    return when {
        value != null -> """$attrName = "$attrValue" """.trim()
        else -> """$attrName = true""".trim()
    }

}

fun HtmlAttribute.toFritz2DataAttribute(attrName: String, attrValue: String): String =
    when {
        !value.isNullOrEmpty() -> """attr("$attrName", "$value")""".trim()
        else -> """attr("$attrName", true, trueValue = "")""".trim()
    }
