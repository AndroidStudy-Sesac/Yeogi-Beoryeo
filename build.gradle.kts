import kotlinx.kover.gradle.plugin.dsl.CoverageUnit

val focusedLineCoverageMinimum = providers.gradleProperty("focusedCoverageLineMinimum").get().toInt()
val focusedBranchCoverageMinimum = providers.gradleProperty("focusedCoverageBranchMinimum").get().toInt()

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.jetbrains.kotlin.jvm) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.kover)
}

dependencies {
    kover(project(":app"))
    kover(project(":data"))
    kover(project(":domain"))
    kover(project(":presentation"))
}

kover {
    currentProject {
        createVariant("focused") {}
    }

    reports {
        variant("focused") {
            filters {
                includes {
                    classes(
                        "com.team.yeogibeoryeo.appguide.AppGuideGeometry*",
                        "com.team.yeogibeoryeo.appguide.AppGuideState*",
                        "com.team.yeogibeoryeo.appguide.AppGuideViewModel*",
                        "com.team.yeogibeoryeo.navigation.AppRoute*",
                        "com.team.yeogibeoryeo.navigation.BottomTabNavigationAction",
                        "com.team.yeogibeoryeo.navigation.CtaPreconditionDialog",
                        "com.team.yeogibeoryeo.navigation.CtaPreconditionDialogSpec",
                        "com.team.yeogibeoryeo.navigation.CtaPreconditionEffect*",
                        "com.team.yeogibeoryeo.navigation.CtaPreconditionState",
                        "com.team.yeogibeoryeo.navigation.FavoriteSpotNavigationMapper*",
                        "com.team.yeogibeoryeo.navigation.MapRouteInitialSpotTypeMapper*",
                        "com.team.yeogibeoryeo.data.*Repository*",
                        "com.team.yeogibeoryeo.data.*Mapper*",
                        "com.team.yeogibeoryeo.data.*Parser*",
                        "com.team.yeogibeoryeo.data.*Normalizer*",
                        "com.team.yeogibeoryeo.data.*DataSource*",
                        "com.team.yeogibeoryeo.data.*LocalSource*",
                        "com.team.yeogibeoryeo.data.region.RegionAssetContract*",
                        "com.team.yeogibeoryeo.domain.*.usecase.*",
                        "com.team.yeogibeoryeo.domain.*.model.*Policy*",
                        "com.team.yeogibeoryeo.domain.*Normalizer*",
                        "com.team.yeogibeoryeo.domain.*Comparator*",
                        "com.team.yeogibeoryeo.domain.favorite.model.*Snapshot*",
                        "com.team.yeogibeoryeo.domain.favorite.model.RegionalGuideFavoriteKey*",
                        "com.team.yeogibeoryeo.domain.item.model.DisposalRecyclability*",
                        "com.team.yeogibeoryeo.presentation.*ViewModel*",
                        "com.team.yeogibeoryeo.presentation.*Mapper*",
                        "com.team.yeogibeoryeo.presentation.*Policy*",
                        "com.team.yeogibeoryeo.presentation.*Formatter*",
                        "com.team.yeogibeoryeo.presentation.*Metrics*",
                        "com.team.yeogibeoryeo.presentation.common.text.KoreanText*",
                        "com.team.yeogibeoryeo.presentation.regionalguide.RegionalGuideCandidateMessage*",
                        "com.team.yeogibeoryeo.presentation.regionalguide.RegionalGuideCandidateBackStack*",
                        "com.team.yeogibeoryeo.presentation.search.ItemGuideDetailAction*",
                        "com.team.yeogibeoryeo.presentation.search.components.QuickCategoryGridOrder*",
                        "com.team.yeogibeoryeo.presentation.search.components.QuickCategoryGridLabel*",
                        "com.team.yeogibeoryeo.presentation.search.components.QuickCategoryGridCollapseLayout*",
                    )
                }
                excludes {
                    androidGeneratedClasses()
                    annotatedBy("androidx.compose.runtime.Composable")
                    classes(
                        "*Hilt*",
                        "*Dagger*",
                        "*_*Factory*",
                        "*_*MembersInjector*",
                        "com.team.yeogibeoryeo.presentation.settings.detail.PrivacyPolicyDetail*",
                    )
                }
            }
            html {
                htmlDir = layout.buildDirectory.dir("reports/kover/focused/html")
            }
            xml {
                xmlFile = layout.buildDirectory.file("reports/kover/focused/report.xml")
            }
            verify {
                rule("focused line coverage") {
                    minBound(focusedLineCoverageMinimum)
                }
                rule("focused branch coverage") {
                    minBound(focusedBranchCoverageMinimum, CoverageUnit.BRANCH)
                }
            }
        }
    }
}
