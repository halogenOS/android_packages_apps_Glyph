/*
 * SPDX-License-Identifier: Apache-2.0
 * SPDX-FileCopyrightText: 2026 The halogenOS Project
 */
package com.nothing.thirdparty;

/*
 * Binder contract of the Glyph Developer Kit. Transaction codes follow
 * declaration order and must not change: 1-5 drive the Glyph LEDs,
 * 6-10 belong to the Glyph Matrix.
 */
interface IGlyphService {
    void setFrameColors(in int[] colors);
    void openSession();
    void closeSession();
    boolean register(String model);
    boolean registerSDK(String apiKey, String model);

    boolean registerMatrixSDK(String apiKey);
    void setMatrixColors(in int[] colors);
    void setGlyphMatrixTimeout(boolean enabled);
    void setAppMatrixColors(in int[] colors);
    void closeAppMatrix();
}
