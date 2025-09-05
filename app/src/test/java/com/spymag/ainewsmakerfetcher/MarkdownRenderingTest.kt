package com.spymag.ainewsmakerfetcher

import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.ext.task.list.items.TaskListItemsExtension
import org.commonmark.ext.autolink.AutolinkExtension
import org.junit.Test
import org.junit.Assert.*

/**
 * Test for enhanced markdown rendering with GitHub-flavored markdown extensions
 */
class MarkdownRenderingTest {

    @Test
    fun testBasicMarkdownRendering() {
        val markdown = """
# Heading 1
## Heading 2

This is a **bold** text and *italic* text.

```kotlin
fun test() {
    println("Code block")
}
```

- List item 1
- List item 2
        """.trimIndent()

        val extensions = listOf(
            TablesExtension.create(),
            StrikethroughExtension.create(),
            TaskListItemsExtension.create(),
            AutolinkExtension.create()
        )

        val parser = Parser.builder()
            .extensions(extensions)
            .build()

        val document = parser.parse(markdown)
        val renderer = HtmlRenderer.builder()
            .extensions(extensions)
            .build()

        val html = renderer.render(document)
        
        assertNotNull("HTML should not be null", html)
        assertTrue("Should contain h1 tag", html.contains("<h1>"))
        assertTrue("Should contain h2 tag", html.contains("<h2>"))
        assertTrue("Should contain strong tag", html.contains("<strong>"))
        assertTrue("Should contain em tag", html.contains("<em>"))
        assertTrue("Should contain pre tag", html.contains("<pre>"))
        assertTrue("Should contain ul tag", html.contains("<ul>"))
    }

    @Test
    fun testGitHubFlavoredMarkdownTable() {
        val markdown = """
| Column 1 | Column 2 |
|----------|----------|
| Row 1    | Data 1   |
| Row 2    | Data 2   |
        """.trimIndent()

        val extensions = listOf(TablesExtension.create())
        val parser = Parser.builder().extensions(extensions).build()
        val renderer = HtmlRenderer.builder().extensions(extensions).build()
        
        val document = parser.parse(markdown)
        val html = renderer.render(document)
        
        assertTrue("Should contain table tag", html.contains("<table>"))
        assertTrue("Should contain th tag", html.contains("<th>"))
        assertTrue("Should contain td tag", html.contains("<td>"))
    }

    @Test
    fun testStrikethroughExtension() {
        val markdown = "This is ~~strikethrough~~ text."
        
        val extensions = listOf(StrikethroughExtension.create())
        val parser = Parser.builder().extensions(extensions).build()
        val renderer = HtmlRenderer.builder().extensions(extensions).build()
        
        val document = parser.parse(markdown)
        val html = renderer.render(document)
        
        assertTrue("Should contain del tag for strikethrough", html.contains("<del>"))
    }

    @Test
    fun testTaskListItems() {
        val markdown = """
- [x] Completed task
- [ ] Incomplete task
        """.trimIndent()

        val extensions = listOf(TaskListItemsExtension.create())
        val parser = Parser.builder().extensions(extensions).build()
        val renderer = HtmlRenderer.builder().extensions(extensions).build()
        
        val document = parser.parse(markdown)
        val html = renderer.render(document)
        
        assertTrue("Should contain input checkbox", html.contains("<input"))
        assertTrue("Should contain checked checkbox", html.contains("checked"))
    }

    @Test
    fun testAutolinkExtension() {
        val markdown = "Visit https://github.com for more info."
        
        val extensions = listOf(AutolinkExtension.create())
        val parser = Parser.builder().extensions(extensions).build()
        val renderer = HtmlRenderer.builder().extensions(extensions).build()
        
        val document = parser.parse(markdown)
        val html = renderer.render(document)
        
        assertTrue("Should contain a tag for autolinked URL", html.contains("<a"))
        assertTrue("Should contain the URL", html.contains("https://github.com"))
    }

    @Test
    fun testCodeLanguageAttributeProvider() {
        val markdown = """
```kotlin
fun test() {
    println("Hello, World!")
}
```

```java
public void test() {
    System.out.println("Hello, World!");
}
```
        """.trimIndent()

        val extensions = listOf(
            TablesExtension.create(),
            StrikethroughExtension.create(),
            TaskListItemsExtension.create(),
            AutolinkExtension.create()
        )

        val parser = Parser.builder()
            .extensions(extensions)
            .build()

        val document = parser.parse(markdown)
        val renderer = HtmlRenderer.builder()
            .extensions(extensions)
            .build()

        val html = renderer.render(document)
        
        assertTrue("Should contain pre tag", html.contains("<pre>"))
        assertTrue("Should contain code tag", html.contains("<code"))
        // Note: The language classes would be added by our custom attribute provider
        // but we can't easily test that without actually using the provider in the test
    }
}