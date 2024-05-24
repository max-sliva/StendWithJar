import com.formdev.flatlaf.FlatLightLaf
import jssc.SerialPort
import jssc.SerialPortEvent
import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.WindowEvent
import java.io.*
import java.util.*
import javax.swing.*


class SimpleEx(title: String) : JFrame() {

    private val curPath = System.getProperty("user.dir")
    private val folderNamePath = "$curPath/folderName.properties"
    private val backgroundImage = "items/background.jpg"
    private var filesSet = listOf("$curPath/welcome.txt", "$curPath/$backgroundImage")
    private val textArea = JTextArea()
    val itemsComboBox = JComboBox<String>()
    //    private val imageHolder = JButton("")
    private val imageHolder = JLabel()
    var itemsBtnsMap = HashMap<String, String>() //мап для хранения связи item - Arduino button
    var btnsItemsMap = HashMap<String, String>() //мап для хранения связи Arduino button - item
    private var serialPort: SerialPort? = null
    private val imageLabel = JLabel()
    var imageWindow = JDialog()
    init {
        val myFile = File("$curPath/itemsBtns.dat")
        val fin = FileInputStream(myFile)
        val oin = ObjectInputStream(fin)
//        var myHash2 = HashMap<String, String>()
        itemsBtnsMap = oin.readObject() as HashMap<String, String>
        println("hash from file = $itemsBtnsMap")
        itemsBtnsMap.forEach { (item, btn) ->
            btnsItemsMap[btn] = item
        }
        println("btnsItemsMap = $btnsItemsMap")

        oin.close()
        fin.close()
        createUI(title)
        println("filesSet = $filesSet")
        val appConfigPath = "$curPath/app.properties"
        val appProps = Properties()
        appProps.load(FileInputStream(appConfigPath))
        println("props = ${appProps.entries}")
        val portName = appProps.getProperty("Port name") //todo проверить, чтобы в линуксе имя с пробелом читалось
        println("portName = $portName")
        serialPort = SerialPort(portName)
        serialPort!!.openPort() //открываем порт
        //задаем параметры порта, 9600 - скорость, такую же нужно задать для Serial.begin в Arduino
        serialPort!!.setParams(9600, 8, 1, 0) //остальные параметры стандартные
        var totalStr = ""
        serialPort!!.addEventListener { event: SerialPortEvent ->
            if (event.isRXCHAR) { // если есть данные для приема
                var str = serialPort!!.readString()
                str = str.trim()
                if (!str.contains("\n ;") && str != "" && str != ";") {
//                    println("received $str") //выводим принятую строку
                    totalStr += str
                    println("!!!totalStr = $totalStr")
                }
                if (str.contains("\n") || str.contains(";")) {
                    if (isNumeric(totalStr)) {
                        val item = btnsItemsMap[totalStr]
                        val itemsMap = getItemsMap(folderNamePath)
                        val tempList = itemsMap[item]?.toList()
                        itemsComboBox.selectedItem = item
                        showItem(tempList)
                    }
                    totalStr = ""
                }
            }
        }
        imageWindow!!.add(imageLabel, BorderLayout.CENTER)
        val graphics = GraphicsEnvironment.getLocalGraphicsEnvironment()
        val device = graphics.defaultScreenDevice
        device.setFullScreenWindow(imageWindow) //for full screen
        imageWindow.isVisible = false
    }

    fun getFilesSet() = filesSet

    fun getImageHolder() = imageHolder

    private fun createUI(title: String) {
        setTitle(title)
        defaultCloseOperation = EXIT_ON_CLOSE
        setUpperBar()
        setCentralPart()
        setSize(400, 300)
        setLocationRelativeTo(null)
    }

