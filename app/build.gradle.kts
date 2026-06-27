plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.plugin.compose") version "2.2.10"
  id("com.google.devtools.ksp") version "2.3.5"
  id("io.github.takahirom.roborazzi") version "1.59.0"
  id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin") version "2.0.1"
  id("org.jetbrains.kotlin.plugin.serialization") version "2.2.10"
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  configurations.all {
    resolutionStrategy {
      force("org.jetbrains.kotlin:kotlin-stdlib:2.2.10")
      force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.2.10")
      force("org.jetbrains.kotlin:kotlin-stdlib-jdk7:2.2.10")
      force("org.jetbrains.kotlin:kotlin-reflect:2.2.10")
    }
  }

  defaultConfig {
    applicationId = "com.aistudio.synergyfit.kybshz"
    minSdk = 24
    targetSdk = 36
    versionCode = 17
    versionName = "1.1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    
    val supabaseUrl = System.getenv("SUPABASE_URL") ?: ""
    buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
    
    val supabaseAnonKey = System.getenv("SUPABASE_ANON_KEY") ?: ""
    buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"")
    
    val googleWebClientId = System.getenv("GOOGLE_WEB_CLIENT_ID") ?: ""
    buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"$googleWebClientId\"")
    
    // NVIDIA_API_KEY eliminada del BuildConfig (se usa proxy backend)
    val proxyUrl = System.getenv("PROXY_URL") ?: "http://10.0.2.2:3000"
    buildConfigField("String", "PROXY_URL", "\"$proxyUrl\"")
  }

  signingConfigs {
    create("release") {
      val userHome = System.getProperty("user.home")
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${userHome}/.keystores/synergyfit.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "my-key-alias"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug {
      signingConfig = signingConfigs.getByName("debugConfig")
    }
  }
  compileOptions {
    isCoreLibraryDesugaringEnabled = true
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  packaging {
    jniLibs {
      keepDebugSymbols += "**/libandroidx.graphics.path.so"
    }
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

dependencies {
    // Supabase
    implementation(platform("io.github.jan-tennert.supabase:bom:3.6.0"))
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:auth-kt")
    
    // Ktor Client for Supabase & Serialization
    implementation(platform("io.ktor:ktor-bom:3.4.3"))
    implementation("io.ktor:ktor-client-okhttp")
    implementation("io.ktor:ktor-client-core")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    
    // Credential Manager (Native Google Login)
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // Charts
    implementation("com.patrykandpatrick.vico:compose:1.15.0")
    implementation("com.patrykandpatrick.vico:compose-m3:1.15.0")
    implementation("com.patrykandpatrick.vico:core:1.15.0")

    // Room — Local SQLite Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    add("ksp", libs.androidx.room.compiler)

  coreLibraryDesugaring(libs.desugar.jdk.libs)
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.appcompat)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.converter.moshi)

  // Gemini SDK (without Firebase)

  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  implementation(libs.retrofit)

  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
}

tasks.register<Copy>("renameApk") {
    outputs.upToDateWhen { false }
    from("build/outputs/apk/debug/app-debug.apk")
    into("../releases")
    rename { "SynergyFit-v1.1.0.apk" }
}

tasks.configureEach {
    if (name == "assembleDebug") {
        finalizedBy("renameApk")
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions.freeCompilerArgs.add("-Xskip-metadata-version-check")
    compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
}
