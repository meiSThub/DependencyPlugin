package com.mei.plugins.dependencyplugin

import com.mei.plugins.dependencyplugin.bean.ScanDepExtension
import com.mei.plugins.dependencyplugin.task.ScanDependenciesTask
import com.android.build.gradle.AppExtension
import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * @date 2021/8/3
 * @author mxb
 * @desc 扫描依赖
 * @desired
 */
class ScanDependenciesPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        println("ScanDependenciesPlugin,应用插件：ScanDependenciesPlugin")
        // 创建扩展
        project.extensions.create("scanDepConfig", ScanDepExtension::class.java)
        // 获取扩展
        val scanDepConfig = project.property("scanDepConfig") as ScanDepExtension
        project.afterEvaluate {
            // 是否是app工程
            val isApp = project.plugins.hasPlugin("com.android.application")
            // 获取变体
            val variants = if (isApp) {
                (project.property("android") as AppExtension).applicationVariants
            } else {
                (project.property("android") as LibraryExtension).libraryVariants
            }
            variants.forEach {
                val scanDepTask = project.tasks.create("scan${it.name.capitalize()}Dependencies",
                    ScanDependenciesTask::class.java, it, scanDepConfig).apply {
                    group = "dependence"
                }

                project.tasks.findByPath("merge${it.name.capitalize()}Resources")
                    ?.dependsOn(scanDepTask)
            }
        }
    }
}