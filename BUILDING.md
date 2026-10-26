# JARs bauen

Voraussetzungen: JDK 25, Maven auf `PATH`, WATUT, CoroUtil und CoroUtil-Paper als benachbarte Ordner.

Unter Windows `build_all_jars.bat` im WATUT-Ordner doppelt anklicken. Das Skript findet JDK 25, baut zuerst CoroUtil (Fabric, NeoForge und Paper/Maven), danach WATUT für Fabric und NeoForge und anschließend das Paper-Modul. Es hält beim ersten Fehler an und wartet zum Schluss auf eine Taste. Für Terminal/Automatisierung: `build_all_jars.bat --no-pause`.

Alle fertigen JARs liegen anschließend im Ordner `jar/`: die drei CoroUtil-JARs (Fabric, NeoForge, Paper Plugin), die zwei WATUT-Mod-JARs, das gemeinsame Modul und das Paper-Plugin. Sowohl die Client-Mod-JARs (Fabric/NeoForge) als auch das Paper-Server-Plugin enthalten bereits die passende CoroUtil-JAR bzw. die CoroUtil-Klassen als eingebettete Abhängigkeit (shaded). Für einen Server oder ein Spiel genügt deshalb die jeweilige WATUT-JAR; separate CoroUtil-JARs werden weiterhin für andere Mods und eigenständige Installationen erstellt.

Die Gradle-Ausgaben sind nach Loader unter `build/fabric/` und `build/neoforge/` getrennt; veröffentlichbare Gradle-JARs liegen unter `build/libs/`. Die Zielversion ist Minecraft 26.3 mit Java 25.

Einzelbuilds: `gradlew.bat -Ploader=fabric build` bzw. `gradlew.bat -Ploader=neoforge build` mit JDK 25 in `JAVA_HOME`. Vor WATUT muss die passende CoroUtil-JAR gebaut sein.

Der normale Build lädt das alte ModPublisher-Plugin nicht, da dessen ASM-Abhängigkeit Java-25-Klassen nicht unterstützt. Die bisherige Veröffentlichungskonfiguration ist in `gradle/mod-publishing.gradle` aufgehoben; Plattform-Veröffentlichung gehört nicht zum 26.3-Build.
