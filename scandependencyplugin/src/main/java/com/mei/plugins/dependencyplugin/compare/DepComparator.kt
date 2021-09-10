package com.mei.plugins.dependencyplugin.compare

import com.mei.plugins.dependencyplugin.bean.DependencyInfo
import com.mei.plugins.dependencyplugin.bean.ScanDepExtension
import com.mei.plugins.dependencyplugin.shell.GitUtils
import com.mei.plugins.dependencyplugin.shell.Tools
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import java.io.File
import java.util.*

/**
 * @date 2021/8/24
 * @author mxb
 * @desc 依赖对比
 * @desired
 */
class DepComparator(

    /**
     * 是否是正式环境
     */
    private var isRelease: Boolean = false,
    /**
     * 工程根路径
     */
    private var rootDir: File? = null,
    /**
     * 扩展配置
     */
    private var scanDepConfig: ScanDepExtension,
) {

    /**
     * 缓存旧的依赖库信息
     */
    private var oldDepMap = mutableMapOf<String, DependencyInfo>()

    /**
     * 新的依赖库与旧的依赖对比，有更新的依赖库列表
     */
    private var diffDepList = mutableListOf<DependencyInfo>()

    /**
     * 对比信息，如：androidx.activity:activity:1.0.0--升级为->androidx.activity:activity:1.0.1
     */
    private var diffDepCompareList = mutableListOf<String>()

    // 通知测试
    private val notifyTest by lazy {
        NotifyTest()
    }

    companion object {
        private const val TAG = "DepComparator"
    }


    /**
     * 把之前保存的依赖信息，缓存到map集合中
     *
     * @param dependenciesFile
     */
    fun readOldDep(dependenciesFile: File) {
        // 不允许推送消息，则不读取旧的依赖数据
        if (!checkPostMsgEnabled()) {
            println("$TAG,不允许推送消息，如果希望推送消息，请配置postMessageEnable=true")
            return
        }
        if (dependenciesFile.length() <= 0) {
            return
        }
        println("$TAG,读取缓存文件start...")
        // 把旧的依赖库信息加载到缓存中
        val sb = StringBuilder()
        dependenciesFile.forEachLine {
            sb.append(it)
        }

        // 把从缓存文件中读取的json字符串，转换成对象列表
        Gson().fromJson<MutableList<DependencyInfo>>(JsonParser.parseString(sb.toString()),
            object : TypeToken<MutableList<DependencyInfo>>() {

            }.type).apply {
            // 打印列表
            println("$TAG,readOldDep:$this")
        }.forEach {
            // 以package_name为key，DependencyInfo为value，存入map集合中
            oldDepMap[it.package_name!!] = it
        }
        println("$TAG,读取缓存文件end...")
    }

    /**
     * 比较新老依赖库列表，保存有更新的库信息
     *
     * @param newDepList 新收集到的依赖库列表
     */
    fun compare(newDepList: List<DependencyInfo>) {
        if (!checkPostMsgEnabled()) {
            println("$TAG,不允许推送消息，如果希望推送消息，请配置postMessageEnable=true")
            return
        }
        // 保存更新的依赖库信息，如：androidx.activity:activity:1.0.0--更新为->1.0.1
        val diffDep = StringBuilder()
        val branchName = GitUtils.getGitBranch()
        diffDep.append("打包分支->$branchName").append("\n")
        var hasUpdate = false// 依赖是否有更新
        newDepList.forEachIndexed { _, info ->
            val oldDep = oldDepMap[info.package_name] ?: return@forEachIndexed
            if (oldDep.version != info.version) {
                // diffDepList.add(it)
                // diffDepCompareList.add("${oldDep.package_name}:${oldDep.version}--更新为->${it.version}")
                diffDep.append("${oldDep.package_name}:${oldDep.version}--更新为->${info.version}")
                    .append("\n")
                hasUpdate = true
            }
        }
        if (!hasUpdate) {
            println("$TAG,依赖库没有更新，diffDep=$diffDep")
            return
        }
        // 推送依赖库更新消息
        postMessage(diffDep.toString())
    }

    /**
     * 推送消息
     *
     * @param diffDepInfo 依赖库更新信息
     */
    private fun postMessage(diffDepInfo: String) {
        notifyTest.postMessage(diffDepInfo, scanDepConfig.webhookUrl)
    }

    /**
     * 检查是否允许推送消息
     * @return true:允许推送消息，false：不允许推送消息
     */
    private fun checkPostMsgEnabled(): Boolean {
        return scanDepConfig.postMessageEnable || (isRelease && Tools.isJenkins(rootDir))
    }

}