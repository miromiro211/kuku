import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val localSettings = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
fun quoted(value: String) = "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""


android {
    namespace = "com.gonggangmate.fresh"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.gonggangmate.fresh"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "2.0-kuru"
        buildConfigField("String", "SUPABASE_URL", quoted(localSettings.getProperty("SUPABASE_URL", "https://gbveuxpgwooarmbzlhlz.supabase.co").trim()))
        buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", quoted(localSettings.getProperty("SUPABASE_PUBLISHABLE_KEY", "sb_publishable_FXu6U5qREDu7JVcaTXlMHA_mFpwwCOR").trim()))
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true; buildConfig = true }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.04.01"))
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    testImplementation("junit:junit:4.13.2")
    testImplementation("com.vaadin.external.google:android-json:0.0.20131108.vaadin1")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
