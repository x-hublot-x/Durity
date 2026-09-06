package com.example.project1.ui.components

import android.graphics.Color as AndroidColor
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.project1.data.model.TaskSegment
import com.example.project1.util.*
import io.noties.markwon.Markwon
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin

/**
 * Нативный Compose-рендерер математических выражений.
 * Поддерживает \frac{}{}, \pi, \sqrt, тригонометрические функции и другие.
 * Без WebView, без KaTeX — только нативный Compose.
 */
@Composable
fun MathView(
    latex: String,
    modifier: Modifier = Modifier,
    textSizeSp: Int = 22,
    textColor: Color = Color.White,
    height: Dp = 56.dp,
    displayMode: Boolean = false
) {
    val trimmed = latex.trim()
    val parsed = parseFraction(trimmed)

    Box(
        modifier = modifier.height(height),
        contentAlignment = Alignment.Center
    ) {
        if (parsed != null) {
            FractionView(
                numerator = parsed.first,
                denominator = parsed.second,
                color = textColor,
                fontSizeSp = textSizeSp
            )
        } else {
            // Любое выражение — конвертируем в текст
            Text(
                text = latexToUnicode(trimmed),
                color = textColor,
                fontSize = textSizeSp.sp,
                fontWeight = if (displayMode) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center,
                lineHeight = (textSizeSp * 1.3f).sp
            )
        }
    }
}

private fun parseFraction(latex: String): Pair<String, String>? {
    if (!latex.startsWith("\\frac")) return null
    val firstOpen = latex.indexOf('{')
    if (firstOpen == -1) return null
    val firstClose = findMatchingBrace(latex, firstOpen)
    if (firstClose == -1) return null

    val secondOpen = latex.indexOf('{', firstClose + 1)
    if (secondOpen == -1) return null
    val secondClose = findMatchingBrace(latex, secondOpen)
    if (secondClose == -1) return null

    val num = latex.substring(firstOpen + 1, firstClose).trim()
    val den = latex.substring(secondOpen + 1, secondClose).trim()
    return Pair(latexToUnicode(num), latexToUnicode(den))
}

private fun findMatchingBrace(s: String, openIdx: Int): Int {
    var depth = 0
    for (i in openIdx until s.length) {
        when (s[i]) {
            '{' -> depth++
            '}' -> {
                depth--
                if (depth == 0) return i
            }
        }
    }
    return -1
}

@Composable
private fun FractionView(
    numerator: String,
    denominator: String,
    color: Color,
    fontSizeSp: Int
) {
    val numSize = fontSizeSp.sp

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(IntrinsicSize.Max)
    ) {
        Text(
            text = numerator,
            color = color,
            fontSize = numSize,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.5.dp)
                .padding(horizontal = 1.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawRect(color = color)
            }
        }

        Text(
            text = denominator,
            color = color,
            fontSize = numSize,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun KatexView(
    latex: String,
    modifier: Modifier = Modifier,
    textColor: Color = Color.White,
    textSizeSp: Int = 18,
    isBlock: Boolean = true
) {
    val colorHex = String.format("#%06X", 0xFFFFFF and textColor.toArgb())

    val sanitizedLatex = latex
        // Восстанавливаем \to, если из-за JSON-парсинга \t превратилась в табуляцию
        .replace("\to", "\\to ")
        .replace("\\", "\\\\")
        .replace("'", "\\'")
        .replace("\r", "")
        .replace("\n", " ")

    val displayMode = isBlock.toString()
    val mathMargin = if (isBlock) "0 auto" else "0"

    val htmlContent = """<!DOCTYPE html>
<html>
<head>
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
<style>
* { margin:0; padding:0; box-sizing:border-box; }
html, body {
  background: transparent;
  color: $colorHex;
  font-size: ${textSizeSp}px;
  margin: 0;
  padding: 0;
  overflow-x: auto;
  overflow-y: hidden;
  white-space: nowrap;
  -webkit-overflow-scrolling: touch;
  width: 100%;
  min-width: 100%;
}
#math-wrapper {
  display: inline-flex;
  min-width: 100%;
  padding: 4px 6px;
  box-sizing: border-box;
}
#math {
  margin: $mathMargin;
  display: inline-block;
}
.katex-display {
  margin: 0 !important;
}
.katex { font-size: 1em; }
</style>
</head>
<body>
<div id="math-wrapper">
  <div id="math"></div>
</div>
<script>
(function() {
  var latex = '$sanitizedLatex';
  var displayMode = $displayMode;

  function render() {
    var el = document.getElementById('math');
    try {
      katex.render(latex, el, { throwOnError: false, displayMode: displayMode });
    } catch(e) {
      el.innerText = latex;
    }
  }

  var link = document.createElement('link');
  link.rel = 'stylesheet';
  link.href = 'file:///android_asset/katex/katex.min.css';
  document.head.appendChild(link);

  var script = document.createElement('script');
  script.src = 'file:///android_asset/katex/katex.min.js';
  script.onload = render;
  script.onerror = function() {
    document.getElementById('math').innerText = latex;
  };
  document.head.appendChild(script);
})();
</script>
</body>
</html>"""

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 36.dp),
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setBackgroundColor(0x00000000)
                settings.javaScriptEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.allowFileAccess = true
                isHorizontalScrollBarEnabled = true
                isVerticalScrollBarEnabled = false
                scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY

                val touchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
                var startX = 0f
                var startY = 0f
                var isHorizontalDrag = false
                setOnTouchListener { v, event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            startX = event.x
                            startY = event.y
                            isHorizontalDrag = false
                            v.parent?.requestDisallowInterceptTouchEvent(false)
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val dx = kotlin.math.abs(event.x - startX)
                            val dy = kotlin.math.abs(event.y - startY)
                            if (!isHorizontalDrag) {
                                val canScrollH = (event.x < startX && v.canScrollHorizontally(1)) ||
                                                 (event.x > startX && v.canScrollHorizontally(-1))
                                if (dx > dy && dx > touchSlop && canScrollH) {
                                    isHorizontalDrag = true
                                    v.parent?.requestDisallowInterceptTouchEvent(true)
                                } else if (dy > dx && dy > touchSlop) {
                                    v.parent?.requestDisallowInterceptTouchEvent(false)
                                }
                            } else {
                                v.parent?.requestDisallowInterceptTouchEvent(true)
                            }
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            v.parent?.requestDisallowInterceptTouchEvent(false)
                            isHorizontalDrag = false
                        }
                    }
                    false
                }

                webViewClient = WebViewClient()
            }
        },
        update = { webView ->
            if (webView.tag != htmlContent) {
                webView.tag = htmlContent
                webView.loadDataWithBaseURL(
                    "file:///android_asset/katex/",
                    htmlContent,
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        }
    )
}

