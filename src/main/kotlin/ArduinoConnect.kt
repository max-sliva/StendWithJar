import jssc.SerialPort
import jssc.SerialPortList
import org.json.JSONArray
import org.json.JSONObject

//fun configureArduinoConnect(engine: WebEngine, pagesNumbersMap: Map<String, String>) {
//fun configureArduinoConnect(engine: WebEngine, htmlFilesJSONArray: JSONArray) {
fun configureArduinoConnect( serialPortFromGUI: SerialPort/*htmlFilesJSONArray: JSONArray*/) {
    var serialPort: SerialPort?
    for (port in SerialPortList.getPortNames()) {
        println(port)
        var totalStr = ""
        serialPort = SerialPort(port)
        if (!serialPort.isOpened) {
            serialPort.openPort()
            serialPort.setParams(9600, 8, 1, 0)
        }
        serialPort.addEventListener{
            if (it.isRXCHAR) {// если есть данные для приема
                var str = serialPort.readString()

//убираем лишние символы (типа пробелов, которые могут быть в принятой строке)
                str = str.trim()
                if (!str.contains("\n ;") && str!="") {
//                    println("received $str") //выводим принятую строку
                    totalStr+=str
                }
                if (str.contains(";")) {
                    println("totalStr = $totalStr")
                    totalStr = ""
                }
//                Platform.runLater{
//                    val url: URL = Main.javaClass.getResource(pagesNumbersMap.get(str))
//                    val htmlFile = htmlFilesJSONArray.first { //todo тут можно раскомментировать потом
//                        (it as JSONObject).get("number")==str
//                    } as JSONObject
//                    val url = Main.javaClass.getResource("HTML/${htmlFile.get("html")}")
                    //todo add
//                    val url= HelloApplication::class.java.getResource("HTML/${htmlFile.get("html")}")
//                    println("htmlFile = $htmlFile")
//                    println("file = $url")
//                    engine.load(url.toString())
                }
            }
        serialPort.closePort()
        }
    }

fun sendToArduino(str: String) {
    //todo сделать отправку запроса в Arduino
    var serialPort: SerialPort?
    for (port in SerialPortList.getPortNames()) {
        serialPort = SerialPort(port)
//        if (!serialPort.isOpened) {
//            serialPort.openPort()
//            serialPort.setParams(9600, 8, 1, 0)
//        }
        serialPort.writeString(str)
        println("sent to Arduino $str")
//        serialPort.closePort()
    }
}


