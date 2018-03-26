package io.github.ranolp.richcord.ui

import com.github.salomonbrys.kotson.*
import com.jagrosh.discordipc.entities.RichPresence
import com.jfoenix.controls.*
import io.github.ranolp.richcord.util.*
import javafx.animation.KeyFrame
import javafx.animation.KeyValue
import javafx.animation.Timeline
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.scene.control.ProgressIndicator
import javafx.scene.control.TextFormatter
import javafx.scene.control.TextInputDialog
import javafx.scene.control.Tooltip
import javafx.scene.image.Image
import javafx.scene.image.ImageView
import javafx.scene.layout.AnchorPane
import javafx.stage.FileChooser
import javafx.util.Duration
import java.io.File
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import kotlin.system.exitProcess

class FormController {
    object Icons {
        val DONE = Image("assets/ic_done_black_64dp.png".toPath().newInputStream())
        val ERROR = Image("assets/ic_error_outline_red_48pt_3x.png".toPath().newInputStream())
    }

    @FXML
    private lateinit var pane: AnchorPane

    @FXML
    private lateinit var title: JFXTextField
    @FXML
    private lateinit var details: JFXTextField
    @FXML
    private lateinit var states: JFXTextField

    @FXML
    private lateinit var currentImage: ImageView
    @FXML
    private lateinit var imageSelector: JFXButton

    @FXML
    private lateinit var elapsedMode: JFXRadioButton
    @FXML
    private lateinit var startTime: JFXTextField
    @FXML
    private lateinit var syncTime: JFXButton

    @FXML
    private lateinit var leftMode: JFXRadioButton
    @FXML
    private lateinit var endDatePicker: JFXDatePicker
    @FXML
    private lateinit var endTimePicker: JFXTimePicker

    @FXML
    private lateinit var updateRichPresence: JFXButton

    @FXML
    private lateinit var noCheckMode: JFXRadioButton

    @FXML
    private lateinit var spinner: ProgressIndicator
    @FXML
    private lateinit var icon: ImageView

    private val application: Requester.Application

    private var fileDirectory: String = ""

    private val dateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME

    private val configPath = "config.json".toPath()
    private val config = {
        if (!configPath.exists()) {
            configPath.createFile()
            configPath.write("{}")
        }
        configPath.newBufferedReader().parseJson().obj
    }()

    private var start: OffsetDateTime? = null
    private var end: OffsetDateTime? = null

    init {
        var token = config["token"].nullString
        while (token == null) {
            token = TextInputDialog().also {
                it.title = "Insert token"
                it.headerText =
                        """We use discord token which is required when creating and updating application.
                           Enable developer mode and press `Ctrl + Shift + I`.
                           Open the application tab, You can find token in local storage.
                           Copy and paste it to this dialog."""
                it.contentText = "Discord token required: "
            }.also { dialog ->
                dialog.setResultConverter {
                    if (it.buttonData.isCancelButton) {
                        logger().info("Cancel requested")
                        exitProcess(0)
                    } else {
                        dialog.editor.text.let {
                            if (it[0] == '"' && it[it.length - 1] == '"') {
                                it.substring(1, it.length - 1)
                            } else {
                                it
                            }
                        }
                    }
                }
            }.showAndWait().map {
                logger().info("Try validating token...")
                Requester.token = it
                if (Requester.isValid()) {
                    logger().info("Valid token!")
                    it
                } else {
                    logger().info("Invalid token!")
                    null
                }
            }.orElse(null)
        }
        config["token"] = token
        Requester.token = token
        config.write(configPath)

        application = Requester.getApplications().firstOrNull { it.id == config["id"].nullLong }
                ?: Requester.createApplication(
            "Richcord",
            "Auto-generated application by Richcord. Created at ${OffsetDateTime.now().format(dateTimeFormatter)}"
        )
        applicationId = application.id
    }