/**
 * Версия KatexView для справочника:
 * - выровнена по левому краю (flex-start)
 * - горизонтальный скролл встроен прямо в WebView (overflow-x: auto)
 * - не обрезает контент
 */
@Composable
fun KatexViewLeft(
    latex: String,
    modifier: Modifier = Modifier,
    textColor: Color = Color.White,
    textSizeSp: Int = 15,
    isBlock: Boolean = false
) {
    val colorHex = String.format("#%06X", 0xFFFFFF and textColor.toArgb())

    val sanitizedLatex = latex
        // Восстанавливаем \to, если из-за JSON-парсинга \t превратилась в табуляцию
        .replace("\to", "\\to ")
        .replace("\\", "\\\\")
        .replace("'", "\\'")
        .replace("\r", "")
        .replace("\n", " ")

    val displayMode = isBlock.toString()

    val htmlContent = """<!DOCTYPE html>
<html>
<head>
<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
<style>
* { margin:0; padding:0; box-sizing:border-box; }
html, body {
  background: transparent;
  color: $colorHex;
  font-size: ${textSizeSp}px;
  margin: 0;
  padding: 0;
  overflow-x: auto;
  overflow-y: hidden;
  white-space: nowrap;
  -webkit-overflow-scrolling: touch;
  width: 100%;
  min-width: 100%;
}
#math-wrapper {
  display: inline-flex;
  min-width: 100%;
  box-sizing: border-box;
}
#math {
  display: inline-block;
  padding: 4px 2px;
  text-align: left;
}
.katex-display {
  margin: 0 !important;
  text-align: left !important;
}
.katex { font-size: 1em; }
</style>
</head>
<body>
<div id="math-wrapper">
  <div id="math"></div>
</div>
<script>
(function() {
  var latex = '$sanitizedLatex';
  var displayMode = $displayMode;

  function render() {
    var el = document.getElementById('math');
    try {
      katex.render(latex, el, { throwOnError: false, displayMode: displayMode });
    } catch(e) {
      el.innerText = latex;
    }
  }

  var link = document.createElement('link');
  link.rel = 'stylesheet';
  link.href = 'file:///android_asset/katex/katex.min.css';
  document.head.appendChild(link);

  var script = document.createElement('script');
  script.src = 'file:///android_asset/katex/katex.min.js';
  script.onload = render;
  script.onerror = function() { document.getElementById('math').innerText = latex; };
  document.head.appendChild(script);
})();
</script>
</body>
</html>"""

    AndroidView(
        modifier = modifier.fillMaxWidth().heightIn(min = 36.dp),
        factory = { context ->
            WebView(context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setBackgroundColor(0x00000000)
                settings.javaScriptEnabled = true
                settings.loadWithOverviewMode = true
                settings.useWideViewPort = true
                settings.allowFileAccess = true
                isHorizontalScrollBarEnabled = true
                isVerticalScrollBarEnabled = false
                scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY

                val touchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
                var startX = 0f
                var startY = 0f
                var isHorizontalDrag = false
                setOnTouchListener { v, event ->
                    when (event.actionMasked) {
                        MotionEvent.ACTION_DOWN -> {
                            startX = event.x
                            startY = event.y
                            isHorizontalDrag = false
                            v.parent?.requestDisallowInterceptTouchEvent(false)
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val dx = kotlin.math.abs(event.x - startX)
                            val dy = kotlin.math.abs(event.y - startY)
                            if (!isHorizontalDrag) {
                                val canScrollH = (event.x < startX && v.canScrollHorizontally(1)) ||
                                                 (event.x > startX && v.canScrollHorizontally(-1))
                                if (dx > dy && dx > touchSlop && canScrollH) {
                                    isHorizontalDrag = true
                                    v.parent?.requestDisallowInterceptTouchEvent(true)
                                } else if (dy > dx && dy > touchSlop) {
                                    v.parent?.requestDisallowInterceptTouchEvent(false)
                                }
                            } else {
                                v.parent?.requestDisallowInterceptTouchEvent(true)
                            }
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            v.parent?.requestDisallowInterceptTouchEvent(false)
                            isHorizontalDrag = false
                        }
                    }
                    false
                }

                webViewClient = WebViewClient()
            }
        },
        update = { webView ->
            if (webView.tag != htmlContent) {
                webView.tag = htmlContent
                webView.loadDataWithBaseURL(
                    "file:///android_asset/katex/",
                    htmlContent,
                    "text/html",
                    "UTF-8",
                    null
                )
            }
        }
    )
}

