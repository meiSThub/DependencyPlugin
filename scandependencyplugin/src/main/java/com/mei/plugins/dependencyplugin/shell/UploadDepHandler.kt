package com.mei.plugins.dependencyplugin.shell

import com.mei.plugins.dependencyplugin.bean.DependencyInfo
import com.mei.plugins.dependencyplugin.bean.ScanDepExtension
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import org.gradle.api.Project
import java.io.File

/**
 * @date 2021/8/23
 * @author mxb
 * @desc 依赖文件上传操作
 * @desired
 */
class UploadDepHandler(
    var project: Project,
    private var scanDepConfig: ScanDepExtension,
    private val fileName: String,
) {

    companion object {
        // 通过 git token 去提交和拉取最新代码
        private const val GITLBA_ACCESS_TOKEN = "fCgsmfsssmdsssgdds"

        // git clone https://oauth2:<access_token>@gitlab.com/.../xxx.git
        // <access_token> token信息
        const val DEFAULT_GIT_URL =
            "https://oauth2:$GITLBA_ACCESS_TOKEN@github.com:meiSThub/version-dependency-check.git"
    }

    /**
     * 存放依赖信息的 git 工程 url
     */
    private val gitUrl by lazy {
        scanDepConfig.gitUrl ?: DEFAULT_GIT_URL
    }

    /**
     * 获取 git 工程的名称
     */
    private val gitProjectName by lazy {
        getVersionProjectName(scanDepConfig.gitUrl ?: DEFAULT_GIT_URL)
    }

    /**
     * 根据git url，获取 git 工程的名称
     *
     * @param gitUrl git url
     * @return
     */
    private fun getVersionProjectName(gitUrl: String): String {
        var projectName = gitUrl.substring(gitUrl.lastIndexOf("/") + 1, gitUrl.length)
        if (projectName.endsWith(".git")) {
            projectName = projectName.substring(0, projectName.lastIndexOf("."))
        }
        println("ScanDependenciesTask,projectName=$projectName")
        return projectName
    }

    /**
     * git 工程本地存放的路径文件夹
     */
    private val gitProjectLocalPath: File by lazy {
        val parentPath = project.rootDir.parentFile
        println("ScanDependenciesTask,parentPath=$parentPath")
        val versionDependencyCheck =
            "$parentPath${File.separator}cache${File.separator}$gitProjectName"
        println("ScanDependenciesTask,依赖工程的路径：$versionDependencyCheck")
        File(versionDependencyCheck)
    }

    /**
     * git 工程是否被 下载
     *
     * @return
     */
    private fun isGitProjectCloned(): Boolean {
        val gitFile = File(gitProjectLocalPath, ".git")
        return gitProjectLocalPath.exists() && gitFile.exists()
    }

    /**
     * 依赖缓存文件
     */
    private val dependencyCacheFile by lazy {
        File("$gitProjectLocalPath${File.separator}$fileName").apply {
            if (!parentFile.exists()) {
                parentFile.mkdirs()
            }
            if (!exists()) {
                createNewFile()
            }
        }
    }

    /**
     * 保存被压缩后的图片路径
     *
     * @param list 依赖集合
     */
    fun saveDependencies(list: List<DependencyInfo>) {
        // 1. 检测git工程是否已经下载，如果没有下载，则先执行clone操作
        cloneDepVersionProject()
        // 2. 执行pull 操作
        updateGitProject()
        // 3. 把文件复制到工程目录下
        saveDependencyToFile(list)
        // 4. 文件修改后，提交修改
        commitAndPush()
    }

    /**
     * 下载git依赖工程
     */
    private fun cloneDepVersionProject() {
        // 如果依赖工程不存在，则下载
        if (!isGitProjectCloned()) {
            // 如果文件路径不存在，则需要下载
            val cmd = "git clone $gitUrl ${gitProjectLocalPath.absolutePath}"
            execCmd(cmd)
        }
    }

    /**
     * 执行git pull 命令，更新git 工程
     */
    private fun updateGitProject() {
        val cmd =
            "git --git-dir=${gitProjectLocalPath.absolutePath}/.git --work-tree=${gitProjectLocalPath.absolutePath} pull"
        // git --git-dir='/xxx/xxx/.git' --work-tree='/xxx/xxx' pull # 注意这两个参数是传给git命令的，要放在'git'的后面
        execCmd(cmd)
    }

    /**
     * 把依赖信息缓存到指定到文件中
     *
     * @param list 依赖列表
     */
    private fun saveDependencyToFile(list: List<DependencyInfo>) {
        val gson: Gson = GsonBuilder().setPrettyPrinting().create()
        dependencyCacheFile.writeText(gson.toJson(list))
    }

    /**
     * 提交更新
     */
    private fun commitAndPush() {
        val gitDir =
            "--git-dir=${gitProjectLocalPath.absolutePath}/.git --work-tree=${gitProjectLocalPath.absolutePath}"
        if (hasUpdate()) {
            println("ScanDependenciesTask,提交更新start...")
            val add = "git $gitDir add ."
            execCmd(add)
            val commit = "git $gitDir commit -m 依赖库有更新"
            execCmd(commit)
            val push = "git $gitDir push origin master --force"
            execCmd(push)
            println("ScanDependenciesTask,提交更新end...")
        } else {
            println("ScanDependenciesTask,没有更新，不需要提交")
        }
    }

    /**
     * 文件是否有更新
     *
     * @return
     */
    private fun hasUpdate(): Boolean {
        val status =
            "git --git-dir=${gitProjectLocalPath.absolutePath}/.git --work-tree=${gitProjectLocalPath.absolutePath} status -s"
        println("ScanDependenciesTask,执行命令->$status")
        return try {
            val process = Runtime.getRuntime().exec(status)
            // 执行 git status -s 命令，如果有返回信息，说明文件有更新
            var text = process.inputStream.bufferedReader().readText()
            val error = process.errorStream.bufferedReader().readText()
            println("error=$error")
            process.waitFor()// 等待命令执行完毕
            text
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }.run {
            println("ScanDependenciesTask,hasUpdate()，git status 返回的信息->$this,是否有更新=${isNotEmpty()}")
            return isNotEmpty()
        }
    }

    /**
     * 执行指定的命令
     *
     * @param cmd
     */
    private fun execCmd(cmd: String) {
        println("ScanDependenciesTask,执行命令->$cmd")
        // 通过project.exec 方法，执行命令，控制台有打印信息
        try {
            val process = Runtime.getRuntime().exec(cmd)
            process.waitFor()// 等待命令执行完毕
            // project.exec {
            //     it.executable = "bash"
            //     it.args = mutableListOf("-c", cmd)
            // }
            val logInfo = process.inputStream.bufferedReader().readText()
            println("logInfo=$logInfo")
            val text = process.errorStream.bufferedReader().readText()
            println("error=$text")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}