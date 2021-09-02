package dev.fritz2.htmlplugin.ide

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.html.HtmlFileImpl
import dev.fritz2.htmlplugin.conversion.HtmlPsiToHtmlDataConverter
import dev.fritz2.htmlplugin.conversion.converToHtmlElements
import dev.fritz2.htmlplugin.conversion.toFritz2


class ConvertHTMLToFritz2Action : AnAction("Convert HTML to fritz2") {

    override fun actionPerformed(e: AnActionEvent) {
        logger.debug { "actionPerformed" }

        val editor = e.getRequiredData(CommonDataKeys.EDITOR)
        val project = e.project ?: return

        val document = editor.document
        val selectionModel = editor.selectionModel
        val start = selectionModel.selectionStart
        val end = selectionModel.selectionEnd


        val text = selectionModel.selectedText

        if (text.isNullOrEmpty()) {
            return
        }

        val sourcePsiFileFromText: PsiFile = HtmlPsiToHtmlDataConverter.createHtmlFileFromText(project, text!!)

        if (sourcePsiFileFromText !is HtmlFileImpl) {
            return
        }

        val convertedToKotlinText = sourcePsiFileFromText
                .converToHtmlElements()
                .toFritz2()

        if (convertedToKotlinText.isEmpty()) {
            return
        }

        //Making the replacement
        WriteCommandAction.runWriteCommandAction(project) {
            document.replaceString(start, end, convertedToKotlinText)
        }
        selectionModel.removeSelection()
    }

}
