package io.github.ranolp.richcord

import com.jagrosh.discordipc.IPCClient
import io.github.ranolp.richcord.util.client
import io.github.ranolp.richcord.util.isClientInitialized
import io.github.ranolp.richcord.util.logger
import io.github.ranolp.richcord.util.resourceOf
import javafx.application.Platform
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.stage.Stage
import kotlin.system.exitProcess
import javafx.application.Application as JavaFxApplication

fun main(args: Array<String>) {
    JavaFxApplication.launch(Richcord::class.java, *args)
    logger<Richcord>().info("Start the program")

}

class Richcord : JavaFxApplication() {
    override fun start(stage: Stage) {
        val parent = FXMLLoader.load<Parent>(resourceOf("form.fxml"))
        val scene = Scene(parent)

        stage.scene = scene
        stage.title = "Richcord"

        stage.show()

        stage.setOnCloseRequest {
            Platform.exit()
            if (isClientInitialized() && client.status == IPCClient.Status.CONNECTED) {
                client.close()
            }
            logger().info("Program end.")
            exitProcess(0)
        }
    }
}
