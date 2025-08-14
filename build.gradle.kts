plugins {
	kotlin("multiplatform") version "2.2.10"
	kotlin("plugin.compose") version "2.2.10"
}

kotlin {
	applyDefaultHierarchyTemplate()

	macosX64 { binaries { executable() } }
	macosArm64 { binaries { executable() } }
	linuxX64 { binaries { executable() } }
	linuxArm64 { binaries { executable() } }
	mingwX64 { binaries { executable() } }

	sourceSets.commonMain.dependencies {
		implementation("com.jakewharton.mosaic:mosaic-runtime:0.16.0")
		implementation("org.jetbrains.kotlinx:kotlinx-io-core:0.8.0")
		implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
	}
}