@Composable
fun MarkdownText(
    markdown: String,
    textColor: Color = Color.White,
    textSizeSp: Float = 14f,
    modifier: Modifier = Modifier
) {
    val colorArgb = textColor.toArgb()
    val cleaned = cleanLatexInText(markdown)

    AndroidView(
        factory = { context ->
            val markwon = Markwon.builder(context)
                .usePlugin(StrikethroughPlugin.create())
                .usePlugin(TablePlugin.create(context))
                .build()
            TextView(context).apply {
                setTextColor(colorArgb)
                textSize = textSizeSp
                setLineSpacing(0f, 1.4f)
                setBackgroundColor(AndroidColor.TRANSPARENT)
                markwon.setMarkdown(this, cleaned)
            }
        },
        update = { tv ->
            val markwon = Markwon.builder(tv.context)
                .usePlugin(StrikethroughPlugin.create())
                .usePlugin(TablePlugin.create(tv.context))
                .build()
            tv.setTextColor(colorArgb)
            tv.textSize = textSizeSp
            markwon.setMarkdown(tv, cleaned)
        },
        modifier = modifier
    )
}

@Composable
fun MixedMathText(
    text: String,
    textColor: Color = Color.White,
    textSizeSp: Int = 15
) {
    val segments = mergeSegments(parseMessageSegments(text))

    Column(modifier = Modifier.fillMaxWidth()) {
        segments.forEach { seg ->
            when (seg) {
                is MessageSegment.BlockMath -> {
                    Spacer(Modifier.height(4.dp))
                    KatexViewLeft(
                        latex = seg.latex,
                        textSizeSp = textSizeSp + 2,
                        textColor = textColor,
                        isBlock = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(4.dp))
                }
                is MessageSegment.PlainText -> {
                    if (seg.text.isNotBlank()) {
                        MarkdownText(
                            markdown = seg.text,
                            textColor = textColor,
                            textSizeSp = textSizeSp.toFloat(),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
                is MessageSegment.InlineMath -> Unit
            }
        }
    }
}

private fun mergeSegments(segments: List<MessageSegment>): List<MessageSegment> {
    val result = mutableListOf<MessageSegment>()
    val buf = StringBuilder()

    fun flushBuf() {
        if (buf.isNotEmpty()) {
            result += MessageSegment.PlainText(buf.toString())
            buf.clear()
        }
    }

    for (seg in segments) {
        when (seg) {
            is MessageSegment.PlainText  -> buf.append(seg.text)
            is MessageSegment.InlineMath -> buf.append(latexToUnicode(seg.latex))
            is MessageSegment.BlockMath  -> { flushBuf(); result += seg }
        }
    }
    flushBuf()
    return result
}

@Composable
fun TaskLatexView(latex: String) {
    val segments = parseTaskLatex(latex)
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        segments.forEach { seg ->
            when (seg) {
                is TaskSegment.BlockMath -> {
                    Spacer(Modifier.height(4.dp))
                    TaskMathBlockView(latex = seg.latex, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(4.dp))
                }
                is TaskSegment.InlineMath -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        contentAlignment = Alignment.Center
                    ) {
                        TaskMathInlineView(latex = seg.latex)
                    }
                }
                is TaskSegment.PlainText -> {
                    if (seg.text.isNotBlank()) {
                        Text(
                            text = seg.text,
                            color = Color.White,
                            fontSize = 16.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TaskMathBlockView(latex: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF141420))
            .padding(vertical = 12.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        KatexView(latex = latex, textSizeSp = 24, isBlock = true)
    }
}

@Composable
fun TaskMathInlineView(latex: String) {
    val rendered = renderIntegralLatex(latex)
    Text(
        text = rendered,
        color = Color.White,
        fontSize = 18.sp,
        fontWeight = FontWeight.Medium
    )
}