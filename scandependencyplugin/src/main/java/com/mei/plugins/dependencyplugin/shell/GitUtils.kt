package com.mei.plugins.dependencyplugin.shell

/**
 * @date 2021/8/24
 * @author mxb
 * @desc
 * @desired
 */
class GitUtils {
    companion object {
        /**
         * 获取Git 分支名
         */
        fun getGitBranch(): String {
            return try {
                val getBranch = "git symbolic-ref --short -q HEAD"
                val process = Runtime.getRuntime().exec(getBranch)
                // 执行 git status -s 命令，如果有返回信息，说明文件有更新
                val text = process.inputStream.bufferedReader().readText()
                println("logInfo=$text")
                val error = process.errorStream.bufferedReader().readText()
                println("error=$error")
                process.waitFor()// 等待命令执行完毕
                text
            } catch (e: Exception) {
                e.printStackTrace()
                ""
            }.apply {
                println("GitUtils,getGitBranch():分支名称=$this")
            }
        }
    }
}