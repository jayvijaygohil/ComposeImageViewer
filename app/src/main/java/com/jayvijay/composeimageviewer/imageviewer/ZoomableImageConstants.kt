package com.jayvijay.composeimageviewer.imageviewer

/**
 * A small tolerance for floating-point comparisons to account for precision errors.
 */
internal const val ZoomScaleTolerance = 0.01f

/**
 * Multiplier to convert a scale factor (e.g., 1.0) into a percentage (e.g., 100).
 */
internal const val ZoomPercentMultiplier = 100

/**
 * Debounce timeout in milliseconds for updating the displayed zoom percentage.
 */
internal const val ZoomPercentDebounceMs = 40L
