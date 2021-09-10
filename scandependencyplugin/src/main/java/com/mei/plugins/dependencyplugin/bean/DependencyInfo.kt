package com.mei.plugins.dependencyplugin.bean

/**
 * @date 2021/8/23
 * @author mxb
 * @desc 依赖的信息
 * @desired
 */
data class DependencyInfo(
    var package_name: String? = null,// 包名
    var version: String? = null,// 版本号
    var hash: String? = null,// 包的hash值,各个端根据实际情况决定是否需要
    var level: Int = 0,// 等级分类,0：核心，1：重要，2：一般
)
