package org.amr.project

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform