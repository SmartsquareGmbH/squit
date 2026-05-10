plugins {
    id("base")
    id("de.smartsquare.squit")
}

squit {
    jdbcDrivers.set(listOf("org.h2.Driver"))
}
