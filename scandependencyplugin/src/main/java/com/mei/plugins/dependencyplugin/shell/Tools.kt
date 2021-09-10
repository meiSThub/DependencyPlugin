package com.mei.plugins.dependencyplugin.shell

import java.io.File
import java.util.*

/**
 * @date 2021/8/24
 * @author mxb
 * @desc
 * @desired
 */
class Tools {
    companion object {
        private const val TAG = "Tools"

        /**
         * 是否是Linux 系统
         *
         * @return
         */
        fun isLinux(): Boolean {
            val system = System.getProperty("os.name")
            return system.startsWith("Linux")
        }

        /**
         * mac 系统
         *
         * @return
         */
        fun isMac(): Boolean {
            val system = System.getProperty("os.name")
            return system.startsWith("Mac OS")
        }

        /**
         * window 系统
         *
         * @return
         */
        fun isWindows(): Boolean {
            val system = System.getProperty("os.name")
            return system.startsWith("Windows")
        }

        /**
         * 判断是否是打包机
         *
         */
        fun isJenkins(rootDir: File?): Boolean {
            println("$TAG,rootDir=${rootDir?.absolutePath}")
            val jenkins =
                rootDir?.absolutePath?.toLowerCase(Locale.getDefault())?.contains("jenkins")
                    ?: false
            println("$TAG,isJenkins=$jenkins")
            return jenkins
        }
    }
}