package dev.fritz2.htmlplugin.conversion


interface HtmlElement


class HtmlText(val text: String) : HtmlElement
class HtmlComment(val text: String) : HtmlElement


class HtmlTag(val tagName: String) : HtmlElement {

    val attributes: MutableList<HtmlAttribute> = mutableListOf()

    val children: MutableList<HtmlElement> = mutableListOf()
}



class HtmlAttribute(val attrName: String, val value: String? = null)

