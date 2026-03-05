plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
}

fun String.toBuildConfigString(): String =
    "\"${replace("\\", "\\\\").replace("\"", "\\\"")}\""

val supabaseFunctionsBaseUrl =
    (project.findProperty("SUPABASE_FUNCTIONS_BASE_URL") as? String)?.trim().orEmpty()
val supabaseFunctionsAnonKey =
    (project.findProperty("SUPABASE_FUNCTIONS_ANON_KEY") as? String)?.trim().orEmpty()
val supabaseFunctionsPushPath =
    (project.findProperty("SUPABASE_FUNCTIONS_PUSH_PATH") as? String)?.trim().orEmpty()
val supabaseFunctionsCreateUserPath =
    (project.findProperty("SUPABASE_FUNCTIONS_CREATE_USER_PATH") as? String)?.trim().orEmpty()
val supabaseFunctionsPaystackInitPath =
    (project.findProperty("SUPABASE_FUNCTIONS_PAYSTACK_INIT_PATH") as? String)?.trim().orEmpty()
val supabaseFunctionsPaystackVerifyPath =
    (project.findProperty("SUPABASE_FUNCTIONS_PAYSTACK_VERIFY_PATH") as? String)?.trim().orEmpty()
val cloudinaryApiKey =
    (project.findProperty("CLOUDINARY_API_KEY") as? String)?.trim().orEmpty()
val cloudinaryApiSecret =
    (project.findProperty("CLOUDINARY_API_SECRET") as? String)?.trim().orEmpty()

android {
    namespace = "com.example.eventglow"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.example.eventglow"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
        buildConfigField("String", "CLOUDINARY_CLOUD_NAME", "\"djakhwvat\"")
        buildConfigField("String", "CLOUDINARY_UPLOAD_PRESET", "\"eventGlowPreset\"")
        buildConfigField("String", "CLOUDINARY_API_KEY", cloudinaryApiKey.toBuildConfigString())
        buildConfigField("String", "CLOUDINARY_API_SECRET", cloudinaryApiSecret.toBuildConfigString())
        buildConfigField("String", "SUPABASE_FUNCTIONS_BASE_URL", supabaseFunctionsBaseUrl.toBuildConfigString())
        buildConfigField("String", "SUPABASE_FUNCTIONS_ANON_KEY", supabaseFunctionsAnonKey.toBuildConfigString())
        buildConfigField("String", "SUPABASE_FUNCTIONS_PUSH_PATH", supabaseFunctionsPushPath.toBuildConfigString())
        buildConfigField(
            "String",
            "SUPABASE_FUNCTIONS_CREATE_USER_PATH",
            supabaseFunctionsCreateUserPath.toBuildConfigString()
        )
        buildConfigField(
            "String",
            "SUPABASE_FUNCTIONS_PAYSTACK_INIT_PATH",
            supabaseFunctionsPaystackInitPath.toBuildConfigString()
        )
        buildConfigField(
            "String",
            "SUPABASE_FUNCTIONS_PAYSTACK_VERIFY_PATH",
            supabaseFunctionsPaystackVerifyPath.toBuildConfigString()
        )

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

        kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {

    // Accompanist
    implementation(libs.accompanist.pager)
    implementation(libs.accompanist.indicators)
    implementation(libs.accompanist.swiperefresh)

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.gson)

    // Coil
    implementation(libs.coil.compose)

    // Serialization
    implementation(libs.serialization.json)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.database)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.appcheck.playintegrity)
    debugImplementation(libs.firebase.appcheck.debug)

    // Core Android
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime)
    implementation(libs.activity.compose)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.graphics)
    implementation(libs.compose.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons.extended)

    // Navigation
    implementation(libs.navigation.runtime)
    implementation(libs.navigation.compose)

    // Media
    implementation(libs.media3.common)
    implementation(libs.play.services.code.scanner)
    implementation(libs.zxing.core)

    // Tests
    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.espresso)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)

    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)


}