    fun initialize() {
        title.text = config["name"].nullString ?: "Richcord"
        details.text = config["details"].nullString ?: "No Details"
        states.text = config["states"].nullString ?: "No States"
        fileDirectory = config["image-directory"].nullString ?: ""
        if (fileDirectory.isNotEmpty()) {
            currentImage.image = Image(fileDirectory.toPath().newInputStream())
        }
        config["id"] = application.id

        config.write(configPath)

        val formatter = {
            TextFormatter<TextFormatter.Change> {
                if (it.isContentChange) {
                    val newLength = it.controlNewText.length
                    if (newLength > 23) {
                        val tail = it.controlNewText.substring(newLength - 23, newLength)
                        it.text = tail
                        it.setRange(0, it.controlText.length)
                    }
                }
                it
            }
        }

        states.textFormatter = formatter()
        details.textFormatter = formatter()

        fun update(success: () -> Unit = {}, failure: (String) -> Unit = {}) {
            logger().info("Try update rich presence...")
            val richPresence = application.getOrEnableRichPresence()

            logger().info("Delete all resources.")
            richPresence.getResources().forEach {
                richPresence.deleteResource(it)
            }

            val large = "large-" + System.currentTimeMillis()

            if (fileDirectory.isNotEmpty()) {
                richPresence.createResource(
                    large,
                    fileDirectory.toPath(),
                    Requester.RichPresence.Resource.Type.BIG
                )
            }
            logger().info("Resource uploaded, wait a moment.")

            Thread.sleep(1500)

            application.setName(title.text)
            config["name"] = title.text
            config["details"] = details.text
            config["states"] = states.text
            config["image-directory"] = fileDirectory
            config.write(configPath)
            logger().info("Config saved.")

            Tooltip.install(currentImage, Tooltip(fileDirectory.toPath().name))
            logger().info("Tooltip installed.")

            sendRichPresence(
                RichPresence.Builder().setDetails(details.text).setState(states.text).setLargeImage(large).also {
                    if (elapsedMode.isSelected) {
                        it.setStartTimestamp(start ?: OffsetDateTime.now())
                    }
                    if (leftMode.isSelected) {
                        end?.let(it::setEndTimestamp)
                    }
                }.build()
                , {
                    logger().info("Rich presence sent, done!")
                    success()
                }, {
                    logger().info("Rich presence not sent: $it")
                    failure(it)
                }
            )
        }

        updateRichPresence.setOnAction {
            updateRichPresence.isDisable = true
            val timeline = if (icon.opacity > 0) {
                Timeline(
                    KeyFrame(
                        Duration.ZERO,
                        KeyValue(icon.opacityProperty(), 1)
                    ),
                    KeyFrame(
                        Duration.seconds(0.4),
                        KeyValue(icon.opacityProperty(), 0),
                        KeyValue(spinner.opacityProperty(), 0)
                    ),
                    KeyFrame(
                        Duration.seconds(0.8),
                        KeyValue(spinner.opacityProperty(), 1)
                    )
                )
            } else {
                Timeline()
            }
            timeline.setOnFinished {
                val iconTimeline = Timeline(
                    KeyFrame(
                        Duration.ZERO,
                        KeyValue(spinner.opacityProperty(), 1)
                    ),
                    KeyFrame(
                        Duration.seconds(0.4),
                        KeyValue(spinner.opacityProperty(), 0),
                        KeyValue(icon.opacityProperty(), 0),
                        KeyValue(updateRichPresence.disableProperty(), false)
                    ),
                    KeyFrame(
                        Duration.seconds(0.8),
                        KeyValue(icon.opacityProperty(), 1)
                    )
                )
                async {
                    update({
                        icon.image = Icons.DONE
                        iconTimeline.play()
                    }, {
                        icon.image = Icons.ERROR
                        iconTimeline.play()
                    })
                }
            }
            timeline.play()
        }

        imageSelector.setOnAction {
            fileDirectory = FileChooser().also {
                it.title = "Choose image file"
                it.initialDirectory = File(System.getProperty("user.dir"))
                it.extensionFilters += FileChooser.ExtensionFilter("Image file", "*.png", "*.jpg", "*.jpeg")
            }.showOpenDialog(pane.scene.window)?.also {
                currentImage.image = Image(it.inputStream())
            }?.absolutePath ?: fileDirectory
        }

        val updateCheckbox = { _: ActionEvent? ->
            syncTime.isDisable = !elapsedMode.isSelected
            if (elapsedMode.isSelected) {
                syncTime.fire()
            } else {
                startTime.text = ""
            }
            endDatePicker.isDisable = !leftMode.isSelected
            endTimePicker.isDisable = !leftMode.isSelected

            if (leftMode.isSelected) {
                endDatePicker.value = LocalDate.now()
                endTimePicker.value = LocalTime.now()
            } else {
                endDatePicker.value = null
                endTimePicker.value = null
            }
        }

        elapsedMode.setOnAction(updateCheckbox)
        leftMode.setOnAction(updateCheckbox)
        noCheckMode.setOnAction(updateCheckbox)

        val updateTime = { _: ActionEvent? ->
            end = OffsetDateTime.of(
                endDatePicker.value ?: LocalDate.now(),
                endTimePicker.value ?: LocalTime.now(),
                Clock.systemDefaultZone().let {
                    it.zone.rules.getOffset(it.instant())
                })
        }

        endDatePicker.setOnAction(updateTime)
        endTimePicker.setOnAction(updateTime)

        syncTime.setOnAction {
            start = OffsetDateTime.now().also {
                startTime.text = it.format(dateTimeFormatter)
            }
        }

        updateCheckbox(null)
        updateRichPresence.fire()
    }
}
