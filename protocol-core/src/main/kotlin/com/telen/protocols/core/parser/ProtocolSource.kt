package com.telen.protocols.core.parser

import android.content.Context
import java.io.InputStream

/**
 * Isolates the one real Android touchpoint (reading the protocol JSON) so everything else in this
 * module is plain-JVM testable.
 */
fun interface ProtocolSource {
    fun open(): InputStream
}

class AssetProtocolSource(private val context: Context, private val fileName: String) :
    ProtocolSource {
    override fun open(): InputStream = context.assets.open(fileName)
}