    private fun setUpperBar() {
        val upperNorthBox = Box(BoxLayout.X_AXIS) //верхняя панель
        val newColor = Color("FF1E63B2".toLong(16).toInt())
        upperNorthBox.isOpaque = true
        upperNorthBox.background = newColor
        val facultyLogoWhite = "faculty_white.png"
        val nvsuLogoWhite = "NVSU_white.png"
        var nvsuLogo = ImageIcon(nvsuLogoWhite) //лого универа
        var image = nvsuLogo.image // объект для преобразования
        var newimg = image.getScaledInstance(100, 100, Image.SCALE_SMOOTH) // задаем размер
        nvsuLogo = ImageIcon(newimg) //применяем новые параметры

        val nvsuLabel = JLabel(nvsuLogo) //лейбл с лого универа
//    nvsuLabel.background = backColor

        val text = File("captionText.txt").readText()
        val textLabel = JLabel(text)


//		Font font = new Font("Serif", Font.BOLD, 12);
        var font: Font? = null
        try {
            font = Font.createFont(
                Font.TRUETYPE_FONT,
                File(System.getProperty("user.dir") + "/Fonts/RobotoMedium/Roboto-Medium.ttf")
            )
        } catch (e: FontFormatException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        val genv = GraphicsEnvironment.getLocalGraphicsEnvironment() // объект для регистрации шрифта
        genv.registerFont(font) // регистрируем шрифт
        font = font!!.deriveFont(41f) // задаем ему размер
        textLabel.foreground = Color.WHITE
        textLabel.font = font
        //		nvsuLabel.setBorder(BorderFactory.createLineBorder(myColor, 5));
        var fitimLogo = ImageIcon(facultyLogoWhite) //лого факультета
        image = fitimLogo.image //объект для преобразования
        newimg = image.getScaledInstance(100, 100, Image.SCALE_SMOOTH) // задаем размер
        fitimLogo = ImageIcon(newimg) //применяем новые параметры
        val fitimLabel = JLabel(fitimLogo) //лейбл с лого факультета

        upperNorthBox.add(Box.createHorizontalGlue())
        upperNorthBox.add(nvsuLabel)
        upperNorthBox.add(Box.createHorizontalGlue())
        upperNorthBox.add(textLabel)
        upperNorthBox.add(Box.createHorizontalGlue())
        upperNorthBox.add(fitimLabel)
        upperNorthBox.add(Box.createHorizontalGlue())

        val northBoxMain = Box(BoxLayout.Y_AXIS)
        val itemsMap = getItemsMap(folderNamePath)
//        val curPath = System.getProperty("user.dir")
//        val backgroundImage = "items/background.jpg"
//        var filesSet =  listOf("$curPath/welcome.txt", "$curPath/$backgroundImage")

        val itemsList = itemsMap.keys.toList()
        val comboBoxModel = DefaultComboBoxModel<String>()
        itemsComboBox.setRenderer(MyComboBoxRenderer("Выберите экспонат: ▼"));
        comboBoxModel.addAll(itemsList)
        itemsComboBox.addActionListener { e ->
            println("selected = ${e.actionCommand}")
            val curItem = itemsComboBox.selectedItem
            println("selected = $curItem")
            println("number to Arduino = ${itemsBtnsMap[curItem]}")
            serialPort!!.writeString("${itemsBtnsMap[curItem]};")
            val tempList = itemsMap[itemsComboBox.selectedItem]?.toList()
            showItem(tempList)
        }
        itemsComboBox.model = comboBoxModel
        val lowerNorthBox = Box(BoxLayout.X_AXIS)
        lowerNorthBox.add(itemsComboBox)
        lowerNorthBox.add(Box.createHorizontalGlue())
        val fontChangeLabel = JLabel("Шрифт:  ")
        val fontPlus = JButton("+")
        val fontMinus = JButton("–")
        lowerNorthBox.add(fontChangeLabel)
        lowerNorthBox.add(fontMinus)
        lowerNorthBox.add(Box.createHorizontalStrut(20))
        lowerNorthBox.add(fontPlus)
        fontPlus.preferredSize = Dimension(70, fontPlus.minimumSize.height)
        fontMinus.preferredSize = Dimension(70, fontMinus.minimumSize.height)
        fontPlus.addActionListener { e ->
            fontSizeChange(e)
        }
        fontMinus.addActionListener { e ->
            fontSizeChange(e)
        }
        northBoxMain.add(upperNorthBox)
        northBoxMain.add(lowerNorthBox)
        add(northBoxMain, BorderLayout.NORTH)
    }

    private fun showItem(tempList: List<String>?) { //ф-ия для показа экспоната
        filesSet = listOf()
        filesSet = filesSet.plus(tempList!!.first())
        filesSet = filesSet.plus(tempList.last())
        println("filesSet = $filesSet")
        val text = File(filesSet.first()).readText()
        textArea.text = text
        var itemImage = ImageIcon(filesSet.last())
        val newWidth = 500
        val newimg = getScaledImage(itemImage, newWidth, 800)

        imageHolder.icon = ImageIcon(newimg)
        imageHolder.preferredSize = Dimension(newWidth, imageHolder.minimumSize.height)
        imageHolder.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(mouseEvent: MouseEvent?) {
//                if (imageWindow!=null) imageWindow!!.isVisible = false
                makeImageWindow(itemImage)
            }
        })
        //            imageHolder.horizontalAlignment = JLabel.CENTER
        ////        imageHolder.icon = itemImage
        //            imageHolder.preferredSize = Dimension(400, 300)
    }

    private fun makeImageWindow(itemImage: ImageIcon) { //создает JDialog с большой картинкой экспоната
        println("image = $itemImage")
        imageWindow.isVisible = true
        println("imageWindow visible is ${imageWindow.isVisible}")
        var newimg = getScaledImage(itemImage, imageWindow!!.width, imageWindow!!.height)
        imageWindow!!.remove(imageLabel)
        imageLabel.icon = ImageIcon(newimg)
        imageLabel.setHorizontalAlignment(JLabel.CENTER)
        imageLabel.setVerticalAlignment(JLabel.CENTER)
        imageLabel.border = BorderFactory.createLineBorder(Color.BLUE, 2)
        imageWindow!!.add(imageLabel, BorderLayout.CENTER)
        imageLabel.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(mouseEvent: MouseEvent?) {
                    imageWindow!!.isVisible = false
            }
        })
        val sizePlusBtn = JButton("+")
        val sizeMinusBtn = JButton("-")
        sizePlusBtn.addActionListener {
            val width = imageLabel.icon.iconWidth
            newimg = getScaledImage(itemImage, width + 10, -1)
            imageLabel.icon = ImageIcon(newimg)
        }
        sizeMinusBtn.addActionListener {
            val width = imageLabel.icon.iconWidth
            newimg = getScaledImage(itemImage, width - 10, -1)
            imageLabel.icon = ImageIcon(newimg)
        }
