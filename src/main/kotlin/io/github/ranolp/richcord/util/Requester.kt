package io.github.ranolp.richcord.util

import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Method
import com.github.kittinunf.fuel.core.Request
import com.github.salomonbrys.kotson.*
import com.google.gson.JsonElement
import java.io.InputStream
import java.nio.file.Path

object Requester {
    lateinit var token: String

    class Application(jsonElement: JsonElement) {
        val id: Long
            get() = _id!!
        val name: String
            get() = _name!!
        val richPresence: RichPresence?
            get() = _richPresence
        private var _id: Long? = null
        private var _name: String? = null
        private var _flags: Long? = null
        private var _richPresence: RichPresence? = null

        object Flags {
            val RICH_PRESENCE = 0b100000000L
        }

        init {
            update(jsonElement)
        }

        private fun update() {
            request("applications/$id", Method.GET, success = {
                update(it.parseJson())
            }, failure = {})
        }

        private fun update(jsonElement: JsonElement) {
            // logger().debug(jsonElement.prettyPrint())
            _id = jsonElement["id"].long
            _name = jsonElement["name"].string
            _flags = jsonElement["flags"].long
            _richPresence = if (hasRichPresence()) _richPresence ?: RichPresence(this) else null
        }

        fun setName(name: String): Boolean =
            request("applications/$id", Method.PUT, request = {
                it.body(
                    """
                        {
                            "name": "$name"
                        }
                    """.trimIndent()
                )
                it.headers["Content-Type"] = "application/json"
            }, success = {
                update()
                true
            }, failure = {
                false
            })

        fun hasRichPresence(): Boolean {
            return _flags!! and Flags.RICH_PRESENCE == Flags.RICH_PRESENCE
        }

        fun enableRichPresence(): RichPresence {
            request("applications/$id/assets/enable", Method.POST)
            update()
            return _richPresence!!
        }

        fun getOrEnableRichPresence(): RichPresence =
            if (!hasRichPresence()) {
                enableRichPresence()
            } else {
                _richPresence!!
            }
    }

    class RichPresence(val application: Application) {
        class Resource(jsonElement: JsonElement) {
            enum class Type(val id: Int) {
                SMALL(1), BIG(2), UNKNOWN(-1)
            }

            val type: Type
                get() = when (_type) {
                    1 -> Type.SMALL
                    2 -> Type.BIG
                    else -> Type.UNKNOWN
                }
            val id: Long
                get() = _id!!
            val name: String
                get() = _name!!

            private var _type: Int? = null
            private var _id: Long? = null
            private var _name: String? = null

            init {
                update(jsonElement)
            }

            fun update(jsonElement: JsonElement) {
                _type = jsonElement["type"].int
                _id = jsonElement["id"].long
                _name = jsonElement["name"].string
            }

            override fun toString(): String {
                return "Resource(type=$type,id=$id,name=$name)"
            }
        }

        fun createResource(name: String, path: Path, type: Resource.Type): Boolean =
            request("applications/${application.id}/assets", Method.POST, request = {
                it.body(
                    """
            {
                "name": "$name",
                "type": "${type.id}",
                "image": "data:image/${path.extension};base64,${path.readAllBytes().encodeBase64()}"
            }""".trimIndent()
                )
                it.headers["Content-Type"] = "application/json"
            }, success = {
                // logger().info(it)
                true
            }, failure = {
                false
            })

        fun createResource(name: String, extension: String, inputStream: InputStream, type: Resource.Type): Boolean =
            request("applications/${application.id}/assets", Method.POST, request = {
                it.body(
                    """
            {
                "name": "$name",
                "type": "${type.id}",
                "image": "data:image/$extension;base64,${inputStream.readBytes().encodeBase64()}"
            }""".trimIndent()
                )
                it.headers["Content-Type"] = "application/json"
            }, success = {
                // logger().info(it)
                true
            }, failure = {
                false
            })

        fun getResources(): List<Resource> =
            request("applications/${application.id}/assets", Method.GET,
                success = {
                    it.parseJson().array.map(::Resource)
                }, failure = {
                    emptyList()
                })

        fun deleteResource(resource: Resource): Boolean = deleteResource(resource.id)

        fun deleteResource(id: Long): Boolean =
            request("applications/${application.id}/assets/$id", Method.DELETE,
                success = {
                    // logger().info(it)
                    true
                }, failure = {
                    false
                })
    }

    fun <T> request(
        url: String,
        method: Method,
        parameters: List<Pair<String, Any?>>? = null,
        request: (Request) -> Unit = {},
        success: (String) -> T,
        failure: (Throwable) -> T, logError: Boolean = true
    ): T {
        val result = FuelManager.instance.request(method, "https://discordapp.com/api/oauth2/$url", parameters).also {
            request(it)

            it.headers["Accept"] = "*/*"
            it.headers["authorization"] = token
            it.headers["User-Agent"] = "curl/7.47.0"
        }.responseString().also {
            // logger().debug(it.first.toString())
            // logger().debug(it.second.toString())
        }.third

        return result.fold({
            success(it)
        }, {
            if (logError) {
                logger().error("An error encountered", it)
            }
            failure(it)
        })
    }

    fun request(
        url: String,
        method: Method,
        parameters: List<Pair<String, Any?>>? = null,
        request: (Request) -> Unit = {}
    ) {
        request(url, method, parameters, request, { null }, { null })
    }

    fun getApplications(): List<Application> =
        request("applications", Method.GET, success = {
            it.parseJson().array.map(::Application)
        }, failure = {
            emptyList()
        })

    fun isValid(): Boolean =
        ::token.isInitialized && request("applications", Method.GET, success = {
            true
        }, failure = {
            false
        }, logError = false)


    fun getApplication(id: Long): Application? =
        request("applications/$id", Method.GET, success = {
            Application(it.parseJson())
        }, failure = {
            null
        })

    fun createApplication(name: String, description: String = ""): Application =
        request("applications", Method.POST, request = {
            it.body(
                """
            {
                "name": "$name",
                "description": "$description",
                "icon": ""
            }""".trimIndent()
            )
            it.headers["Content-Type"] = "application/json"
        }, success = {
            Application(it.parseJson())
        }, failure = {
            // todo: replace
            throw Error()
        })
}
