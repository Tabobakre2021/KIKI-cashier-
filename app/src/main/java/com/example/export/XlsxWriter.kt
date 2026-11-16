package com.example.export

import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class XlsxSheetData(
    val sheetName: String,
    val rows: List<List<String>>
)

object XlsxWriter {

    fun writeWorkbook(outputFile: File, sheets: List<XlsxSheetData>) {
        ZipOutputStream(FileOutputStream(outputFile)).use { zip ->
            writeEntry(zip, "[Content_Types].xml", buildContentTypes(sheets.size))
            writeEntry(zip, "_rels/.rels", ROOT_RELS)
            writeEntry(zip, "xl/workbook.xml", buildWorkbookXml(sheets))
            writeEntry(zip, "xl/_rels/workbook.xml.rels", buildWorkbookRels(sheets.size))
            writeEntry(zip, "xl/styles.xml", STYLES_XML)

            sheets.forEachIndexed { idx, sheet ->
                writeEntry(zip, "xl/worksheets/sheet${idx + 1}.xml", buildWorksheetXml(sheet.rows))
            }
        }
    }

    private fun writeEntry(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun buildContentTypes(sheetCount: Int): String {
        val overrides = (1..sheetCount).joinToString("\n") { idx ->
            """<Override PartName="/xl/worksheets/sheet$idx.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>"""
        }
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
  $overrides
</Types>"""
    }

    private fun buildWorkbookXml(sheets: List<XlsxSheetData>): String {
        val sheetTags = sheets.mapIndexed { idx, s ->
            val safeName = escapeXml(s.sheetName.take(30).replace("/", "-"))
            """<sheet name="$safeName" sheetId="${idx + 1}" r:id="rId${idx + 1}"/>"""
        }.joinToString("\n    ")

        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    $sheetTags
  </sheets>
</workbook>"""
    }

    private fun buildWorkbookRels(sheetCount: Int): String {
        val sheetRels = (1..sheetCount).joinToString("\n  ") { idx ->
            """<Relationship Id="rId$idx" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet$idx.xml"/>"""
        }
        val styleId = sheetCount + 1
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  $sheetRels
  <Relationship Id="rId$styleId" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""
    }

    private fun buildWorksheetXml(rows: List<List<String>>): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
        sb.append("""<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
        sb.append("""<sheetViews><sheetView workbookViewId="0" rightToLeft="1"/></sheetViews>""")
        sb.append("""<sheetData>""")

        rows.forEachIndexed { rowIdx, cols ->
            val rowNum = rowIdx + 1
            val styleIdx = if (rowIdx == 0) 1 else 0
            sb.append("""<row r="$rowNum">""")
            cols.forEachIndexed { colIdx, rawVal ->
                val cellRef = "${colLetters(colIdx)}$rowNum"
                val num = rawVal.toDoubleOrNull()
                if (num != null && rawVal.trim().matches(Regex("^-?\\d+(\\.\\d+)?$"))) {
                    sb.append("""<c r="$cellRef" s="$styleIdx"><v>$rawVal</v></c>""")
                } else {
                    sb.append("""<c r="$cellRef" t="inlineStr" s="$styleIdx"><is><t>${escapeXml(rawVal)}</t></is></c>""")
                }
            }
            sb.append("""</row>""")
        }

        sb.append("""</sheetData></worksheet>""")
        return sb.toString()
    }

    private fun colLetters(index: Int): String {
        var i = index
        val sb = StringBuilder()
        while (i >= 0) {
            sb.insert(0, ('A' + (i % 26)))
            i = (i / 26) - 1
        }
        return sb.toString()
    }

    private fun escapeXml(input: String): String = input
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

    private const val ROOT_RELS = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

    private const val STYLES_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <fonts count="2">
    <font><sz val="11"/><name val="Calibri"/></font>
    <font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Calibri"/></font>
  </fonts>
  <fills count="3">
    <fill><patternFill patternType="none"/></fill>
    <fill><patternFill patternType="gray125"/></fill>
    <fill><patternFill patternType="solid"><fgColor rgb="FF741F3D"/></patternFill></fill>
  </fills>
  <borders count="1">
    <border><left/><right/><top/><bottom/><diagonal/></border>
  </borders>
  <cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
  <cellXfs count="2">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
    <xf numFmtId="0" fontId="1" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1"/>
  </cellXfs>
</styleSheet>"""
}