//        imageWindow?.add(Box(BoxLayout.X_AXIS).apply { add(sizePlusBtn); add(sizeMinusBtn) }, BorderLayout.NORTH)
    }

    private fun getScaledImage(itemImage: ImageIcon, newWidth: Int, newHeight: Int): Image{
        var image = itemImage.image
        var imageWidth = itemImage.iconWidth
        var imageHeight = itemImage.iconHeight
        if (itemImage.iconWidth > itemImage.iconHeight) {
            val ratio = imageWidth.toFloat() / newWidth
            imageWidth = newWidth
            imageHeight = (itemImage.iconHeight / ratio).toInt()
            if (imageHeight>newHeight) { //это чтобы по ширине был не шире поля вывода картинки (лейбла)
                val ratio = imageHeight.toFloat() / newHeight
                imageHeight = newHeight
                imageWidth = (imageWidth / ratio).toInt()
            }
        } else {
            val ratio = imageHeight.toFloat() / newHeight
            imageHeight = newHeight
            imageWidth = (itemImage.iconWidth / ratio).toInt()
            if (imageWidth>newWidth) {
                val ratio = imageWidth.toFloat() / newWidth
                imageWidth = newWidth
                imageHeight = (imageHeight / ratio).toInt()
            }
        }
        if (newWidth<0 || newHeight <0) image.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH)
        return image.getScaledInstance(imageWidth, imageHeight, Image.SCALE_SMOOTH)
    }

    private fun fontSizeChange(e: ActionEvent) {
        var font = textArea.font
        var fontSize = font.size2D
        println("fontSize = $fontSize")
        if (e.actionCommand == "+") fontSize++ else fontSize--
        font = font.deriveFont(fontSize)
        textArea.font = font
    }

