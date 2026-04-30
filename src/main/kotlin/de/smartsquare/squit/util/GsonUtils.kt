package de.smartsquare.squit.util

import com.google.gson.Gson
import com.google.gson.GsonBuilder

/**
 * Shared [Gson] instance for compact serialisation/deserialisation.
 */
val gson = Gson()

/**
 * Shared [Gson] instance with pretty-printing enabled.
 */
val prettyGson: Gson = GsonBuilder().setPrettyPrinting().create()
