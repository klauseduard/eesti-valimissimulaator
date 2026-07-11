import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.chart.BarChart
import javafx.scene.chart.CategoryAxis
import javafx.scene.chart.NumberAxis
import javafx.scene.chart.XYChart
import javafx.stage.Stage
import javafx.scene.layout.VBox
import javafx.scene.control.Label
import javafx.geometry.Pos
import javafx.scene.text.Font
import javafx.scene.text.FontWeight

class MandateVisualization : Application() {
    companion object {
        private var mandateData: Map<String, Int> = emptyMap()
        
        fun showResults(data: Map<String, Int>) {
            mandateData = data
            // launch tuleb välja kutsuda väljaspool JavaFX rakenduslõime;
            // kutse blokeerub, kuni kasutaja akna sulgeb
            launch(MandateVisualization::class.java)
        }
    }

    override fun start(stage: Stage) {
        val xAxis = CategoryAxis()
        val yAxis = NumberAxis()
        
        val chart = BarChart(xAxis, yAxis)
        chart.title = "Riigikogu Mandaatide Jaotus"
        
        val series = XYChart.Series<String, Number>()
        series.name = "Mandaadid"
        
        mandateData.forEach { (party, seats) ->
            series.data.add(XYChart.Data(party, seats))
        }
        
        chart.data.add(series)
        chart.animated = true
        
        val title = Label("2023 Riigikogu Valimised")
        title.font = Font.font("Arial", FontWeight.BOLD, 20.0)
        
        val vbox = VBox(10.0)
        vbox.alignment = Pos.CENTER
        vbox.children.addAll(title, chart)
        
        val scene = Scene(vbox, 800.0, 600.0)
        stage.title = "Valimistulemused"
        stage.scene = scene
        stage.show()
    }
} 