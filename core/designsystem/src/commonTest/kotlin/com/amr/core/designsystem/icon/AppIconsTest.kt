package com.amr.core.designsystem.icon

import androidx.compose.ui.graphics.vector.VectorPath
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppIconsTest {

    @Test
    fun everyIconParsesIntoANonEmptyPath() {
        AppIcons.all.forEach { icon ->
            val path = icon.root.single() as VectorPath
            assertTrue(path.pathData.isNotEmpty(), "${icon.name} has no path nodes")
            assertEquals(24f, icon.viewportWidth)
        }
    }

    @Test
    fun iconNamesAreUnique() {
        val names = AppIcons.all.map { it.name }
        assertEquals(names.size, names.toSet().size)
    }
}