//    private fun getItemsMap(): MutableMap<String, Set<String>> {
//        val curPath = System.getProperty("user.dir")
//        val backgroundImage = "items/background.jpg"
//        println("user dir = $curPath")
////    var filesSet =  listOf("$curPath/welcome.txt", "$curPath/$backgroundImage")
//        val appProps = Properties()
//        appProps.load(FileInputStream(folderNamePath))
//        println("props = ${appProps.entries}")
//        val itemsFolderName = appProps.getProperty("itemsFolder")
//        val itemsDir = "$curPath/items/$itemsFolderName"
//        val dirsList = listDirsUsingDirectoryStream(itemsDir)
//        println("dirsList = $dirsList")
//        //var setOfItems =
////    var itemsMap = mutableMapOf<String, String>()
//        val itemsMap2 =
//            mutableMapOf<String, Set<String>>() //мап для хранения названия экспоната и набора из его описания и картинки
//        dirsList.forEach {
//            val filesList = listFilesUsingDirectoryStream("$itemsDir/$it")
//            val txtFile = filesList.find { it.contains(".txt") }
//            val imgFile = filesList.find { it != txtFile }
//            val txtFilePath = "$itemsDir/$it/$txtFile"
//            val imgFilePath = "$itemsDir/$it/$imgFile"
////        val filesSet = setOf(txtFilePath, imgFilePath)
//            val file = File(txtFilePath)
//            val ioStream = BufferedReader(FileReader(file))
//            val firstStringInFile = ioStream.readLine()
//            val s = if (firstStringInFile == "") ioStream.readLine() else firstStringInFile //если первая строка пустая
////        println("for $it: $filesList caption = $s")
////        itemsMap[s] = "$itemsDir/$it"
//            itemsMap2[s] = setOf(txtFilePath, imgFilePath)
//        }
//        itemsMap2.forEach {
//            println("${it.key} : ${it.value}")
//        }
//        return itemsMap2
//    }

    private fun setCentralPart() {
        val pane = JPanel(GridBagLayout())
//        pane.border = BorderFactory.createLineBorder(Color.GREEN, 3)
//        pane.layout = GridBagLayout()
        val text = File(filesSet.first()).readText()
        textArea.text = text
        textArea.lineWrap = true
        textArea.wrapStyleWord = true
//        textArea.border = BorderFactory.createLineBorder(Color.BLUE, 2)
        var font = textArea.font
        font = font.deriveFont(22f)
        textArea.font = font
        val c = GridBagConstraints()
        c.fill = GridBagConstraints.BOTH
        c.gridx = 0 //aligned with button 2
//        c.gridwidth = 3 //2 columns wide
        c.weightx = 3.0
        c.weighty = 1.0
        c.gridy = 0 //
        pane.add(JScrollPane(textArea), c)

//        var button = JButton("")
        c.fill = GridBagConstraints.BOTH
        c.gridx = 1
//        c.gridwidth = 1
        c.weightx = 0.5
        c.gridy = 0
//        imageHolder.border = BorderFactory.createLineBorder(Color.RED, 2)
//        val filesSet = frame.getFilesSet()
        var itemImage = ImageIcon(filesSet.last())

        var newimg = getScaledImage(itemImage, 500, 800)
//            image.getScaledInstance(imageWidth, imageHeight, Image.SCALE_SMOOTH) // задаем размер
        imageHolder.icon = ImageIcon(newimg)
        imageHolder.border = BorderFactory.createLineBorder(Color.RED, 2)
        imageHolder.horizontalAlignment = JLabel.CENTER
//        imageHolder.icon = itemImage
        imageHolder.preferredSize = Dimension(400, 300)
        pane.add(imageHolder, c)

//        button = JButton("Long-Named Button 4")
////        c.fill = GridBagConstraints.HORIZONTAL
//        c.fill = GridBagConstraints.BOTH
//        c.ipady = 40 //make this component tall
//        c.weightx = 0.0
//        c.gridwidth = 3
//        c.gridx = 0
//        c.gridy = 1
//        pane.add(button, c)
//
//        button = JButton("5")
////        c.fill = GridBagConstraints.HORIZONTAL
//        c.fill = GridBagConstraints.BOTH
//        c.ipady = 0 //reset to default
//        c.weighty = 1.0 //request any extra vertical space
//        c.anchor = GridBagConstraints.PAGE_END //bottom of space
//        c.insets = Insets(10, 0, 0, 0) //top padding
//        c.gridx = 1 //aligned with button 2
//        c.gridwidth = 2 //2 columns wide
//        c.gridy = 2 //third row
//        pane.add(button, c)
        add(pane, BorderLayout.CENTER)
    }
}

