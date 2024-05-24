
import jssc.SerialPort
import jssc.SerialPortEvent
import jssc.SerialPortList
import java.awt.BorderLayout
import java.awt.Color
import java.awt.event.ActionEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.*
import java.util.*
//import java.util.prefs.Preferences
import javax.swing.*

class SettingsWork: JFrame() {
//    private lateinit var prefs: Preferences
    private val curPath = System.getProperty("user.dir")
//    val rootPath = curPath
    private val appConfigPath = "$curPath/app.properties"
    private val folderNamePath = "$curPath/folderName.properties"
    private var itemsFolderName: String
    private var itemsDir: String
    private var dirsList:  Set<String>
    private var serialPort: SerialPort? = null
    private var radioBtnsArray = arrayOf<JRadioButton>()
    private var itemsNames: MutableSet<String>
    private var centerPane = JPanel()
    var checkedCount = 0
    val itemsGroup = ButtonGroup()
    val arduinoBtnsGroup = ButtonGroup()
    var itemIsChecked = false
    var arduinoBtnIsChecked = false
    var itemsBtnsMap = HashMap<String, String>() //мап для хранения связи item - Arduino button

    init {
        val appProps = Properties()
        appProps.load(FileInputStream(folderNamePath))
        println("props = ${appProps.entries}")
        itemsFolderName = appProps.getProperty("itemsFolder")
        itemsDir = "$curPath/items/$itemsFolderName"
        dirsList = listDirsUsingDirectoryStream(itemsDir)
        println("dirsList = $dirsList")
        val itemsMap = getItemsMap(folderNamePath)
        itemsNames = itemsMap.keys
        println("keys = $itemsNames")
        createUI("Настройки")
//        configureArduinoConnect(choosenPort)
////        sendToArduino("-")
//        sendToArduino(choosenPort, "1;\n")
//        println("filesSet = $filesSet")
    }

    private fun createUI(title: String) {
        setTitle(title)
        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent) {
                serialPort?.closePort()
            }
        })
//        defaultCloseOperation = EXIT_ON_CLOSE
//        setUpperBar()
//        setCentralPart()
        setSize(800, 600)
        setLocationRelativeTo(null)
