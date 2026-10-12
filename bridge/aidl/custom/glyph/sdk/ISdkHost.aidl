/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package custom.glyph.sdk;

/*
 * How the Glyph Developer Kit bridge hands app calls to the Glyph app. Each call names the
 * app it comes from, which the bridge learned from the incoming binder call.
 */
oneway interface ISdkHost {
    void register(int uid, String packageName, String model);
    void open(int uid, String packageName);
    void show(int uid, in int[] levels);
    void close(int uid);
}
