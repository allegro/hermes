package pl.allegro.tech.hermes

import org.gradle.api.Project

fun Project.findBooleanProperty(property: String, defaultValue: Boolean): Boolean =
    (findProperty(property) as? String)?.toBooleanStrictOrNull() ?: defaultValue

fun Project.findIntProperty(property: String, defaultValue: Int): Int =
    (findProperty(property) as? String)?.toIntOrNull() ?: defaultValue

fun Project.findListProperty(property: String, defaultValue: List<String>): List<String> =
    (findProperty(property) as? String)?.split(' ') ?: defaultValue

fun Project.findLongProperty(name: String, defaultValue: Long): Long =
    (findProperty(name) as? String)?.toLongOrNull() ?: defaultValue

fun Project.findStringProperty(property: String, defaultValue: String): String =
    (findProperty(property) as? String) ?: defaultValue