//        setProperties()
        readProperties()
        val savePropsButton = JButton("Сохранить")
        savePropsButton.addActionListener {
            saveProperties()
        }

        val setItemsDirectory = JButton("Папка с файлами...")
        setItemsDirectory.addActionListener {
            val label = JLabel("")
            val fileChooser = JFileChooser()
            fileChooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
//            println("curPath = $curPath")
            fileChooser.currentDirectory = File(curPath)
            val option = fileChooser.showOpenDialog(this)
            if (option == JFileChooser.APPROVE_OPTION) {
                val file = fileChooser.selectedFile
                label.text = "Folder Selected: ${file.name}"
                println("Folder Selected: ${file.name}")
                val FOLDER_NAME = file.name
                val props = Properties()
                props.setProperty("itemsFolder", FOLDER_NAME)
                val f = File(folderNamePath)
                val out: OutputStream = FileOutputStream(f)
                props.store(out, "folder properties")
                val itemsMap = getItemsMap(folderNamePath)
                itemsNames = itemsMap.keys
                setCenterPane()
            } else {
                label.text = "Open command canceled"
                println("Open command canceled")
            }
        }
        var totalStr = ""
        val portNames = SerialPortList.getPortNames() // получаем список портов
        val comPorts = JComboBox(portNames) //создаем комбобокс с этим
        //списком
        val northLowerBox = Box(BoxLayout.X_AXIS) // или JPanel с FlowLayout сделать для номеров кнопок
        val getPortsNumBtn = JButton("Получить кол-во кнопок")
        getPortsNumBtn.isEnabled = false
        comPorts.selectedIndex = -1 //чтоб не было выбрано ничего в комбобоксе
        comPorts.addActionListener { arg: ActionEvent? ->  //слушатель выбора порта в комбобоксе
// получаем название выбранного порта
            val choosenPort = comPorts.getItemAt(comPorts.selectedIndex)
            //если serialPort еще не связана с портом или текущий порт не равен выбранному в комбо-боксе
            if (serialPort == null || !serialPort!!.portName.contains(choosenPort)) {
                serialPort = SerialPort(choosenPort) //задаем выбранный порт
//                val ITEM_NAME = "Some name"
            val PORT_NAME = choosenPort
            //create a properties file
            val props = Properties()
//            props.setProperty("Item name", ITEM_NAME)
            props.setProperty("Port name", PORT_NAME)
            val f = File(appConfigPath)
            val out: OutputStream = FileOutputStream(f)
            //If you wish to make some comments
            props.store(out, "port name")
//                val myFile = File("port.dat")
//                val f = FileOutputStream(myFile)
//                val o = ObjectOutputStream(f)
//                o.writeObject(serialPort)
//                o.close()
//                f.close()
                serialPort!!.openPort() //открываем порт
                //задаем параметры порта, 9600 - скорость, такую же нужно задать для Serial.begin в Arduino
                serialPort!!.setParams(9600, 8, 1, 0) //остальные параметры стандартные
                serialPort!!.addEventListener { event: SerialPortEvent ->
                    if (event.isRXCHAR) { // если есть данные для приема
                        var str = serialPort!!.readString()
                        str = str.trim()
                        if (!str.contains("\n ;") && str!="" && str!=";") {
//                    println("received $str") //выводим принятую строку
                            totalStr+=str
                            println("!!!totalStr = $totalStr")
                        }
                        if (str.contains("\n") || str.contains(";")) {
                            println("totalStr = $totalStr")
                            if (totalStr.contains("n=")) { //если получили кол-во кнопок
                                val lastEq = totalStr.lastIndexOf('=')
                                val n: Int = totalStr.subSequence(lastEq+1, if (totalStr.contains(";")) totalStr.length-1 else totalStr.length).toString().toInt()
                                println("n is $n")
                                if (n>0) { //создаем радиокнопки
                                    for(i in 0..<n){
                                        val radioBtn = JRadioButton(i.toString())
                                        radioBtn.addActionListener {
                                            serialPort!!.writeString("$i;")
                                        }
                                        arduinoBtnsGroup.add(radioBtn)
                                        radioBtnsArray = radioBtnsArray.plus(radioBtn)
                                        radioBtn.addActionListener {
                                            val curBtn = it.source as JRadioButton
                                            checkRadioBtn(curBtn)
                                        }
                                        northLowerBox.add(radioBtn)
                                        northLowerBox.add(Box.createHorizontalStrut(10))
                                    }
                                    this.validate()
                                }
                            }
                            if (isNumeric(totalStr)) {
                                println("$totalStr is numeric")
                                radioBtnsArray[totalStr.toInt()].isSelected = true
                                val curBtn = radioBtnsArray[totalStr.toInt()]
                                checkRadioBtn(curBtn)
                            }
                            totalStr = ""
                        }
                    }
                }
//                serialPort!!.writeString("+")
//                println("sent +")
                Thread.sleep(2000) //чтобы точно успеть получить кол-во кнопок
                getPortsNumBtn.isEnabled = true
            }
        }
        getPortsNumBtn.addActionListener {
            serialPort!!.writeString("+")
            println("sent +")
            getPortsNumBtn.isEnabled = false
        }
        val boxWithComboAndBtn = Box(BoxLayout.X_AXIS)
        boxWithComboAndBtn.add(comPorts)
        boxWithComboAndBtn.add(getPortsNumBtn)
        boxWithComboAndBtn.add(Box.createHorizontalGlue())
        val northUpperBox = Box(BoxLayout.X_AXIS)
        northUpperBox.add(setItemsDirectory)
        northUpperBox.add(Box.createHorizontalGlue())
        northUpperBox.add(savePropsButton)
        val northBox = Box(BoxLayout.Y_AXIS)
        northBox.add(boxWithComboAndBtn)
        northBox.add(northUpperBox)
        northBox.add(northLowerBox)

        add(northBox, BorderLayout.NORTH)
        add(centerPane, BorderLayout.CENTER)
        setCenterPane()
        val southBox = Box(BoxLayout.X_AXIS)
        val cancelBtn = JButton("Сбросить")
        cancelBtn.addActionListener {
            for (item in arduinoBtnsGroup.elements){
                item.isEnabled = true
                item.isSelected = false
            }
            arduinoBtnsGroup.clearSelection()
            setCenterPane()
            checkedCount = 0
            arduinoBtnIsChecked = false
            itemIsChecked = false
            itemsBtnsMap.clear()
        }
        southBox.add(Box.createHorizontalGlue())
        southBox.add(cancelBtn)
        add(southBox, BorderLayout.SOUTH)
    }

    private fun checkRadioBtn(curBtn: JRadioButton) {
        if (!arduinoBtnIsChecked) {
            checkedCount++
            arduinoBtnIsChecked = true
            itemIsChecked = false
        }
        if (checkedCount == 2) {
            curBtn.isEnabled = false
            for (item in itemsGroup.elements) {
                if (item.isSelected) {
                    item.isEnabled = false
                    itemsBtnsMap[item.text] = curBtn.actionCommand
                    item.text += "(" + curBtn.actionCommand + ")"
                }
            }
            itemIsChecked = false
            arduinoBtnIsChecked = false
            checkedCount = 0
        }
    }

    private fun setCenterPane(): JPanel {
        remove(centerPane)
        centerPane = JPanel()
        centerPane.border = BorderFactory.createLineBorder(Color.BLUE, 2)
        for (itemName in itemsNames){
            val itemRBtn = JRadioButton(itemName)
            itemRBtn.addActionListener {
                if (!itemIsChecked) {
                    checkedCount++
                    itemIsChecked = true
                    arduinoBtnIsChecked = false
                }
                if (checkedCount==2) {
                    (it.source as JRadioButton).isEnabled = false
                    for (item in arduinoBtnsGroup.elements){
                        if (item.isSelected) {
                            item.isEnabled = false
                            itemsBtnsMap[itemName] = item.text
                            (it.source as JRadioButton).text +="(" +item.text+")"
                        }
                    }
                    itemIsChecked = false
                    arduinoBtnIsChecked = false
                    checkedCount = 0
                }
            }
            itemsGroup.add(itemRBtn)
            centerPane.add(itemRBtn)
            centerPane.add(Box.createHorizontalStrut(20))
        }
        add(centerPane, BorderLayout.CENTER)
        validate()
        return centerPane
    }

