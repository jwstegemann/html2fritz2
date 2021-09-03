package dev.fritz2.htmlplugin.conversion


const val INDENT = "    " // 4 spaces

fun StringBuilder.addTabIndent(currentIndent: Int) = repeat(currentIndent) { append(INDENT) }

fun HtmlElement.toFritz2(currentIndent: Int = 0): String =
    when (this) {
        is HtmlTag -> toFritz2(currentIndent)
        is HtmlText -> toFritz2Text(currentIndent)
        is HtmlComment -> toFritz2Comment(currentIndent)
        else -> error("${this.javaClass.typeName} not supported")
    }

/**
 * Tags with only one text child is inline
 */
fun HtmlTag.isInline(): Boolean =
    children.size == 1 &&
            children[0] is HtmlText &&
            attributes.filterBodyAttributes().isEmpty()


fun Collection<HtmlAttribute>.filterConstructorAttributes(): List<HtmlAttribute> =
    filter { it.isConstructorAttribute() }

fun Collection<HtmlAttribute>.filterBodyAttributes(): List<HtmlAttribute> =
    filter { !it.isConstructorAttribute() }


fun HtmlAttribute.isConstructorAttribute(): Boolean = when (attrName) {
    "class" -> true
    "id" -> true
    else -> false
}

const val customTagName = "custom"

fun HtmlTag.toFritz2(currentIndent: Int = 0): String {
    val inline = isInline()
    val sb = StringBuilder()

    sb.addTabIndent(currentIndent)
    val tagNameLowerCase = tagName.toLowerCase()

    val fritz2TagName = if (!fritz2Tags.contains(tagNameLowerCase)) customTagName
    else when (tagNameLowerCase) {
        "object" -> "`object`"
        else -> tagNameLowerCase
    }

    sb.append(fritz2TagName)

    val constructorAttributes = attributes.filterConstructorAttributes()

    if (constructorAttributes.isNotEmpty() || fritz2TagName == customTagName) {
        sb.append("(")

        //handle tags not implemented in fritz2
        if (fritz2TagName == customTagName) sb.append(""""$tagName",""")

        if (constructorAttributes.size == 1 && constructorAttributes[0].attrName.toLowerCase() == "class"
        ) {
            sb.append(""""${constructorAttributes[0].value}"""")
        } else {
            sb.append(constructorAttributes.joinToString(", ") { attribute ->
                attribute.toFritz2Parameter(this)
            })
        }


        sb.append(")")
    }


    sb.append(" {")
    if (!inline) {
        sb.append("\n")
    }

    attributes.filterBodyAttributes().forEach { attribute ->
        sb.addTabIndent(currentIndent + 1)
        sb.append(attribute.toFritz2Attribute(this))
        sb.append("\n")
    }



    for (child in children) {
        if (!inline) {
            sb.appendln(child.toFritz2(currentIndent + 1))
        } else {

            // add space before + inline
            sb.append(" ")
            sb.append(child.toFritz2(0))
        }
    }

    if (!inline) {
        sb.addTabIndent(currentIndent)
    }
    sb.append("}")
    return sb.toString()
}


fun HtmlText.toFritz2Text(currentIndent: Int = 0): String =
    StringBuilder().apply {
        addTabIndent(currentIndent)
        append("+ \"\"\"$text\"\"\"")
    }.toString()

fun HtmlComment.toFritz2Comment(currentIndent: Int = 0): String =
    StringBuilder().apply {
        addTabIndent(currentIndent)
        append("/* $text */")
    }.toString()

fun Collection<HtmlElement>.toFritz2(currentIndent: Int = 0): String =
    joinToString("\n") { it.toFritz2(currentIndent) }

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


val fritz2Attributes = listOf(
    "fill",
    "xmlns",
    "d",
    "for",
    "abbr",
    "accept",
    "acceptCharset",
    "action",
    "align",
    "allowFullscreen",
    "allowUserMedia",
    "alt",
    "async",
    "autocomplete",
    "autofocus",
    "autoplay",
    "charset",
    "checked",
    "className",
    "cite",
    "colSpan",
    "cols",
    "controls",
    "coords",
    "crossOrigin",
    "currentTime",
    "dateTime",
    "defaultChecked",
    "defaultMuted",
    "defaultPlaybackRate",
    "defaultSelected",
    "defaultValue",
    "dirName",
    "formAction",
    "formEnctype",
    "formMethod",
    "formNoValidate",
    "formTarget",
    "maxLength",
    "minLength",
    "noValidate",
    "playbackRate",
    "playsInline",
    "referrerPolicy",
    "returnValue",
    "rowSpan",
    "selectedIndex",
    "typeMustMatch",
    "useMap",
    "viewBox",
    "data",
    "default",
    "defer",
    "disabled",
    "download",
    "encoding",
    "enctype",
    "event",
    "hash",
    "headers",
    "height",
    "high",
    "host",
    "hostname",
    "href",
    "hreflang",
    "indeterminate",
    "inputMode",
    "isMap",
    "kind",
    "label",
    "length",
    "loop",
    "low",
    "max",
    "method",
    "min",
    "multiple",
    "muted",
    "name",
    "nonce",
    "open",
    "optimum",
    "password",
    "pathname",
    "pattern",
    "ping",
    "placeholder",
    "port",
    "poster",
    "preload",
    "protocol",
    "rel",
    "required",
    "reversed",
    "rows",
    "scope",
    "search",
    "selected",
    "readOnly",
    "shape",
    "size",
    "sizes",
    "span",
    "src",
    "srcdoc",
    "srclang",
    "srcset",
    "start",
    "step",
    "target",
    "type",
    "username",
    "value",
    "volume",
    "width",
    "wrap"
)

val attributesWithoutQuotes = listOf(
    "colSpan",
    "cols",
    "height",
    "length",
    "maxLength",
    "minLength",
    "rowSpan",
    "rows",
    "selectedIndex",
    "size",
    "span",
    "start",
    "value",
    "width"
)

val fritz2Tags = listOf(
    "a",
    "abbr",
    "address",
    "area",
    "article",
    "aside",
    "audio",
    "b",
    "bdi",
    "bdo",
    "blockquote",
    "br",
    "button",
    "canvas",
    "caption",
    "cite",
    "code",
    "col",
    "colgroup",
    "command",
    "custom",
    "data",
    "datalist",
    "dd",
    "del",
    "details",
    "dfn",
    "dialog",
    "div",
    "dl",
    "dt",
    "em",
    "embed",
    "fieldset",
    "figcaption",
    "figure",
    "footer",
    "form",
    "h1",
    "h2",
    "h3",
    "h4",
    "h5",
    "h6",
    "header",
    "hr",
    "i",
    "iframe",
    "img",
    "input",
    "ins",
    "kbd",
    "label",
    "legend",
    "li",
    "main",
    "map",
    "mark",
    "meter",
    "nav",
    "noscript",
    "ol",
    "optgroup",
    "option",
    "output",
    "p",
    "param",
    "path",
    "picture",
    "pre",
    "progress",
    "q",
    "quote",
    "rp",
    "rt",
    "ruby",
    "s",
    "samp",
    "script",
    "section",
    "select",
    "small",
    "span",
    "strong",
    "sub",
    "summary",
    "sup",
    "svg",
    "table",
    "tbody",
    "td",
    "textarea",
    "tfoot",
    "th",
    "thead",
    "time",
    "tr",
    "track",
    "u",
    "ul",
    "video",
    "wbr"
)
