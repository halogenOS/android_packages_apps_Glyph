/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.platform

import java.lang.reflect.Method
import kotlin.reflect.KClass

/**
 * Framework members the SDK leaves out, looked up by name. The platform lets apps signed with
 * its key use them; everything that needs one goes through this package, so the rest of the
 * app builds against the SDK alone.
 */
internal object HiddenMembers {

    /** The value of the static field [name] of [owner]. */
    @Suppress("UNCHECKED_CAST")
    fun <T> constant(owner: KClass<*>, name: String): T = owner.java.getField(name).get(null) as T

    /** The public method [name] of [owner] taking [parameters]. */
    fun method(owner: KClass<*>, name: String, vararg parameters: Class<*>): Method = owner.java.getMethod(name, *parameters)
}