//    fun isNumeric(toCheck: String): Boolean {
//        return toCheck.all { char -> char.isDigit() }
//    }

    fun readProperties(){
//        val rootPath = Thread.currentThread().contextClassLoader.getResource("").path

        val myFile = File("$curPath/itemsBtns.dat")
        val fin = FileInputStream(myFile)
        val oin = ObjectInputStream(fin)
//        var myHash2 = HashMap<String, String>()
        val myHash2 = oin.readObject() as HashMap<String, String>
        println("hash from file = $myHash2")
        oin.close()
        fin.close()


//        val appProps = Properties()
//        appProps.load(FileInputStream(appConfigPath))
//        println("props = ${appProps.entries}")
//        val appVersion = appProps.getProperty("version")
//        println("version = $appVersion")
    }

//    fun setProperties() {
//        // This will define a node in which the preferences can be stored
//        prefs = Preferences.userRoot().node(this.javaClass.name)
//        val ID1 = "Test1"
//        val ID2 = "Test2"
//        val ID3 = "Test3"
//
//        // First we will get the values
//        // Define a boolean value
//        println(prefs.getBoolean(ID1, true))
//        // Define a string with default "Hello World
//        println(prefs.get(ID2, "Hello World"))
//        // Define an integer with default 50
//        println(prefs.getInt(ID3, 50))
//        println("all prefs = $prefs")
        // now set the values
//        prefs.putBoolean(ID1, false)
//        prefs.put(ID2, "Hello Europa")
//        prefs.putInt(ID3, 45)

        // Delete the preference settings for the first value
//        prefs.remove(ID1)
//    }

    private fun saveProperties() {
        println("items-buttons map = $itemsBtnsMap")
        val myFile = File("itemsBtns.dat")
        val f = FileOutputStream(myFile)
        val o = ObjectOutputStream(f)
        o.writeObject(itemsBtnsMap)
        o.close()
        f.close()

//        try {
//            val ITEM_NAME = "Some name"
//            val PORT_NUMBER = "Some url"
//            //create a properties file
//            val props = Properties()
//            props.setProperty("Item name", ITEM_NAME)
//            props.setProperty("Port number", PORT_NUMBER)
//            val f = File(appConfigPath)
//            val out: OutputStream = FileOutputStream(f)
//            //If you wish to make some comments
//            props.store(out, "items with button ports association")
//        } catch (e: Exception) {
//            e.printStackTrace()
//        }
    }
}