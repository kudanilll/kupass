import com.android.build.api.dsl.LibraryExtension
import com.nielcode.kupass.buildlogic.configureDetekt
import com.nielcode.kupass.buildlogic.configureKotlinAndroid
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.library")
            extensions.configure<LibraryExtension> { configureKotlinAndroid(this) }
            configureDetekt()
        }
    }
}
