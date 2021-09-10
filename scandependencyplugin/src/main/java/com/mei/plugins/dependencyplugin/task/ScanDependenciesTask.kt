package com.mei.plugins.dependencyplugin.task

import com.mei.plugins.dependencyplugin.bean.DependencyInfo
import com.mei.plugins.dependencyplugin.bean.ScanDepExtension
import com.mei.plugins.dependencyplugin.compare.DepComparator
import com.mei.plugins.dependencyplugin.shell.UploadDepHandler
import com.android.build.gradle.api.BaseVariant
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

/**
 * @date 2021/8/3
 * @author mxb
 * @desc 扫描依赖
 * @desired
 */
open class ScanDependenciesTask @Inject constructor(
    var variant: BaseVariant,
    /**
     * 扩展配置
     */
    private var scanDepConfig: ScanDepExtension,
) : DefaultTask() {

    companion object {
        private const val TAG = "ScanDependenciesTask"

        // release名称
        private const val RELEASE_NAME = "release"
    }

    private fun isRelease(): Boolean = variant.name == RELEASE_NAME

    /**
     * 依赖搜集后，缓存到的文件名称
     */
    private val dependencyFileName by lazy {
        val name = if (isRelease()) {
            "release"
        } else {
            "test"
        }
        "android_$name.json"
    }

    /**
     * 依赖信息的缓存文件
     */
    private val dependenciesFile: File by lazy {
        File("${project.projectDir}/cache/$dependencyFileName").apply {
            if (!parentFile.exists()) {
                parentFile.mkdirs()
            }
            if (!exists()) {
                createNewFile()
            }
        }
    }

    // 依赖比较服务
    private val depComparator by lazy {
        DepComparator(isRelease(), project.rootDir, scanDepConfig)
    }

    @TaskAction
    fun doAction() {
        try {// 读取缓存的依赖库信息
            depComparator.readOldDep(dependenciesFile)

            val configuration = project.configurations.getByName("${variant.name}CompileClasspath")
            println("ScanDependenciesTask,configuration=$configuration")
            var count = 0 // 用于统计依赖的数量
            // 遍历依赖集合
            configuration.resolvedConfiguration.lenientConfiguration.allModuleDependencies.mapNotNull { dep: ResolvedDependency ->
                val identifier: ModuleVersionIdentifier = dep.module.id
                if (identifier.version == "unspecified") {
                    return@mapNotNull null
                }
                println("$count、依赖组件->${identifier}")
                count++
                DependencyInfo("${identifier.group}:${identifier.name}",
                    identifier.version, level = level(identifier.group))
            }.sortedWith(compareBy({
                // 多条件排序
                it.level // 先按照level进行排序
            }, {
                it.package_name // 再按照packageName进行排序
            })).run {
                println("ScanDependenciesTask,依赖总数：$count")
                // 以json格式，保存依赖到指定文件
                UploadDepHandler(project, scanDepConfig, dependencyFileName).saveDependencies(this)
                saveDependencies(this)
                depComparator.compare(this)
                println("ScanDependenciesTask,ScanDependenciesTask 执行完成。。。")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    /**
     * 保存被压缩后的图片路径
     *
     * @param list 依赖集合
     */
    private fun saveDependencies(list: List<DependencyInfo>) {
        // 把新压缩的图片保存到压缩文件中
        FileOutputStream(dependenciesFile).writer().use { writer ->
            // 格式化json字符串
            val gson: Gson = GsonBuilder().setPrettyPrinting().create()
            writer.write(gson.toJson(list))
        }
    }

    /**
     * 依赖的等级
     *
     * @param groupId
     * @return 等级分类,0：核心，1：重要，2：一般
     */
    private fun level(groupId: String): Int {
        scanDepConfig.levelMap?.forEach { (level, prefixList) ->
            // groupId是否以集合prefixList中的字符串开头
            val hasPrefix = prefixList.find {
                groupId.startsWith(it) || groupId == it
            }
            // 如果能找到,则直接返回level值
            if (hasPrefix != null) {
                return level
            }
        }
        // 默认level值
        return when {
            groupId.startsWith("io.mei") || groupId.startsWith("com.mei") -> 0
            groupId.startsWith("com.google") || groupId.startsWith("androidx") -> 1
            else -> 2
        }
    }
}