private fun createAndShowGUI() {

    val firstFrame = JFrame("Загрузка")
    firstFrame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
    firstFrame.setSize(400, 300)
    firstFrame.setLocationRelativeTo(null)
    val pane = JPanel(GridBagLayout())
    val c = GridBagConstraints()
    c.fill = GridBagConstraints.HORIZONTAL
    c.gridx = 0 //aligned with button 2
//        c.gridwidth = 3 //2 columns wide
    c.weightx = 2.0
    c.weighty = 2.0
    c.gridy = 0 //
    val showMainBtn = JButton("Музей")
    showMainBtn.addActionListener {
        val frame = SimpleEx("Simple")
//    frame.isUndecorated = true
        val graphics = GraphicsEnvironment.getLocalGraphicsEnvironment()
        val device = graphics.defaultScreenDevice
//        device.setFullScreenWindow(frame) //for full screen
        frame.isVisible = true
        firstFrame.isVisible = false
    }
    pane.add(showMainBtn, c)
    c.gridx = 2
    pane.add(Box.createHorizontalGlue(), c)
    val showSettingsBtn = JButton("Настройки")
    c.gridx = 4 //aligned with button 2
//        c.gridwidth = 3 //2 columns wide
    c.weightx = 2.0
    c.weighty = 2.0
    c.gridy = 0 //
    showSettingsBtn.addActionListener {
        val settingsWindow = SettingsWork()
        settingsWindow.isVisible = true
    }

    pane.add(showSettingsBtn, c)
    firstFrame.add(pane, BorderLayout.CENTER)
    firstFrame.isVisible = true
//    val frame = SimpleEx("Simple")
////    frame.isUndecorated = true
//    val graphics = GraphicsEnvironment.getLocalGraphicsEnvironment()
//    val device = graphics.defaultScreenDevice
//    device.setFullScreenWindow(frame) //for full screen
//    frame.isVisible = true
//    val filesSet = frame.getFilesSet()
//    var itemImage = ImageIcon(filesSet.last())
//    var image = itemImage.image
//    val imageHolder = frame.getImageHolder()
//    var newimg = image.getScaledInstance(imageHolder.width, imageHolder.height, Image.SCALE_SMOOTH) // задаем размер
////    imageHolder.icon = ImageIcon(newimg)
//    imageHolder.icon = itemImage
}

fun main() {
    FlatLightLaf.setup()
    UIManager.setLookAndFeel(FlatLightLaf())
    val looks = UIManager.getInstalledLookAndFeels()
    for (look in looks) {
        println(look.className)
    }
    EventQueue.invokeLater(::createAndShowGUI)
}

//класс для отображения заголовка в комбобоксе
internal class MyComboBoxRenderer(private val _title: String) : JLabel(), ListCellRenderer<Any?> {
    override fun getListCellRendererComponent(
        list: JList<*>?, value: Any?,
        index: Int, isSelected: Boolean, hasFocus: Boolean
    ): Component {
        text = if (index == -1 && value == null) _title
        else value.toString()
        return this
    }
}